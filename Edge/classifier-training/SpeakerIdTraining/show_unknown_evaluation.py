#!/usr/bin/env python3
# 
# This program takes the unknown score file and apply a rejection threshold.
#
# TC 2021-09-10 (Fri) --



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
    parser.add_argument ("scoreFile", help="The input score data ")
    parser.add_argument ("thres", help="score threshold, eg 50")

    # Specify Example:
    parser.epilog='''Example:
        %s unknown_scores.pkl 50
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
    print ("args.scoreFile = ", args.scoreFile)
    print ("args.thres = ", args.thres)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    # load the score
    scores = pickle.load (open (args.scoreFile, 'rb'))

    # apply threshold
    thres = int (args.thres)
    rejectionRate = sum (scores < thres)
    print (f"rejectionRate = {rejectionRate}")
    
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)


