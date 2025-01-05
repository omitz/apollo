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

from shapely.geometry import LineString
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
    parser.add_argument ("thres", help="score threshold, eg 50")
    parser.add_argument ("scoreFile", help="The input score data ")
    parser.add_argument ("outFileStem",
                         help="output graphic file without extension")

    # Specify Example:
    parser.epilog='''Example:
        %s 3 50 unknown_scores.pkl faceid_threshold_study
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
    print ("args.thres = ", args.thres)
    print ("args.scoreFile = ", args.scoreFile)
    print ("args.outFileStem = ", args.outFileStem)
    input ("press enter to continue")
    print ("")


    #---------------------------
    # run the program :
    #---------------------------
    k = int (args.k)
    thres = float (args.thres)
    foldData = show_cross_validation_result.getFoldData (k)
    (cnfsnMtrxs, clssfctonReport, clssfctonDict) = (
        show_cross_validation_result.getAnalysis (foldData, thres))


    ## load the unkonw rejections scores
    scores = pickle.load (open (args.scoreFile, 'rb'))
    
    ## We want to create a graph as function of threshold
    precisions = []
    recalls = []
    rejections = []
    tresVals = range (0, 101, 5)
    for thres in tresVals:
        (cnfsnMtrxs, clssfctonReport, clssfctonDict) = (
            show_cross_validation_result.getAnalysis (foldData, thres))
        print (clssfctonDict['weighted avg'])
        precisions.append (clssfctonDict['weighted avg']['precision'])
        recalls.append (clssfctonDict['weighted avg']['recall'])

        rejectionRate = sum (scores < thres) / 100.0
        rejections.append (rejectionRate)

    ## Generate the plot
    plot (tresVals, precisions, 'r-x')
    plot (tresVals, recalls, 'g-o')
    plot (tresVals, rejections, 'b-d')

    # find the intersection between recall and rejection
    x = np.array (tresVals)
    f = np.array (recalls)
    g = np.array (rejections)
    first_line = LineString (np.column_stack((x, f)))
    second_line = LineString (np.column_stack((x, g)))
    intersection = first_line.intersection(second_line)
    
    if intersection.geom_type == 'MultiPoint':
        plot (*LineString(intersection).xy, 'ko')
    elif intersection.geom_type == 'Point':
        plot (*intersection.xy, 'ks')

    xlabel ("Score Threshold")
    legend (["Precision", "Recall", "Unknown Rejection", "%.2f" % intersection.x])
    title ("FaceID Score Study")
    axvline (x=50, ls='--', color='k')

    savefig (f"{args.outFileStem}.png", transparent=True,
             pad_inches=0, bbox_inches='tight')
    savefig (f"{args.outFileStem}.pdf", transparent=True,
             pad_inches=0, bbox_inches='tight')

    print (f"created {args.outFileStem}.pdf/png")
    # show ()


    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)
    
