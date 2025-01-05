#! /usr/bin/env python3
#
# Count the overall length of the (English) speech.
#
# TC 2021-09-21 (Tue) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import subprocess
import json
from vosk import Model, KaldiRecognizer, SpkModel, SetLogLevel
import json

from uuid import uuid4


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


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


def sh_(shell_command: str) -> tuple:
    """
    :param shell_command: your shell command
    :return: ( 1 | 0, stdout)
    ref: stackexchange
    """
    logfile: str = '/dev/shm/%s' % uuid4().hex
    cmd: str = '%s > %s' % (shell_command, logfile) # may not be bash.. can't use '&>'
    print (f"cmd = {cmd}")
    err: int = os.system(cmd)
    out: str = open(logfile, 'r').read().strip()
    os.remove(logfile)
    return err, out

    
def verify_speech (fn, max_duration):
    """
    fn : audio file name
    Return : (True/False, {"error":xxx, "speech-len":xxx, "audio-len":xxx, "max-audio-len": xxx})
    """
    global g_model
    global g_spk_model

    # get the total duration
    (retVal, audioLen_str) = sh_ ("ffprobe -v error -show_entries format=duration " +
                                  f"-of default=noprint_wrappers=1:nokey=1 {fn}")
    if (retVal != 0):
        return (False, {"error" : f"ffprobe could not read {fn}",
                        "speech-len":0, "audio-len" :0,
                        "audio-max-length" : max_duration})
    print (f"audioLen_str = '{audioLen_str}'")
    
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
        return (False, {"error" : f"ffmpeg could not read {fn}",
                        "speech-len":0,
                        "audio-len" : float (audioLen_str),
                        "audio-max-length" : max_duration})

    totalSpk_sec = 0
    totalLen = 0
    maxLen = (max_duration * sample_rate * 2)
    all_metaInfo = [] # meta info = confidence, timestamp (end, begin), word itself.
    while True:
        data = process.stdout.read(4000)
        dataLen = len(data)
        if dataLen == 0:
            break
        totalLen += dataLen
        if (totalLen > maxLen):
            print ("audio length truncated to max duration ", max_duration)
            break
        if rec.AcceptWaveform(data):
            # print ("*** count = ", count)
            res = json.loads(rec.Result())
            totalSpk_sec += res.get('spk_frames', 0)
            print ("pause detected")
            print (f"speech duration is {totalSpk_sec/100}")
    res = json.loads(rec.FinalResult())
    totalSpk_sec += res.get('spk_frames', 0)
    # print (f" final res = {res}")
    # print (f" final spk frames = {res['spk_frames']}")

    totalSpk_sec /= 100         # 100 frames per second
    print (f"Total speech duration is {totalSpk_sec}")
    if totalSpk_sec < 4.0:
        print (f"speech duration is {totalSpk_sec} < 4.0")
        return (False, {"error" : f"English speech duration is {totalSpk_sec} < 4.0 s",
                        "speech-len":totalSpk_sec,
                        "audio-len" : float (audioLen_str),
                        "audio-max-length" : max_duration})
    return (True, {"error" : "", "speech-len" : totalSpk_sec,
                   "audio-len" : float (audioLen_str),
                   "audio-max-length" : max_duration})
    


def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("inputFile", help="The input audio file")
    parser.add_argument ("maxAudoSec", help="Max audio length (in seconds) to process")
    parser.add_argument ("jsonOutFile", nargs='?', help="optional json output file")

    # Specify Example:
    parser.epilog='''Example:
        %s file.wav 30
        %s file.aac 30 json.out
        %s file.m4a 30
        ''' % (sys.argv[0], sys.argv[0], sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------
    # Create a parser:
    args = parseCommandLine ()

    # Access the options:
    print ("args.inputFile = ", args.inputFile)
    print ("maxAudoSec = ", args.maxAudoSec)
    print ("jsonOutFile = ", args.jsonOutFile)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    SetLogLevel(-1)
    progDir = os.path.split (os.path.abspath (sys.argv[0]))[0]
    print (f"progDir = {progDir}")
    LoadModels (progDir + "/model-eng", progDir + "/model-spk")

    (valid, meta) = verify_speech (args.inputFile, int (args.maxAudoSec))
    if args.jsonOutFile:
        encoded = json.dumps(meta)
        with open(args.jsonOutFile, 'w') as outFile:
            outFile.write (encoded)
        # with open(args.jsonOutFile, 'r') as outFile:
        #     encoded = outFile.read ()
        #     obj = json.loads(encoded)
        
    if valid:
        sys.exit (os.EX_OK)  
    
    sys.exit (os.EX_SOFTWARE)   # internal software error


    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

