#! /usr/bin/env python3
#
# This program takes the cross validation result and present them as a
# averaged confusion matrix.
#
# TC 2021-09-09 (Thu) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import pickle
from sklearn.metrics import confusion_matrix
from sklearn.metrics import classification_report

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------

def getFoldData (foldDir, k):
    files = [os.path.join (foldDir, f"{k}-fold{idx}.pkl") for idx in range(k)]
    foldData = [pickle.load (open (fn, 'rb')) for fn in files]
    return foldData


def getAnalysis (foldData, thres):
    cnfsnMtrxs = []
    y_test_all = []
    y_pred_all = []
    all_target_names = []

    for foldDatum in foldData:
        [y_test, y_pred, scores, labels, target_names] = foldDatum
        nClasses = len (labels)
        labels = range (nClasses+1) # take account into unknow class
        if len (all_target_names) < len (target_names):
            all_target_names = target_names.copy() # important

        y_pred_thres = y_pred.copy() # important
        y_pred_thres [ scores < thres ] = nClasses # mark it unknown if score is too low
        cnfsnMtrx = confusion_matrix (y_test, y_pred_thres, labels=labels)
        cnfsnMtrxs.append (cnfsnMtrx)
        y_test_all += y_test.tolist()
        y_pred_all += y_pred_thres.tolist()

    try:
        # Test to see if we need the Unknow class.  This happens if
        # the threshold is low enough so everything is high confident
        classification_report (y_test_all, y_pred_all, target_names=all_target_names)
    except:
        all_target_names.append ("Unknown")

    clssfctonReport = classification_report (y_test_all, y_pred_all,
                                             target_names = all_target_names)
    clssfctonDict = classification_report (y_test_all, y_pred_all,
                                           target_names = all_target_names,
                                           output_dict = True)
    return (cnfsnMtrxs, clssfctonReport, clssfctonDict)


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
    parser.add_argument ("thres", help="score threshold, eg 50")
    parser.add_argument ("outPdf", nargs='?', help="output pdf file")

    # Specify Example:
    parser.epilog='''Example:
        %s 3 50
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
    print ("args.thres = ", args.thres)
    print ("args.outPdf = ", args.outPdf)
    input ("press enter to continue")
    print ("")

    #---------------------------
    # run the program :
    #---------------------------
    # create the k confusion matrices as an accumulated confusion matrix
    k = int (args.k)
    thres = float (args.thres)
    foldData = getFoldData (args.foldDir, k)
    (cnfsnMtrxs, clssfctonReport, clssfctonDict) = getAnalysis (foldData, thres)

    #  accumulated confusion matrix
    print (sum (cnfsnMtrxs))
    print (clssfctonReport)

    # output to string 
    import io
    fid = io.StringIO()
    print ("args.k = ", args.k, file=fid)
    print ("args.foldDir = ", args.foldDir, file=fid)
    print ("args.thres = ", args.thres, file=fid)
    print (sum (cnfsnMtrxs), file=fid)
    print (clssfctonReport, file=fid)
    fid.seek(0)

    # output to pdf
    if args.outPdf:
        from fpdf import FPDF 
        pdf = FPDF()      
        pdf.add_page()  
        pdf.set_font("Courier", size = 8)
        for x in fid: 
            pdf.cell(80,3, txt = x, ln = 1, align = 'L')

        pdf.cell(80,3, "", ln = 3, align = 'L')
        pdf.cell(80,3, "Recall = what proportion of *ground truth* is correctly classified?",
                 ln = 1, align = 'L')
        pdf.cell(80,3, "       = diagonal element / row sum", ln = 1, align = 'L')
        pdf.cell(80,3, "", ln = 1, align = 'L')
        pdf.cell(80,3, "Precision = what proportion of *predicted Positives* is truly Positive?",
                 ln = 1, align = 'L')
        pdf.cell(80,3, "       = diagonal element / column sum", ln = 1, align = 'L')
        
        pdf.output (args.outPdf)
        fid.close()
    

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

