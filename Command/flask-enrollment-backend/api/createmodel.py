import os
from io import BytesIO
import math
from pathlib import Path
import sys
import json
import errno

import boto3
from flask import Blueprint, Flask, current_app, request, send_file, jsonify
from flask_restful import Resource, Api, reqparse, abort
from flask_praetorian import auth_required
from werkzeug.datastructures import FileStorage
from werkzeug.utils import secure_filename
import requests

from apollo.models import EnrollmentLog
from apollo import PostgresDatabase

from .  import db
from sqlalchemy.exc import ResourceClosedError

import shutil
import tempfile
import time
from datetime import datetime
import threading
from itertools import repeat
from uuid import uuid4

# Constants
ALLOWED_IMG_EXTENSIONS = {'.png', '.jpg', '.jpeg', '.bmp', '.ppm', '.tif'}
ALLOWED_SND_EXTENSIONS = {'.aac', '.m4a', '.wav', '.mp3'}
HTTP_OK = 200
HTTP_BAD_REQUEST = 400
MAX_AUDIO_SEC = 30

# Allow concurrent analytic
g_speakerIdLock = threading.RLock() # extra thread safety lock for speakerID analytic
g_faceIdLock    = threading.RLock() # extra thread safety lock for faceID analytic
g_speakerIdTrainingLUT = {}         # tracks speakerID training progress
g_faceIdTrainingLUT = {}            # tracks facedID training progress

# Allow concurrent data uploading
g_faceIdDataWriteLock = threading.RLock() # atomic operation for uploading face data
g_speakerIdDataWriteLock = threading.RLock() # atomic operation for uploading speaker data
g_faceIdHasLock = threading.Event()          # for vipbaseclass put method to run simultaneously
g_speakerIdHasLock = threading.Event()       # for vipbaseclass put method to run simultaneously

# Log support
g_database = PostgresDatabase (table=EnrollmentLog.__table__)

# Flask
upload_blueprint = Blueprint('createmodel', __name__)
api = Api(upload_blueprint)


def readJson (fileName):
    try:
        with open (fileName, 'r') as inFile:
            encoded = inFile.read ()
            obj = json.loads(encoded)
            return obj
    except Exception as inst:
        print(type(inst))    # the exception instance
        print(inst.args)     # arguments stored in .args
        print(inst)          # __str__ allows args to be printed directly,

def removeNoCheck (fileName):
    try:
        os.remove (fileName)
    except:
        pass
    

def sendDownloadFile (filename, ts=True, delete=False, dryrun=False):
    """
    https://stackoverflow.com/questions/13344538/how-to-clean-up-temporary-file-used-with-send-file

    ts: if True, then use the file timestamp.
    """

    attachBaseName = Path(filename).name
    if ts:
        filenamePath = Path(filename)
        file_ts = filenamePath.lstat().st_mtime
        ts_str = datetime.fromtimestamp(file_ts).isoformat()
        attachBaseName = filenamePath.stem + "__ts=" + ts_str + filenamePath.suffix
        
    if dryrun:
        return attachBaseName

    
    fid = open (filename, mode='rb')
    fileContent = fid.read()
    fid.close()

    # fileContent = Path (filename).read_bytes() 
    print (f"size = {len(fileContent)}", flush=True)
    fid = tempfile.TemporaryFile()
    fid.write (fileContent)
    fid.seek(0)

        
    if delete:
        os.remove (filename)
        
    response = send_file(fid, as_attachment=True,
                         # use "curl -I" to see the attachment_filename
                         attachment_filename = attachBaseName,
                         add_etags=False)

    fid.seek(0, os.SEEK_END)
    size = fid.tell()
    fid.seek(0)
    response.headers.extend({
        'Content-Length': size,
        'Cache-Control': 'no-cache'
    })
    return response


def isAllowedFile (filename, allowedExts):
    ext = Path(filename).suffix.lower()
    return ext in allowedExts


def getUploadedFile (uploadFolder, allowedExts):
    """
    This routine gets the uploaded file and save it to uploadFolder directory.

    return (filename, success, errorMesg)
    """
    try:
        file = request.files['file']
    except Exception as inst: # check error
        print(type(inst))    # the exception instance
        print(inst.args)     # arguments stored in .args
        print(inst)          # __str__ allows args to be printed directly,
        print ('No selected file', flush=True)
        return (None, False, "No file provided")

    if file and isAllowedFile (file.filename, allowedExts):
        filename = secure_filename (file.filename)
        ## Do not overwrite if file already exists
        if os.path.isfile (os.path.join (uploadFolder, filename)):
            print (f'file {filename} already exists', flush=True)
            errorMesg = f'File {filename} already exists, please delete it first.'
            return (file.filename, False, errorMesg)
            
        ## Save the file
        file.save (os.path.join (uploadFolder, filename))
        print (f'saved {filename} to {uploadFolder}', flush=True)
        return (file.filename, True, "")

    print (f'file {file.filename} not uploaded', flush=True)
    errorMesg = ("Please check to see if file " + 
                 f"'{file.filename}' has the right extension.")
    return (file.filename, False, errorMesg)


def CheckTrainingStatus (action, analytic, mission, inTrainingLUT):
    """"
    Check if mission is currently in training.
    inTrainingLUT: {mission:timestamp}
    return: (True, errorDict) -- currently in training, or (False, {})
    """
    print (f"inTrainingLUT is {inTrainingLUT}", flush=True)
    lutKey = mission
    if lutKey in inTrainingLUT:
        ts1 = inTrainingLUT [lutKey]
        ts2 = time.time()
        # delta = datetime.fromtimestamp(ts2) - datetime.fromtimestamp(ts1)
        delta_sec = ts2 - ts1
        errDict = {"action":action, "success":False,
                   "analytic": analytic,
                   "mission": mission,
                   "error": f"'{mission}' is currently in training, " +
                   "Please try again later.",
                   "training_started": datetime.fromtimestamp(ts1).isoformat(),
                   "training_elapsed": int (delta_sec)}
        return (True, errDict)
    return (False, {})


def LogRecord (user, action, parameters=None, errorMesg=None):
    """
    parameters = request.args
    """
    global g_database
    row = {}
    row ["user"] = user
    row ["action"] = action
    row ["timestamp"] = datetime.now()
    if parameters != None:
        row ["parameters"] = str(parameters.to_dict(flat=False))
    if errorMesg != None:
        row ["success"] = False
        row ["error"] = errorMesg
    else:
        row ["success"] = True
    print (f"LogRecord action = {action}", flush=True)
    g_database.save_record_to_database (row, EnrollmentLog) # creates new session each time


def getFileTimeStamp (filePath):
    return os.stat(filePath).st_mtime

def getFileTimeStamp_str (filePath):
    return datetime.fromtimestamp(os.stat(filePath).st_mtime).isoformat()

