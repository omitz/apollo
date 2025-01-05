#! /usr/bin/env python3
#
# This program plots the threshold study.
#
# TC 2021-09-13 (Mon) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import show_cross_validation_result
import pickle 
from pylab import *

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
    parser.add_argument ("k", help="k used in cross validation")
    parser.add_argument ("foldDir", help="directory to save the fold data ")
    parser.add_argument ("outFilePdf", help="output pdf file")

    # Specify Example:
    parser.epilog='''Example:
        %s 3 speakerid_threshold_study
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
    print ("args.k = ", args.k)
    print ("args.foldDir = ", args.foldDir)
    print ("args.outFilePdf = ", args.outFilePdf)
    input ("press enter to continue")
    print ("")


    #---------------------------
    # run the program :
    #---------------------------
    k = int (args.k)
    foldData = show_cross_validation_result.getFoldData (args.foldDir, k)

    
    ## We want to create a graph as function of threshold
    precisions = []
    recalls = []
    tresVals = range (0, 101, 5)
    for thres in tresVals:
        (cnfsnMtrxs, clssfctonReport, clssfctonDict) = (
            show_cross_validation_result.getAnalysis (foldData, thres))
        print (clssfctonDict['weighted avg'])
        precisions.append (clssfctonDict['weighted avg']['precision'])
        recalls.append (clssfctonDict['weighted avg']['recall'])


    ## Generate the plot
    plot (tresVals, precisions, 'r-x')
    plot (tresVals, recalls, 'g-o')

    xlabel ("Score Threshold")
    legend (["Weighted Avg. Precision", "Weighted Avg. Recall"])
    title (f"SpeakerID\n{k}-Fold Stratified Cross Validation vs Score")
    axvline (x=50, ls='--', color='k')

    # savefig (f"{args.outFileStem}.png", transparent=True,
    #          pad_inches=0, bbox_inches='tight')
    savefig (f"{args.outFilePdf}", transparent=True,
             pad_inches=0, bbox_inches='tight')

    print (f"created {args.outFilePdf}")
    # show ()


    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)
    
