#!/usr/bin/env python3
"""
Stand-alone vosk Kaldi-based program.

2020-07-10
"""

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import sys
import os
import argparse
import textwrap
import json
import subprocess
from pathlib import PosixPath, Path
from vosk import Model, KaldiRecognizer
import io
from typing import Union

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
g_currentPath = Path(os.path.dirname(os.path.realpath(__file__)))
g_model = None
    

def run (inAudioPath: PosixPath, outTxtPath: Union[PosixPath,io.StringIO],
         outMetaPath : PosixPath = None) -> bool:
    """Runs the vosk speech-to-text program.
    
    Args:
      inAudioPath: 
        The audio file (can be any file format recognized by ffmpeg).
      outTxtPath:
        The transcribed text.  To have the output dump to screen, just
        set it to pathlib.Path("/dev/stdout").  
        Can also take in-memory file (i.e, io.StringIO type)
      outMetaPath:  optional
        The meta data in json format.  The meta data can be use to obtain location,
        confidence, and time stamp of each word.  The output format is
        [(conf, begin_ts, end_ts, word1) ...  (conf, begin_ts, end_ts, wordn)]

    Returns:
      True: successful.
      False: unsuccessful.  See error messages.

    """
    global g_model
    if os.path.exists (g_currentPath / "model"):
        if g_model == None:
            # Load the Kaldi model only once.
            print ("loading model for the first time.", flush=True)
            g_model = Model(str(g_currentPath / "model"))
    else:
        print ("Error: Missing kaldi model.", flush=True)
        return False

    # Use ffmpeg to directly load the audio file (taken from vosk example)
    kaldi = KaldiRecognizer (g_model, 16000) # reset timestamp
    sample_rate=16000
    try:
        process = subprocess.Popen(['ffmpeg', '-loglevel', 'quiet', '-i',
                                    inAudioPath,
                                    '-ar', str(sample_rate) , '-ac', '1', '-f', 's16le', '-'],
                                   stdout=subprocess.PIPE)
    except:
        print (f"Error: ffmpeg could not read {inAudioPath}", flush=True)
        return False
        
    # Process the audio file:
    all_text = []
    all_metaInfo = [] # meta info = confidence, timestamp (end, begin), word itself.
    while True:
        # data = wf.read(4000)
        data = process.stdout.read(4000)
        if len(data) == 0:
            break
        if kaldi.AcceptWaveform(data):
            ## This may be expensive to do -- convert from json to dict and then back to list.
            ## We could just parse the json string directly..
            res = json.loads (kaldi.Result())
            result = res.get('result', '') # if no speech was detected, res will be {'text': ''}
            all_metaInfo += map (lambda elm: tuple(elm.values()),
                                 result)
            if len(res['text']) > 0:
                all_text.append (res['text'] + ", ")

    # concactenate the transcribed result:
    res = json.loads (kaldi.FinalResult())
    result = res.get('result', '') # if no audio was detected, res will be {'text': ''}
    all_metaInfo += map (lambda elm: tuple(elm.values()),
                         result) # each elment is a tuple (confidence end_ts begin_ts word)
    if len(res['text']) > 0:
        all_text.append (res['text'] + ".")
    finalText=''.join(all_text) # faster way than to use '+' operator

    # check no output -- audio problem??
    if (all_text == "."):
        print (f"Error: Could not read audio {inAudioPath}.", flush=True)
        return False

    # Verify we get correct meta data.
    assert (len (all_metaInfo) == len (finalText.split()))

    # write output
    if type (outTxtPath) == io.StringIO:
        outTxtPath.write ("%s\n" % finalText)
    else:
        outTxtPath.write_text ("%s\n" % finalText)
    if outMetaPath:
        outMetaPath.write_text (json.dumps (all_metaInfo))
    # print (f"wrote to {outMetaPath}")
    # print (f"json.dumps (all_metaInfo) = ", json.dumps (all_metaInfo))

    # clean up
    process.stdout.close()
    process.wait()              # make sure the process terminates
    return True


def parseCommandLine () -> argparse.Namespace:
    """Parse commandline argurments.

    Returns:
      An argparse.Namespace object whose member variables correspond
      to commandline agruments. For example the "--debug" commandline
      option becomes member variable .debug.
    """
    
    description="""
    A speech-to-text program using Kaldi models.
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("audioFile", help="input audio file")
    parser.add_argument ("resultFile", help="output transcription result")
    parser.add_argument ("metaFile", help="output meta data in json format")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")

    # Specify Example:
    parser.epilog='''Example:
    %s demo3.wav out.txt meta.data
    ''' % (sys.argv[0])

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
    print ("args.audioFile = ", args.audioFile)
    print ("args.resultFile = ", args.resultFile)
    print ("args.metaFile = ", args.metaFile)

    #---------------------------
    # run the program :
    #---------------------------
    run (Path(args.audioFile), Path(args.resultFile), Path(args.metaFile))

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