def GetFileInfo (root, fname, getSize=True):
    """
    Return a dictionary structure:
    {"name", "date", "date-str"}
    """
    def getFileSize (filePath):
        return os.stat(filePath).st_size

    if getSize:
        return {"name":fname,
                "date": getFileTimeStamp (Path(root)/fname),
                "date-str": getFileTimeStamp_str (Path(root)/fname),
                "size": getFileSize (Path(root)/fname)}
    return {"name":fname,
            "date": getFileTimeStamp (Path(root)/fname),
            "date-str": getFileTimeStamp_str (Path(root)/fname)}


def GetDirContents (parentDir, listDir_flg=False):
    """
    Return a tuple (childDirs, childDirListing)
    # - Return json output like "childDirListing[i].name or childDirListing[i].date"
    # childDirListing[i].name
    # childDirListing[i].date
    # childDirListing[i].dir-list  (if listDir_flg = true), otherwise "files-list"
    # childDirListing[i].files[i].name
    # childDirListing[i].files[i].date
    """
    (root, dirnames, filenames) = next (os.walk (parentDir))
    childDirs = sorted (dirnames)

    dirContents = []
    for (root, dirnames, filenames) in os.walk (parentDir, topdown=True):
        (_, dirName) = os.path.split (root)
        filenames = sorted (filenames)
        dirnames = sorted (dirnames)
        # print (root, dirnames, filenames)
        # print (dirName)
        # print ("childDirs", childDirs)
        if ((len(childDirs) == 0) or # works for no child directories too
            (dirName in childDirs)): # ignore sub-directories below 
            print (root, dirnames, filenames)
            if listDir_flg:
                files = list (map (GetFileInfo, repeat (root), dirnames, repeat (False)))
                itemType = "dirs"
                files_list = dirnames
            else:
                files = list (map (GetFileInfo, repeat (root), filenames))
                itemType = "files"
                files_list = filenames
            dirContents.append ({"name": dirName,
                                 "date": getFileTimeStamp (root),
                                 "date-str": getFileTimeStamp_str (root),
                                 itemType: files,
                                 f"{itemType}-list": files_list})
            
    return (childDirs, dirContents)
    

def GetMissionContain (vip, missionsDir):
    """
    Return a list of missions that contain the specific vip. 
    All we do is walk through the mission directories and look for the vip. 
    """

    missions = []
    (_, missionContents) = GetDirContents (missionsDir, listDir_flg=True)
    missions = [missionContent["name"] for missionContent in missionContents
                if vip in missionContent["dirs-list"]]
    return missions
    

