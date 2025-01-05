#!/usr/bin/env python3

# required imports
#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import argparse, sys
import textwrap
import sys                                 # exit, argv
import gc

import os
import subprocess

import scipy.io.wavfile as wave
from tools.System.config import cfg
from tools.NeMo.demo_inference import offline_inference



#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------


def ParseArg ():
    #------------------------------
    # parse command-line arguments:
    #------------------------------
    # Create a parser:
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("wavDir", help="input directory containing wav files")
    parser.add_argument ("outFile", help="output transcription")

    # Specify Example:
    parser.epilog='''Example:
        %s CDR_meeting_audio out.txt
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)

    return args


# format audio clip (sampling rate)
def format_audio_rate(audio_clip_in, audio_clip_out):
    subprocess.check_output("sox -v 0.98 {0} -c 1 -r 16000 {1}".
                            format(audio_clip_in, audio_clip_out), shell=True)
    sample_rate, signal = wave.read(audio_clip_out)
    print("Audio has been formatted and has sample rate: " + str(sample_rate))
    return audio_clip_out


#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    args = ParseArg ()
    
    #---------------------------
    # run the program :
    #---------------------------
    transcripts = []
    dirName = args.wavDir
    wavFiles = sorted ([os.path.join (dirName, f) for f in os.listdir (dirName) 
                        if f.endswith (".wav")])

    # test to see if wav file has 3 fields:
    # prefix, startTime, spkrId
    isDiarizationFormat = False
    try:
        wavFile = wavFiles[0]
        (_, fileName) = os.path.split (wavFile)
        prefix, startTime, spkrId = fileName.split ('_')
        spkrId, _ = spkrId.split ('.')
        isDiarizationFormat = True
    except:
        pass

    with open (args.outFile, "w") as outFile:
        for audio_file in wavFiles:

            ## Multi Model:
            # MODEL_YAML = os.path.join(cfg.NEMO.BASE_PATH,
            #                           'examples/asr/configs/jasper10x5dr.yaml')
            # CHECKPOINT_ENCODER = 'models/multi_dataset_v2/JasperEncoder-STEP-400000.pt'
            # CHECKPOINT_DECODER = 'models/multi_dataset_v2/JasperDecoderForCTC-STEP-400000.pt'
            # text, f_probs = offline_inference (MODEL_YAML, CHECKPOINT_ENCODER,
            #                                            CHECKPOINT_DECODER, audio_file)
            # text = text[0]
            
            # ## am-finetuned-WSJ_train_speed–lm-WSJ
            # MODEL_YAML = os.path.join(cfg.NEMO.BASE_PATH,
            #                           'examples/asr/configs/quartznet15x5.yaml')
            # CHECKPOINT_ENCODER = 'models/wsj_finetuned/JasperEncoder-STEP-174000.pt'
            # CHECKPOINT_DECODER = 'models/wsj_finetuned/JasperDecoderForCTC-STEP-174000.pt'
            # lm = 'models/lm/WSJ_lm.binary'
            # f_transcript, f_preds, text = offline_inference (
            #     MODEL_YAML, CHECKPOINT_ENCODER, CHECKPOINT_DECODER, audio_file, lm)
            # # text, f_probs = offline_inference (
            # #     MODEL_YAML, CHECKPOINT_ENCODER, CHECKPOINT_DECODER, audio_file)
            # text = text[0]
            # print ("text = ", text)


            ## Jasper10x5DR/ model
            MODEL_YAML = os.path.join (
                os.getcwd(), 'models/Jasper10x5DR/jasper10x5dr.yaml')
            CHECKPOINT_ENCODER = os.path.join (
                os.getcwd(), 'models/Jasper10x5DR/JasperEncoder-STEP-265520.pt')
            CHECKPOINT_DECODER = os.path.join (
                os.getcwd(), 'models/Jasper10x5DR/JasperDecoderForCTC-STEP-265520.pt')
            text, f_probs = offline_inference (MODEL_YAML, CHECKPOINT_ENCODER,
                                               CHECKPOINT_DECODER, audio_file)
            text = text[0]

            if isDiarizationFormat:
                (_, fileName) = os.path.split(audio_file)
                _, startTime, spkrId = fileName.split ('_')
                spkrId, _ = spkrId.split ('.')
                prefix = "[%s] Spkr %s : " % (startTime, spkrId)
                transcripts.append (prefix + text)
            else:
                transcripts.append (text)

            outFile.write ("%s\n\n" % transcripts[-1])
            outFile.flush ()

            ## save memory?
            gc.collect()
            gc.garbage
            del gc.garbage[:]

            


    # with open (args.outFile, "w") as outFile:
    #     # outFile.write (transcripts)
    #     outFile.writelines("%s\n\n" % text for text in transcripts)
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n")

