#! /usr/bin/env python3
#
#
#
# TC 2021-05-20 (Thu)


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import joblib
from sklearn_porter import Porter
from sklearn_porter.language import *


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
    parser.add_argument ("classiferJson", help="The output classifier json file")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    
    # Specify Example:
    parser.epilog='''Example:
        %s svm.pkl svm_vosk.json
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
    print ("classiferFile", args.classiferFile)
    print ("classiferJson", args.classiferJson)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    # 1.) load the scikit classifier and port it to java
    estimator = joblib.load (args.classiferFile)
    porter = Porter (estimator, language="java")
    output = porter.export (class_name=None, method_name="predict",
                            export_dir="./", export_data=True,
                            export_append_checksum=False,
                            details=True, export_filename=args.classiferJson)
    # Save transpiled estimator:
    # with open ("SVC.java", 'w') as file_:
    #     file_.write(output.get('estimator'))
    

    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