class VipBaseClass (Resource):
    m_LOCK = None
    m_analytic = None
    m_VIPS_DIR = None
    m_MISSIONS_DIR = None
    m_API_TRAIN_DIR = None
    m_allowedExts = None
    m_inTrainingLUT = None
    m_writeLock = None          # make write to disk atomic
    m_hasLock = None            # block when other operation has the lock
    
    def __init__ (self, analytic, mLock, inTrainingLUT,
                  trainDir, cacheDir, allowedExts, uuidPrefix, **kwargs):
        print (f"calling {analytic} VipBaseClass constructor", flush=True)

        self.m_LOCK = mLock
        self.m_analytic = analytic
        self.m_VIPS_DIR = Path ("api") / trainDir / "vips_with_profile_picture"
        self.m_MISSIONS_DIR = Path ("api") / trainDir / "missions"
        self.m_API_TRAIN_DIR = Path ("api") / trainDir 
        self.m_allowedExts = allowedExts
        self.m_inTrainingLUT = inTrainingLUT
        self.m_writeLock = kwargs["writeLock"]
        self.m_hasLock = kwargs["hasLock"]
        

    def path_hierarchy(self, path):
        hierarchy = {
            'type': 'folder',
            'name': os.path.basename(path),
            'path': path,
        }

        try:
            hierarchy['children'] = [
                self.path_hierarchy(os.path.join(path, contents))
                for contents in os.listdir(path)
            ]
        except OSError as e:
            if e.errno != errno.ENOTDIR:
                raise
            hierarchy['type'] = 'file'

        return hierarchy


    def get(self):
        """List vip data

        keys:
          vip:  eg., Jim_Gaffigan, if not specified, list all vips

        """
        print ("====GET====", flush=True)
        action = f"{request.path}:get"
        user = request.args["user"]
        
        ## extract the required fields:
        with self.m_LOCK:       # prevent new Training from starting
            if not ("vip" in request.args):
                # Get all vips
                action = 'list_all_vips'
                if not self.m_VIPS_DIR.is_dir(): # check error
                    errorMesg = f"Error listing {self.m_VIPS_DIR}"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False, "error": errorMesg},
                            HTTP_BAD_REQUEST)
                (vips, vipContents) = GetDirContents (self.m_VIPS_DIR, listDir_flg=False)
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True,
                         "vips": vips,
                         "vipContents": vipContents}, HTTP_OK)
            # Got specific vip
            vip = request.args['vip']
            vipDir = self.m_VIPS_DIR / vip

            # Check to see if want only a specific file
            if ("file" in request.args):
                action = 'list_vip_file'
                filename = request.args['file']
                vipFile = vipDir / filename
                if not vipFile.is_file(): # check error
                    errorMesg = f"{vipFile} does not exist"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "error": errorMesg},
                            HTTP_BAD_REQUEST)
                # get file info
                fileInfo = GetFileInfo (vipDir, filename)
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True,
                         "file" : filename,
                         "file-info": fileInfo},
                        HTTP_OK)
            
            # Get all files for the specific vip
            if vip == "_all_":
                print(f"Getting all the folders and files")
                print(json.dumps(self.path_hierarchy(str(self.m_VIPS_DIR))), flush=True)   
                #return ({self.path_hierarchy(self.m_VIPS_DIR), HTTP_OK)
                return ({"action":action, "success":True,
                         "vips":self.path_hierarchy(str(self.m_VIPS_DIR))}, HTTP_OK) 

            else:
                print(f"Get specific vip= {vip} VIPS_DIR={self.m_VIPS_DIR} vipDir={vipDir}")

                ## want to list the vip content             
                action = 'list_vip_content'
                dir_content = []
                if not vipDir.is_dir(): # check error
                    errorMesg = f"'{vipDir}' does not exists"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False, "vip": vip, "error": errorMesg},
                            HTTP_BAD_REQUEST)
                (root, dirnames, filenames) = next (os.walk (vipDir))
                dir_content = ({"root":root, "dirnames":sorted(dirnames),
                                "filenames":sorted(filenames)})
                (_, vipContent) = GetDirContents (vipDir, listDir_flg=False)
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "vip": vip,
                         "dir-content": dir_content, "vipContent":vipContent},
                        HTTP_OK)

        
    def put (self):
        """Upload a data sample to a particular vip

        keys:
          vip:  eg., Ewan_McGregor
        """
        print ("==== PUT ====", flush=True)
        action = f"{request.path}:put"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_writeLock:                                 # make it atomic
            lockAquired = self.m_LOCK.acquire (blocking=False) ## Allow multiple insertion
            print (f"lockAquired = {lockAquired}", flush=True)
            if lockAquired: self.m_hasLock.set()

        if (lockAquired == False) and (self.m_hasLock.is_set() == False):
            # lock was by other operation, we have to wait
            print ("------> put has to wait for other operations", flush=True)
            lockAquired = self.m_LOCK.acquire ()
            self.m_hasLock.set()

        if not ("vip" in request.args):
            errorMesg = "need to specify vip"
            LogRecord (user, action, request.args, errorMesg)
            if lockAquired: self.m_LOCK.release() ## Allow multiple insertion
            self.m_hasLock.clear()
            return ({"action":action, "success":False, "error":errorMesg},
                    HTTP_BAD_REQUEST)
        action = "insert_file"
        vip = request.args["vip"]

        # check if any mission containing the vip is currently in training
        missions_list = GetMissionContain (vip, self.m_MISSIONS_DIR)
        for mission in missions_list:
            (isTraining, errDict) = CheckTrainingStatus (
                action, self.m_analytic, mission, self.m_inTrainingLUT)
            if isTraining:
                errDict ["vip"] = vip
                errDict ["vip-in-missions"] = missions_list
                errDict ["error"] = (f"Can not insert to vip '{vip}' because a " +
                                     "mission containing this vip is currently "+
                                     "in training.  Please try again later.")
                LogRecord (user, action, request.args, errDict ["error"])
                if lockAquired: self.m_LOCK.release() ## Allow multiple insertion
                self.m_hasLock.clear()
                return (errDict, HTTP_BAD_REQUEST)

        # write to disk is an atomic operation
        with self.m_writeLock:
            # create the directory if needed
            vipDir = self.m_VIPS_DIR / vip
            if not vipDir.is_dir():
                os.makedirs (vipDir)

            # obtain and upload the file 
            (filename, success, errorMesg) = getUploadedFile (vipDir, self.m_allowedExts)
            if not success:
                LogRecord (user, action, request.args, errorMesg)
                if lockAquired: self.m_LOCK.release() ## Allow multiple insertion
                self.m_hasLock.clear()
                return ({'action':action, 'success':False, 'file':str (filename),
                         'vip':vip, "error":errorMesg},
                        HTTP_BAD_REQUEST)

        # verify data integrity 
        insertedFile = vipDir / filename
        print (f"self.m_API_TRAIN_DIR = {self.m_API_TRAIN_DIR}")
        jsonOutFile = f"/dev/shm/{uuid4().hex}"
        cmd = (f"{self.m_API_TRAIN_DIR}/verify_input_file.bash {str (insertedFile)} {jsonOutFile}")
        if self.m_analytic == "speakerid":
            if ("audio-max-length" in request.args):
                maxAudioLen = request.args['audio-max-length']
            else:
                maxAudioLen = MAX_AUDIO_SEC
            cmd += f" {maxAudioLen}"
        print (f"cmd = {cmd}", flush=True)
        
        retVal = os.system (cmd)
        jsonObj = readJson (jsonOutFile)
        removeNoCheck (jsonOutFile)
        if retVal != 0:         # check error
            print ("undo download", flush=True)
            os.remove (insertedFile) # undo download
            if self.m_analytic == "faceid":
                errorMesg = jsonObj.get (
                    "error", "input file verifcation failed (detected face must be > 160x160)")
                faceXY = jsonObj.get('face-xy', 0)
                faceSize = jsonObj.get('face-size', 0)
                imageSize = jsonObj.get('image-size', 0)
                extraDic = {'face-xy': faceXY, 'face-size': faceSize, 'image-size': imageSize}
            elif self.m_analytic == "speakerid":
                errorMesg = jsonObj.get (
                    "error", "input file verifcation failed (speech must be English and > 4s)")
                speechLen = jsonObj.get('speech-len', 0)
                audioLen = jsonObj.get('audio-len', 0)
                extraDic = {'audio-max-length': str (maxAudioLen),
                            'audio-length': str (audioLen),
                            'speech-len': str (speechLen), 'vip':vip}
            else:
                1/0
            LogRecord (user, action, request.args, errorMesg)
            if lockAquired: self.m_LOCK.release() ## Allow multiple insertion
            self.m_hasLock.clear()
            return ({**{'action':action, 'success':False, 'file': (filename),
                        'vip':vip, "error":errorMesg}, **extraDic},
                    HTTP_BAD_REQUEST)

        # file insterted successfully
        LogRecord (user, action, request.args)
        if lockAquired: self.m_LOCK.release() ## Allow multiple insertion
        self.m_hasLock.clear()
        if self.m_analytic == "speakerid":
            speechLen = jsonObj.get('speech-len', 0)
            audioLen = jsonObj.get('audio-len', 0)
            extraDic = {'audio-max-length': str (maxAudioLen),
                        'audio-length': str (audioLen),
                        'speech-len': str (speechLen), 'vip':vip}
        elif self.m_analytic == "faceid":
            faceXY = jsonObj.get('face-xy', 0)
            faceSize = jsonObj.get('face-size', 0)
            imageSize = jsonObj.get('image-size', 0)
            extraDic = {'face-xy': faceXY, 'face-size': faceSize, 'image-size': imageSize}
        else:
            1/0
        return ({**{'action':action, 'success':True, 'file':str (insertedFile),
                    'vip':vip}, **extraDic}, HTTP_OK)


    def delete (self):
        """Remove a data sample from a specific vip

        keys:
          vip:  eg., Ewan_McGregor
          file: image to delete,  eg., jim_gaffigan.jpg
        """
        
        print ("==== DELETE ====", flush=True)
        action = f"{request.path}:delete"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_LOCK:       # prevents new Trainig from starting
            # print ("delete sleeps with LOCK for 3 sec", flush=True)
            # time.sleep (3)
            if not ("vip" in request.args):
                errorMesg = "need to specify vip"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False,
                         "error":errorMesg},
                        HTTP_BAD_REQUEST)
            vip = request.args['vip']

            # check if any mission containing the vip is currently in training
            missions_list = GetMissionContain (vip, self.m_MISSIONS_DIR)
            for mission in missions_list:
                (isTraining, errDict) = CheckTrainingStatus (
                    action, self.m_analytic, mission, self.m_inTrainingLUT)
                if isTraining:
                    errDict ["vip"] = vip
                    errDict ["vip-in-missions"] = missions_list
                    errDict ["error"] = (f"Can not delete vip '{vip}' because a " +
                                         "mission containing this vip is currently "+
                                         "in training.  Please try again later.")
                    LogRecord (user, action, request.args, errDict ["error"])
                    return (errDict, HTTP_BAD_REQUEST)

            vipDir = self.m_VIPS_DIR / vip 
            if not vipDir.is_dir(): # check error
                errorMesg = f"vip {vip} does not exist"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error": errorMesg},
                        HTTP_BAD_REQUEST)
            
            if ("file" in request.args):
                # Delete a particular file from vip:
                action = 'delete_data_sample'
                filename = request.args['file']
                deleteFile = vipDir / filename
                if not deleteFile.is_file(): # check error
                    errorMesg = f"{deleteFile} does not exist"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "error": errorMesg},
                            HTTP_BAD_REQUEST)
                try:
                    os.remove (deleteFile)
                except Exception as inst: # check error
                    print(type(inst))    # the exception instance
                    print(inst.args)     # arguments stored in .args
                    print(inst)          # __str__ allows args to be printed directly,
                    errorMesg = f"could not remove {deleteFile}"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False, "error":errorMesg},
                            HTTP_BAD_REQUEST)

                # success
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "file":str (deleteFile)}, HTTP_OK)
            
            # Delete the vip entirely:
            action = 'delete_vip'
            
            # First, remove vip symlinks from all missions
            missionWalker = iter (os.walk (self.m_MISSIONS_DIR))
            next (missionWalker)        # get rid of top parent

            filenames = True
            while not (filenames == None):
                (root, dirnames, filenames) = next (missionWalker, (None, [], None))
                # print (f"dirnames = {dirnames}", flush=True)
                if vip in dirnames:
                    missionVipDir = os.path.join (root, vip)
                    print (f"removing symlink {missionVipDir}", flush=True)
                    try:
                        os.remove (missionVipDir)
                    except Exception as inst: # check error
                        print(type(inst))    # the exception instance
                        print(inst.args)     # arguments stored in .args
                        print(inst)          # __str__ allows args to be printed directly,
                        errorMesg = f"could not remove {missionVipDir}"
                        LogRecord (user, action, request.args, errorMesg)
                        return ({"action":action, "success":False, "vip": vip, "error":errorMesg},
                                HTTP_BAD_REQUEST)

            # Then, delete the vip entirely:
            try:
                shutil.rmtree (vipDir)
            except Exception as inst: # check error
                print(type(inst))    # the exception instance
                print(inst.args)     # arguments stored in .args
                print(inst)          # __str__ allows args to be printed directly,
                errorMesg = f"could not remove {vipDir}"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False,
                         "vipDir": str (vipDir), "error": errorMesg},
                        HTTP_BAD_REQUEST)
            
            # success
            LogRecord (user, action, request.args)
            return ({"action":action, "success":True, "vip":vip}, HTTP_OK)
                

