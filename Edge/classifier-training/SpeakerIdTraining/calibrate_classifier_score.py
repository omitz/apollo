#! /usr/bin/env python3
#
#
# TC 2021-05-21 (Fri) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import pickle


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------



def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("classiferFile", help="The classifier")
    parser.add_argument ("labelFile", help="The classifier label mapping file")
    parser.add_argument ("classiferMetaFile", help="The output classifier meta file")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")

    # Specify Example:
    parser.epilog='''Example:
        %s svm.pkl label meta_vosk.pkl
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
    print ()
    print ("classiferFile = ", args.classiferFile)
    print ("labelFile", args.labelFile)
    print ("classiferMetaFile", args.classiferMetaFile)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    uSpeakers = [line.strip() for line in open (args.labelFile, "r").readlines()]
    fiftyPoint = 40             # TBD
    pickle.dump ([uSpeakers, fiftyPoint], open (args.classiferMetaFile, 'wb'))


    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

