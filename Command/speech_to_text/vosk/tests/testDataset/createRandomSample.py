#! /usr/bin/env python3
#
# This program randomly selects 50 samples from the Mozilla Common
# voice dataset obtained from :
#
#  https://www.kaggle.com/chetvertakov/common-voice-dataset
#
# TC 2021-02-22 (Mon) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import shutil

import pandas as pd

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
    parser.add_argument ("arg1", help="argument1 here")
    parser.add_argument ("arg2", nargs='?', help="optional argument2 here")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")
    parser.add_argument ("vars", nargs='+', help="arbitrary many varialbes")

    # Specify Example:
    parser.epilog='''Example:
        %s a b --verbose v1 v2 v3
        %s --debugMode 1 a b v1 v2 v3
        ''' % (sys.argv[0], sys.argv[0])

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
    # args = parseCommandLine ()

    # Access the options:

    #---------------------------
    # run the program :
    #---------------------------
    metadata = pd.read_csv('metadata.csv')
    metadata.info()
    rdnSample = metadata.sample (n=50)

    # just want text and filename
    rdnSample = rdnSample[["text", "filename"]]

    # create a samples directory and copy the sample audio there
    try:
        shutil.rmtree ('samples/')
    except:
        pass
    os.mkdir ('samples')
    [ shutil.copyfile(src, src.replace('cv-valid-train', 'samples'))
      for src in rdnSample["filename"]]
    
    # Save to disk
    rdnSample.filename = [fn.replace('cv-valid-train', 'samples')
                          for fn in rdnSample.filename]
    rdnSample.to_csv ("samples.csv", index=False)
    

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)