class MissionBaseClass (Resource):
    m_LOCK = None
    m_analytic = None
    m_MISSIONS_DIR = None
    m_VIPS_DIR = None
    m_API_TRAIN_DIR = None
    m_inTrainingLUT = None
    m_UUID_PREFIX = None
    
    def __init__ (self, analytic, mLock, inTrainingLUT,
                  trainDir, cacheDir, allowedExts, uuidPrefix, **kwargs):
        print (f"calling {analytic} MissionBaseClass constructor", flush=True)

        self.m_LOCK = mLock
        self.m_analytic = analytic
        self.m_API_TRAIN_DIR = Path ("api") / trainDir 
        self.m_VIPS_DIR = Path ("api") / trainDir / "vips_with_profile_picture"
        self.m_MISSIONS_DIR = Path ("api") / trainDir / "missions"
        self.m_UUID_PREFIX = uuidPrefix # eg FaceID_ or SpeakerID_
        self.m_inTrainingLUT = inTrainingLUT

    def get(self):
        """List mission data

        keys:
          mission: eg., celebrity10, if not specified, list all missions

        """
        print ("====GET====", flush=True)
        action = f"{request.path}:get"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_LOCK:       # prevent new Training from starting
            if not ("mission" in request.args):
                # Get all missions
                action = 'list_all_missions'
                if not self.m_MISSIONS_DIR.is_dir(): # check error
                    errorMesg = f"Error listing {self.m_MISSIONS_DIR}"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "analytic":self.m_analytic,
                             "error": errorMesg},
                            HTTP_BAD_REQUEST)
                (missions, missionContents) = GetDirContents (self.m_MISSIONS_DIR, True)
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "missions": missions,
                         "missionContents": missionContents}, HTTP_OK)

            # Got specific mission
            mission = request.args['mission']
            missionDir = self.m_MISSIONS_DIR / mission

            ## want to list the mission content 
            action = 'list_mission_content'
            dir_content = []
            if not missionDir.is_dir(): # check error
                errorMesg = f"'{missionDir}' does not exists"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "mission": mission,
                         "error": errorMesg},
                        HTTP_BAD_REQUEST)
            (root, dirnames, filenames) = next (os.walk (missionDir))
            dir_content = ({"root":root, "dirnames":sorted(dirnames),
                            "filenames":sorted(filenames)})
            LogRecord (user, action, request.args)
            return ({"action":action, "success":True, "mission": mission,
                     "dir-content": dir_content}, HTTP_OK)
            

    def post(self):
        """
        This method creates the classifier for a specific mission

        keys:
          mission=celebrity10
        """
        print ("==== POST ====", flush=True)
        action = f"{request.path}:post"
        user = request.args["user"]

        # check if mission is specified:
        if not ("mission" in request.args):
            return ({'action': action, 'success':False,
                     "error":f"must speicify 'mission'"},
                    HTTP_BAD_REQUEST)
        action = 'train_mission'
        mission = request.args['mission']

        # check if mission is currently in training
        (isTraining, errDict) = CheckTrainingStatus (
            action, self.m_analytic, mission, self.m_inTrainingLUT)
        if isTraining:
            LogRecord (user, action, request.args, "Training in progress")
            return (errDict, HTTP_BAD_REQUEST)

        # Starting training, set lock in inTrainingLUT:
        print ("********** About to set self.m_inTrainingLUT", flush=True)
        with self.m_LOCK:       # make sure it's good time to start training
            print ("********* Setting self.m_inTrainingLUT", flush=True)
            self.m_inTrainingLUT [mission] = time.time()

        # Do actual training -- this is a blocking call
        cmd = (f"{self.m_API_TRAIN_DIR}/run.bash " + 
               f"missions/{mission} " +
               f"classifier_models/{mission} " +
               f"{self.m_UUID_PREFIX}")
        if self.m_analytic == "speakerid":
            if ("audio-max-length" in request.args):
                maxAudioLen = request.args['audio-max-length']
            else:
                maxAudioLen = MAX_AUDIO_SEC
            cmd += f" {maxAudioLen}"
        print (f"cmd = {cmd}", flush=True)
        retVal = os.system (cmd)

        # Add training timing info:
        started_ts = self.m_inTrainingLUT [mission]
        ended_ts = time.time ()

        # Done with training, now remove m_inTrainingLUT lock:
        with self.m_LOCK:       # protects m_inTrainingLUT access
            self.m_inTrainingLUT.pop (mission)
        
        if retVal != 0:         # check error
            return ({'action': action, 'success':False,
                     'mission': mission,
                     "error":f"failed to execute: '{cmd}'"},
                    HTTP_BAD_REQUEST)

        return ({'action': action, 'success':True,
                 'mission': mission,
                 "training_started": datetime.fromtimestamp(started_ts).isoformat(),
                 "training_ended": datetime.fromtimestamp(ended_ts).isoformat(),
                 "training_elapsed": int (ended_ts - started_ts)},
                HTTP_OK)
        
        
    def put (self):
        """Add an existing vip to a specific mission.

        keys:
          mission:  eg., celebrity10
          vip:  eg., Ewan_McGregor
        """
        print ("==== PUT ====", flush=True)
        action = f"{request.path}:put"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_LOCK:       # prevents new Trainig from starting
            if not (("vip" in request.args) and ("mission" in request.args)):
                errorMesg = "need to specify both mission and vip"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error":errorMesg},
                        HTTP_BAD_REQUEST)
            action = "insert_vip"
            vip = request.args["vip"]
            mission = request.args["mission"]

            # check if mission is currently in training
            (isTraining, errDict) = CheckTrainingStatus (
                action, self.m_analytic, mission, self.m_inTrainingLUT)
            if isTraining:
                LogRecord (user, action, request.args, "Training in progress")
                return (errDict, HTTP_BAD_REQUEST)
            
            missionDir = self.m_MISSIONS_DIR / mission
            # create the directory if needed
            if not missionDir.is_dir():
                os.makedirs (missionDir)

            # create the vip symbolic link if needed
            srcVipDir = self.m_VIPS_DIR / vip
            destVipDir = missionDir / vip

            if not srcVipDir.is_dir(): # check error
                errorMesg = f"vip {vip} does not exist!"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error":errorMesg, "vip":vip},
                        HTTP_BAD_REQUEST)
            if destVipDir.is_symlink(): # check error
                errorMesg = f"vip '{vip}' already in mission '{mission}'"
                LogRecord (user, action, request.args)
                return ({"action":action, "success":False, "error":errorMesg,
                         "vip":vip, "mission":mission}, HTTP_BAD_REQUEST)

            # do symbolic link
            try:
                relVipDir = Path (os.path.relpath (srcVipDir, missionDir))
                destVipDir.symlink_to (relVipDir)
            except Exception as inst: # check error
                print(type(inst))    # the exception instance
                print(inst.args)     # arguments stored in .args
                print(inst)          # __str__ allows args to be printed directly,
                errorMesg = ("could not create symlink " +
                             f"{str(destVipDir)} -> {str(srcVipDir)}")
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error":errorMesg},
                        HTTP_BAD_REQUEST)
            # succss
            LogRecord (user, action, request.args)
            return ({"action":action, "success":True, "vip":vip, "mission":mission,
                     "srcVipDir":str(srcVipDir), "destVipDir":str(destVipDir)},
                    HTTP_OK)

    def delete (self):
        """Remove the entire mission or just a specific vip from the mission.

        keys:
          mission:  eg., celebrity10
          vip:  eg., Ewan_McGregor, if not specified, delete the entire mission
        """
        
        print ("==== DELETE ====", flush=True)
        action = f"{request.path}:delete"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_LOCK:       # prevents new Trainig from starting
            if not ("mission" in request.args):
                errorMesg = "must specify mission"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error":errorMesg},
                        HTTP_BAD_REQUEST)

            mission = request.args["mission"]
            missionDir = self.m_MISSIONS_DIR / mission

            # check if mission is currently in training
            (isTraining, errDict) = CheckTrainingStatus (
                action, self.m_analytic, mission, self.m_inTrainingLUT)
            if isTraining:
                LogRecord (user, action, request.args, "Training in progress")
                return (errDict, HTTP_BAD_REQUEST)
            
            if not ("vip" in request.args):
                # Delete the entire mission:
                action = "delete_mission"
                if not missionDir.is_dir(): # check error
                    errorMesg = f"mission '{str (missionDir)}' does not exist"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False, "mission": mission,
                             "missionDir": str (missionDir), "error":errorMesg},
                            HTTP_BAD_REQUEST)
                try:
                    shutil.rmtree (missionDir)
                except Exception as inst:
                    print(type(inst))    # the exception instance
                    print(inst.args)     # arguments stored in .args
                    print(inst)          # __str__ allows args to be printed directly,
                    errorMesg = f"could not remove {missionDir}"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "missionDir": str (missionDir), "error": errorMesg},
                            HTTP_BAD_REQUEST)

                # success
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "mission":mission}, HTTP_OK)
                
            # Delete a particular vip from the mission:
            action = 'delete_vip_from_mission'
            vip = request.args['vip']
            vipSymlink = missionDir / vip
            if not vipSymlink.is_symlink(): # check error
                errorMesg = f"'{vipSymlink}' does not exist or is not a symbolic link"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "vip": vip, "error": errorMesg},
                        HTTP_BAD_REQUEST)
            try:
                os.remove (vipSymlink)
            except Exception as inst:
                print(type(inst))    # the exception instance
                print(inst.args)     # arguments stored in .args
                print(inst)          # __str__ allows args to be printed directly,
                errorMesg = f"could not remove symbolic link at {vipSymlink}"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error":errorMesg},
                        HTTP_BAD_REQUEST)

            # success
            LogRecord (user, action, request.args)
            return ({"action":action, "success":True, "mission":mission, "vip":vip}, HTTP_OK)
        

