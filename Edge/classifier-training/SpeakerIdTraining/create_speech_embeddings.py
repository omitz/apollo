#! /usr/bin/env python3
#
# This program extracts embedding from audio file and store them in an
# output pickle file.
#
# TC 2021-05-19 (Wed) 


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import subprocess
import json
import pandas as pd
import pickle
import numpy as np
from time import time

from pandarallel import pandarallel
from vosk import Model, KaldiRecognizer, SpkModel, SetLogLevel

import joblib
from joblib import Memory
mem = Memory(cachedir='/tmp/joblib', verbose=1)


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
g_model = None
g_spk_model = None

#-------------------------
# Private Implementations 
#-------------------------
def LoadModels (modelPath, spkModelPath):
    if not os.path.exists (modelPath):
        print ("Please download the model from " +
               "https://alphacephei.com/vosk/models and unpack as " +
               "{} in the current folder.".format(model_path))
        exit (1)

    if not os.path.exists (spkModelPath):
        print ("Please download the speaker model from " +
               "https://alphacephei.com/vosk/models and unpack as " +
               "{} in the current folder.".format(spk_model_path))
        exit (1)

    global g_model
    global g_spk_model
    g_model = Model (modelPath)
    g_spk_model = SpkModel(spkModelPath)


def GetFileMetaInfo (fileName):
    """
    Return a unique string about the file.
    """

    ## use hash
    ## use file size and date
    fstat = os.stat (fileName)
    return f"{fstat.st_mtime}{fstat.st_size}"
    

def GetEmbeddingsFromListFile (list_file, max_duration=60):
    """This routine extracts embeddings from audio files from a text file.
    (list_file).  Each each audio file should contain at least 4
    seconds of speech.

    max_duration : seconds

    return: embeddings and their corresponding speakers

    """
    @mem.cache
    def getEmbedding (fn_metaId):
        """
        fn : audio file name
        Return : X-vector 
        """
        global g_model
        global g_spk_model
        (fn, metaId) = fn_metaId
        # fn = fn_metaId
        
        # wf = wave.open (fn, "rb")
        # rec = KaldiRecognizer(model, spk_model, 16000)
        # if wf.getnchannels() != 1 or wf.getsampwidth() != 2 or wf.getcomptype() != "NONE":
        #     print ("Audio file must be WAV format mono PCM.")
        #     exit (1)
        # assert (wf.getframerate() == sample_rate)

        # ts = time()
        # te = time()
        # print ('%2.2f sec' % (te-ts))

        sample_rate = 16000
        rec = KaldiRecognizer (g_model, g_spk_model, sample_rate)
        try:
            print (f"opening file {fn}")
            process = subprocess.Popen (['ffmpeg', '-loglevel', 'quiet', '-i',
                                         fn, '-ar', str(sample_rate) , '-ac', '1',
                                         '-f', 's16le', '-'],
                                        stdout=subprocess.PIPE)
        except:
            print (f"Error: ffmpeg could not read {fn}", flush=True)
            1/0
            return np.zeros (128)

        # count = 0
        embeddings = []
        totalLen = 0
        maxLen = (max_duration * sample_rate * 2)
        while True:
            # data = wf.readframes(4000)
            data = process.stdout.read(4000)
            dataLen = len(data)
            if dataLen == 0:
                break
            totalLen += dataLen
            if (totalLen > maxLen):
                print ("audio length truncated to max duration ", max_duration)
                break
            # count += 1
            # rec.AcceptWaveform(data)
            if rec.AcceptWaveform(data):
                # print ("*** count = ", count)
                res = json.loads(rec.Result())
                if 'spk' in res:
                    # print ("Text:", res['text'])
                    embeddings.append(res['spk'])
        res = json.loads(rec.FinalResult())
        if 'spk' in res:
            embeddings.append(res['spk'])
        # print ("     len(embeddings) = ", len(embeddings))
        # avgEmbedding = np.mean (np.array(embeddings), axis=0)
        # print ("Text:", res['text'])
        # return avgEmbedding
        # return res['spk']
        
        # there are no results
        nEmbeddings = len (embeddings)
        print (f"nEmbeddings = {nEmbeddings}")
        if nEmbeddings == 0:
            print ("WARNNING !**** failed to extract speaker for file " + fn, flush=True)
            1/0                 # TBF: we purposely crash to get attention..
            return np.zeros (128)

        # there are mulitple results
        if nEmbeddings > 1:
            # avgEmbedding = np.mean (np.array(embeddings[:-1]), axis=0) # skip the last embedding
            avgEmbedding = np.mean (np.array(embeddings), axis=0)
            return avgEmbedding.tolist() # everything is list to save to pickle

        # there is only one result
        return embeddings[0]
        
    #
    result = pd.read_csv (list_file, delimiter=",")
    # apply to each element
    # result['embedding'] = result['filename'].apply(getEmbedding)
    # result['embedding'] = result['filename'].parallel_apply(getEmbedding)
    realFileNames = pd.Series ([(os.path.realpath (filename), GetFileMetaInfo (filename))
                                for filename in result['filename']])
    result['embedding'] = realFileNames.parallel_apply (getEmbedding)

    # return result[['filename','speaker','embedding']]
    trainX = np.asarray (result['embedding'].to_list()) # use double to match java side
    # trainX = np.asarray (result['embedding'].to_list(), dtype=np.float32)
    trainy = result['speaker'].to_list()
    # return [result['embedding'], result['speaker']]
    return [trainX, trainy]


def init ():
    ## 
    pandarallel.initialize()
    SetLogLevel(-1)
    
    ## Load the embedding model
    LoadModels ("model-eng", "model-spk")
    

def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("speechFileList", help="The file containing the list of audio files.")
    parser.add_argument ("speechEmbeddingFile", help="The output speech embedding pickle file.")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")

    # Specify Example:
    parser.epilog='''Example:
        %s enroll_list.csv speech-embeddings.pkl
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
        return args
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)


#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------
    # Create a parser:
    args = parseCommandLine ()

    # # Access the options:
    print ("speechFileList = ", args.speechFileList)
    print ("speechEmbeddingFile = ", args.speechEmbeddingFile)
    input ("press enter to continue")

    #---------------------------
    # run the program :
    #---------------------------
    # pandarallel.initialize()
    # SetLogLevel(-1)
    
    # ## Load the embedding model
    # LoadModels ("model-eng", "model-spk")
    init ()
    
    ## Extract embeddings
    (trainX, trainy) = GetEmbeddingsFromListFile (args.speechFileList)

    # Save embeddings
    pickle.dump ([trainX, trainy], open (args.speechEmbeddingFile, 'wb'))
    
    # 

    
    #---------------------------
    # program termination:
    #---------------------------
    print ("wrote to ", args.speechEmbeddingFile)
    print ("Program Terminated Properly\n", flush=True)