class MissionDownloadBaseClass (Resource):
    m_LOCK = None
    m_analytic = None
    m_inTrainingLUT = None
    m_UUID_PREFIX = None
    m_API_TRAIN_DIR = None

    def __init__ (self, analytic, mLock, inTrainingLUT,
                  trainDir, cacheDir, allowedExts, uuidPrefix, **kwargs):
        print (f"calling {analytic} DownloadBaseClass constructor", flush=True)

        self.m_LOCK = mLock
        self.m_analytic = analytic
        self.m_UUID_PREFIX = uuidPrefix # eg FaceID_ or SpeakerID_
        self.m_API_TRAIN_DIR = Path ("api") / trainDir 
        self.m_inTrainingLUT = inTrainingLUT
        
    def get (self):
        """
        Download atak data package or enrollmenet/mission dataset.

        keys:
          mission:  eg., celebrity10
          download: "atak", "dataset", "performance"
          dry-run: eg. True   -- just return version info
        """
        print ("====GET====", flush=True)
        action = f"{request.path}:get"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_LOCK:       # prevent new Training from starting
            if not (("mission" in request.args) and ("download" in request.args)):
                errorMesg = "must specify both mission and download type"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error": errorMesg},
                        HTTP_BAD_REQUEST)

            download = request.args['download']
            mission = request.args['mission']

            if (download == "atak"):
                # Donwload ATAK data pacakge:
                action = 'download_atak_data_package'
                # check if enrollmen is currently in training
                (isTraining, errDict) = CheckTrainingStatus (
                    action, self.m_analytic, mission, self.m_inTrainingLUT)
                if isTraining:
                    LogRecord (user, action, request.args, "Training in progress")
                    return (errDict, HTTP_BAD_REQUEST)
                uuid = self.m_UUID_PREFIX + mission # see convention in run.bash
                atakFile = "atak_uuid=" + uuid + ".zip" # see convention in run.bash                
                atakFilePath = self.m_API_TRAIN_DIR / "classifier_models" / mission / atakFile
                print (f"atakFilePath = {atakFilePath}", flush=True)
                if not atakFilePath.is_file(): # check error
                    errorMesg = f"could not find atak data pacakge: {atakFilePath}"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "mission": mission, "error": errorMesg},
                            HTTP_BAD_REQUEST)
                
                if not ("dry-run" in request.args):
                    LogRecord (user, action, request.args)
                    return sendDownloadFile (str (atakFilePath))

                # just want version info
                fileVersionName = sendDownloadFile (str (atakFilePath), dryrun=True)
                action = f"{action}_dry-run"
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "mission": mission,
                         "version": fileVersionName},
                        HTTP_OK)
            
            if (download == "performance"):
                action = 'download_performance_evaluation'
                # check if enrollmen is currently in training
                (isTraining, errDict) = CheckTrainingStatus (
                    action, self.m_analytic, mission, self.m_inTrainingLUT)
                if isTraining:
                    LogRecord (user, action, request.args, "Training in progress")
                    return (errDict, HTTP_BAD_REQUEST)
                perfFile = f"{self.m_analytic}-3-fold-cross-validation.pdf"
                perfFilePath = self.m_API_TRAIN_DIR / "classifier_models" / mission / perfFile
                print (f"perfFilePath = {perfFilePath}", flush=True)
                if not perfFilePath.is_file(): # check error
                    errorMesg = f"could not find performance result : {perfFilePath}"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "mission": mission, "error": errorMesg},
                            HTTP_BAD_REQUEST)
                
                if not ("dry-run" in request.args):
                    LogRecord (user, action, request.args)
                    return sendDownloadFile (str (perfFilePath))

                # just want version info
                fileVersionName = sendDownloadFile (str (perfFilePath), dryrun=True)
                action = f"{action}_dry-run"
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "mission": mission,
                         "version": fileVersionName},
                        HTTP_OK)
            
            if (download == "dataset"):
                action = 'download_dataset'
                tmpDir = tempfile.TemporaryDirectory()
                datasetFile=f"{tmpDir.name}/{mission}.zip"
                zipSrcDir = self.m_API_TRAIN_DIR / "missions" / mission

                if not zipSrcDir.is_dir():
                    errorMesg = f"mission dirctory {str(zipSrcDir)} does not exits."
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "mission": mission, "error": errorMesg},
                            HTTP_BAD_REQUEST)
                
                cmd = (f"{self.m_API_TRAIN_DIR}/create_dataset_zip.bash " + 
                       f"{zipSrcDir} {datasetFile}")
                print (f"cmd = {cmd}", flush=True)
                retVal = os.system (cmd)
                success = retVal == 0
                if not success:
                    errorMesg = f"could not create dataset: {datasetFile}"
                    LogRecord (user, action, request.args, errorMesg)
                    return {"action":action, "success":False,
                            "mission": mission, "error": errorMesg}
                if not ("dry-run" in request.args):
                    LogRecord (user, action, request.args)
                    return sendDownloadFile (datasetFile, delete=True)
                # just want version info
                fileVersionName = sendDownloadFile (datasetFile, dryrun=True)
                action = f"{action}_dry-run"
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True,
                         "mission": mission,
                         "version": fileVersionName},
                        HTTP_OK)
                
    
                
            # failure
            errorMesg = f"could not understand download request: {download}"
            LogRecord (user, action, request.args, errorMesg)
            return ({"action":"download", "success":False, "error": errorMesg,
                     "mission": mission},
                    HTTP_BAD_REQUEST)



class MissionDuplicateBaseClass (Resource):
    m_LOCK = None
    m_analytic = None
    m_inTrainingLUT = None
    m_UUID_PREFIX = None
    m_API_TRAIN_DIR = None

    def __init__ (self, analytic, mLock, inTrainingLUT,
                  trainDir, cacheDir, allowedExts, uuidPrefix, **kwargs):
        print (f"calling {analytic} MissionBaseClass constructor", flush=True)

        self.m_LOCK = mLock
        self.m_analytic = analytic
        self.m_API_TRAIN_DIR = Path ("api") / trainDir 
        self.m_VIPS_DIR = Path ("api") / trainDir / "vips_with_profile_picture"
        self.m_MISSIONS_DIR = Path ("api") / trainDir / "missions"
        self.m_UUID_PREFIX = uuidPrefix # eg FaceID_ or SpeakerID_
        self.m_inTrainingLUT = inTrainingLUT
        
    def put (self):
        """Duplicate a specific mission.

        keys:
          old-mission:  eg., celebrity10
          new-mission:  eg., celebrity10_v2
        """
        print ("==== PUT ====", flush=True)
        action = f"{request.path}:put"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_LOCK:       # prevents new Trainig from starting
            if not (("new-mission" in request.args) and ("old-mission" in request.args)):
                errorMesg = "need to specify both old-mission and new-mission"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "error":errorMesg},
                        HTTP_BAD_REQUEST)
            action = "duplicate_missions"
            old_mission = request.args["old-mission"]
            new_mission = request.args["new-mission"]

            # check if old-mission exists
            oldMissionDir = self.m_MISSIONS_DIR / old_mission
            if not oldMissionDir.is_dir(): # check error
                errorMesg = f"old-mission '{str (oldMissionDir)}' does not exist"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "old-mission": old_mission,
                         "old-mission": str (oldMissionDir), "error":errorMesg},
                        HTTP_BAD_REQUEST)

            # check if new-mission exists
            newMissionDir = self.m_MISSIONS_DIR / new_mission
            if newMissionDir.is_dir(): # check error
                errorMesg = f"new-mission '{str (newMissionDir)}' already exists"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False, "new-mission": new_mission,
                         "new-mission": str (newMissionDir), "error":errorMesg},
                        HTTP_BAD_REQUEST)

            # check if old-mission is currently in training
            (isTraining, errDict) = CheckTrainingStatus (
                action, self.m_analytic, old_mission, self.m_inTrainingLUT)
            if isTraining:
                LogRecord (user, action, request.args, "Training in progress")
                return (errDict, HTTP_BAD_REQUEST)
            
            # duplicate the directories, preserve the symbolic link
            try:
                shutil.copytree (oldMissionDir, newMissionDir, symlinks=True)
            except Exception as inst:
                print(type(inst))    # the exception instance
                print(inst.args)     # arguments stored in .args
                print(inst)          # __str__ allows args to be printed directly,
                return ({"action":action, "success":False,
                         "old-mission": str (oldMissionDir),
                         "new-mission": str (newMissionDir),
                         "error": f"could not duplicate missions"},
                        HTTP_BAD_REQUEST)
            
            # succss
            LogRecord (user, action, request.args)
            return ({"action":action, "success":True, "old-mission":old_mission,
                     "new-mission":new_mission, "oldMissionDir":str(oldMissionDir),
                     "newMissionDir":str(newMissionDir)},
                    HTTP_OK)
        
        
class VIPDownloadBaseClass (Resource):
    m_LOCK = None
    m_analytic = None
    m_inTrainingLUT = None
    m_UUID_PREFIX = None
    m_API_TRAIN_DIR = None

    def __init__ (self, analytic, mLock, inTrainingLUT,
                  trainDir, cacheDir, allowedExts, uuidPrefix, **kwargs):
        print (f"calling {analytic} DownloadBaseClass constructor", flush=True)

        self.m_LOCK = mLock
        self.m_analytic = analytic
        self.m_UUID_PREFIX = uuidPrefix # eg FaceID_ or SpeakerID_
        self.m_API_TRAIN_DIR = Path ("api") / trainDir 
        self.m_inTrainingLUT = inTrainingLUT
        
    def get (self):
        """
        Download a file, a vip, or all vips

        keys:
          vip:  eg., Jim_Gaffigan
          file:  eg., profile.jpg
          dry-run: eg. True   -- just return version info
        """
        print ("====GET====", flush=True)
        action = f"{request.path}:get"
        user = request.args["user"]

        ## extract the required fields:
        with self.m_LOCK:       # prevent new Training from starting
            if not ("vip" in request.args):
                action = 'download_all_vips'
                tmpDir = tempfile.TemporaryDirectory()
                datasetFile=f"{tmpDir.name}/{self.m_analytic}_vips.zip"
                zipSrcDir = self.m_API_TRAIN_DIR / "vips_with_profile_picture"

                if not zipSrcDir.is_dir(): # check error
                    errorMesg = f"vips dirctory {str(zipSrcDir)} does not exits."
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False, "error": errorMesg},
                            HTTP_BAD_REQUEST)
            else:
                vip = request.args['vip']
                vipDir = self.m_API_TRAIN_DIR / "vips_with_profile_picture" / vip
                if ("file" in request.args):
                    # Download data file:
                    file = request.args['file']
                    action = "download_vip_datafile"
                    dataFile = vipDir / file
                    if not dataFile.is_file(): # check error
                        errorMesg = f"file {str(dataFile)} is not a file."
                        LogRecord (user, action, request.args, errorMesg)
                        return ({"action":action, "success":False,
                                 "vip": vip, "file": file, "error": errorMesg},
                            HTTP_BAD_REQUEST)
                    if not ("dry-run" in request.args):
                        LogRecord (user, action, request.args)
                        return sendDownloadFile (dataFile)

                    # just want version info
                    fileVersionName = sendDownloadFile (dataFile, dryrun=True)
                    action = f"{action}_dry-run"
                    LogRecord (user, action, request.args)
                    return ({"action":action, "success":True,
                             "vip": vip,
                             "file": file,
                             "version": fileVersionName},
                            HTTP_OK)

                # Download single vip:
                action = 'download_single_vip'
                tmpDir = tempfile.TemporaryDirectory()
                datasetFile=f"{tmpDir.name}/{vip}.zip"
                zipSrcDir = vipDir

                if not zipSrcDir.is_dir(): # check error
                    errorMesg = f"vip dirctory {str(zipSrcDir)} does not exits."
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False, "vip": vip, "error": errorMesg},
                            HTTP_BAD_REQUEST)

            # zip the directory (either single vip or all vips)
            cmd = (f"{self.m_API_TRAIN_DIR}/create_dataset_zip.bash " + 
                   f"{zipSrcDir} {datasetFile}")
            print (f"cmd = {cmd}", flush=True)
            retVal = os.system (cmd)
            success = retVal == 0
            if not success:
                errorMesg = f"could not create dataset: {datasetFile}"
                LogRecord (user, action, request.args, errorMesg)
                return {"action":action, "success":False, "error": errorMesg}
            if not ("dry-run" in request.args):
                LogRecord (user, action, request.args)
                return sendDownloadFile (datasetFile, delete=True)

            # just want version info
            fileVersionName = sendDownloadFile (datasetFile, dryrun=True)
            action = f"{action}_dry-run"
            LogRecord (user, action, request.args)
            return ({"action":action, "success":True,
                     "version": fileVersionName},
                    HTTP_OK)


class MiscBaseClass (Resource):
    m_LOCK = None
    m_cacheDir = None
    m_analytic = None
    m_inTrainingLUT = None
    
    def __init__ (self, analytic, mLock, inTrainingLUT,
                  trainDir, cacheDir, allowedExts, uuidPrefix, **kwargs):
        print (f"calling {analytic} MiscBaseClass constructor", flush=True)

        self.m_LOCK = mLock
        self.m_analytic = analytic
        self.m_cacheDir = cacheDir
        self.m_inTrainingLUT = inTrainingLUT

        
    def get (self):
        """ get log entries
        """
        print ("==== GET ====", flush=True)
        action = f"{request.path}:get"
        user = request.args["user"]

        with self.m_LOCK:       # prevents mulitple simultaneous log access
            # raw sql take priority
            if "raw-sql" in request.args: # eg. "SELECT * FROM enrollment;"
                rawSQL = request.args["raw-sql"]
                try:
                    rs = db.engine.execute (f"{rawSQL}") # creats new connection
                    print (f"rs result is {rs}")
                    results = rs.fetchall()
                except ResourceClosedError:
                    results = []
                    pass
                except Exception as inst:
                    print(f"Error type= {type(inst)}")    # the exception instance
                    # print(inst.args)     # arguments stored in .args
                    print(f"error arg = {inst}")    # __str__ allows args to be printed directly
                    errorMesg = "sql query failed"
                    LogRecord (user, action, request.args, errorMesg)
                    return ({"action":action, "success":False,
                             "raw-sql": rawSQL, "error":errorMesg}, HTTP_BAD_REQUEST)

                results_list = [{key: value for key, value in row.items()} for row in results]
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "logs":results_list}, HTTP_OK)


            if not "filter-user" in request.args:
                action = 'get_all_logs'
                results = db.session.query (EnrollmentLog).all()
                results_list = [result.serialize() for result in results]
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, "logs":results_list}, HTTP_OK)

            ## Read from Log db
            action = 'get_filtered_logs'
            fUser = request.args["filter-user"]
            results = db.session.query (EnrollmentLog).filter_by(user=fUser)
            # results = db.session.query (EnrollmentLog).all()
            results_list = [result.serialize() for result in results]

            LogRecord (user, action, request.args)
            return ({"action":action, "success":True, "logs":results_list}, HTTP_OK)


    def delete (self):
        """ delete cache
        """
        print ("==== DELETE ====", flush=True)
        action = f"{request.path}:delete"
        user = request.args["user"]

        with self.m_LOCK:       # prevents new Trainig from starting
            # Want to remove all cache
            action = 'delete_cache'

            # make sure no Training for analytic is taking place:
            if len (self.m_inTrainingLUT):
                errorMesg = (f"Analytic '{self.m_analytic}' is currently in training. " +
                             "Please try again later.")
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False,
                         "analytic": self.m_analytic, "error": errorMesg},
                        HTTP_BAD_REQUEST)
            
            print (f"Removing all cache for {self.m_analytic}..", flush=True)
            if not self.m_cacheDir.is_dir(): # check "error"
                LogRecord (user, action, request.args)
                return ({"action":action, "success":True, # empty cache is not an error
                         "cacheDir": str (self.m_cacheDir),
                         "warning": f"cacheDir does not exist"}, # warning instead 
                        HTTP_OK) # empty cache is not an error
            try:
                shutil.rmtree (self.m_cacheDir)
            except Exception as inst: # check error
                print(type(inst))    # the exception instance
                print(inst.args)     # arguments stored in .args
                print(inst)          # __str__ allows args to be printed directly,
                errorMesg = f"could not remove cacheDir"
                LogRecord (user, action, request.args, errorMesg)
                return ({"action":action, "success":False,
                         "cacheDir": str (self.m_cacheDir), "error": errorMesg},
                        HTTP_BAD_REQUEST)
            
            # succeed in deleting cache, we are done.
            LogRecord (user, action, request.args)
            return ({"action":action, "success":True,
                     "cacheDir": str (self.m_cacheDir)}, HTTP_OK)

        
class AnalyticRestAPIClass (Resource):
    """An abstract class that selects either speakerid and faceid
    analytics.  This craeats a single REST API drop point for both
    analytics.

    The REST API must supply a "analytic" parameter:
          'analytic': either 'speakerid' or 'faceid'
    """
    speakerID = None
    faceID = None
    analytic = None
    
    def __init__(self, RestAPIClass, **kwargs):
        """
        RestAPIClass : Either VipBaseClass, MissionBaseClass, or MiscBaseClass
        """
        # FaceID specific
        global g_faceIdLock
        global g_faceIdDataWriteLock
        global g_faceIdHasLock
        global g_faceIdTrainingLUT
        trainDir = "FaceIdTraining"
        cacheDir = Path ("/tmp/joblib/joblib/"+
                         "__main__--code-api-FaceIdTraining-create_face_dataset")
        kwargs = dict (kwargs, writeLock = g_faceIdDataWriteLock, hasLock = g_faceIdHasLock)
        self.faceID = RestAPIClass ("faceid", g_faceIdLock, g_faceIdTrainingLUT,
                                    trainDir, cacheDir, ALLOWED_IMG_EXTENSIONS,
                                    "faceID_", **kwargs)

        # SpeakerID specific
        global g_speakerIdLock
        global g_speakerIdDataWriteLock
        global g_speakerIdHasLock
        global g_speakerIdTrainingLUT
        trainDir = "SpeakerIdTraining"
        cacheDir = Path ("/tmp/joblib/joblib/"+
                         "__main__--code-api-SpeakerIdTraining-create_speech_embeddings/")
        kwargs = dict (kwargs, writeLock = g_speakerIdDataWriteLock, hasLock = g_speakerIdHasLock)
        self.speakerID = RestAPIClass ("speakerid", g_speakerIdLock, g_speakerIdTrainingLUT,
                                       trainDir, cacheDir, ALLOWED_SND_EXTENSIONS,
                                       "speakerID_", **kwargs)

    def checkAnalytic(self, endpoint):
        """
        Return: False, faceID, or speakerID analytic
        """
        assert ("user" in request.args)
        user = request.args ["user"]
        if not "analytic" in request.args:
            errorMesg = f"failed to provide 'analytic'"
            errResult = ({"action":endpoint, 'success':False, "error":errorMesg},
                         HTTP_BAD_REQUEST)
            LogRecord (user, endpoint, request.args, errorMesg)
            return (False, errResult)
        self.analytic = request.args['analytic']
        
        if not (self.analytic == "speakerid" or self.analytic == "faceid"):
            errorMesg = f"unrecognized analytic: '{self.analytic}'"
            errResult =  ({"action":endpoint, 'success':False,
                           "analytic": self.analytic, "error":errorMesg},
                          HTTP_BAD_REQUEST)
            LogRecord (user, endpoint, request.args, errorMesg)
            return (False, errResult)
        if self.analytic == "speakerid":
            return (self.speakerID, None)
        if self.analytic == "faceid":
            return (self.faceID, None)
        raise NameError('Invalid analytic. No supposed to be here.')


    def checkUser(self, endpoint):
        # check user:
        if not ("user" in request.args):
            errorMesg = "must provide user"
            errResult =  ({"action":endpoint, "success":False,
                           "error": errorMesg}, HTTP_BAD_REQUEST)
            LogRecord ("_missing_user_", endpoint, request.args, errorMesg)
            return (False, errResult)
        return (True, None)

    
    # @auth_required
    def get(self):
        endpoint = f"{request.path}:get"
        (hasUser, errResult) = self.checkUser (endpoint)
        if (hasUser == False) : return errResult
        (validAnalytic, errResult) = self.checkAnalytic (endpoint)
        if (validAnalytic == False): return errResult
        return validAnalytic.get()

    # @auth_required
    def put(self):
        endpoint = f"{request.path}:put"
        (hasUser, errResult) = self.checkUser (endpoint)
        if (hasUser == False) : return errResult
        (validAnalytic, errResult) = self.checkAnalytic (endpoint)
        if (not validAnalytic): return errResult
        return validAnalytic.put()

    # @auth_required
    def delete(self):
        endpoint = f"{request.path}:delete"
        (hasUser, errResult) = self.checkUser (endpoint)
        if (hasUser == False) : return errResult
        (validAnalytic, errResult) = self.checkAnalytic (endpoint)
        if (not validAnalytic): return errResult
        return validAnalytic.delete()
    
    # @auth_required
    def post(self):
        endpoint = f"{request.path}:post"
        (hasUser, errResult) = self.checkUser (endpoint)
        if (hasUser == False) : return errResult
        (validAnalytic, errResult) = self.checkAnalytic (endpoint)
        if (not validAnalytic): return errResult
        return validAnalytic.post()

    
class VipClass (AnalyticRestAPIClass):
    """VIP-related REST API.
    """
    def __init__(self):
        super().__init__(VipBaseClass, asdf="hello")

        
class MissionClass (AnalyticRestAPIClass):
    """VIP-related REST API.
    """
    def __init__(self):
        super().__init__(MissionBaseClass)

        
class VIPDownloadClass (AnalyticRestAPIClass):
    """REST API for downloading VIP-related data
    """
    def __init__(self):
        super().__init__(VIPDownloadBaseClass)


class MissionDownloadClass (AnalyticRestAPIClass):
    """REST API for downloading mission-related data
    """
    def __init__(self):
        super().__init__(MissionDownloadBaseClass)

class MissionDuplicateClass (AnalyticRestAPIClass):
    """REST API for downloading mission-related data
    """
    def __init__(self):
        super().__init__(MissionDuplicateBaseClass)
        
class MiscClass (AnalyticRestAPIClass):
    """REST API for miscellaneous tasks
    """
    def __init__(self):
        super().__init__(MiscBaseClass)

    
## Define interfaces
api.add_resource (VipClass, '/vip')
api.add_resource (MissionClass, '/mission')
api.add_resource (VIPDownloadClass, '/vip/download')
api.add_resource (MissionDownloadClass, '/mission/download')
api.add_resource (MissionDuplicateClass, '/mission/duplicate')
api.add_resource (MiscClass, '/misc')
