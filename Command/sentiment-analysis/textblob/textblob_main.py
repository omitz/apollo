#!/usr/bin/env python3
#
# ref: https://pythonprogramming.net/sentiment-analysis-python-textblob-vader/

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import argparse, sys
import textwrap
import os, sys                                 # exit, argv
from textblob import TextBlob

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------
def run (inTextFile,
         outTextFile):
    """Run textblob

    Args:
      inTextFile: 
        Location of the input text file.
      outTextFile:
        Location of the output text file.
    """
    with open (inTextFile, "r") as fid:
        text = fid.read()

    analysis = TextBlob (text)
    # for sentence in analysis.sentences:
    #     print(sentence.sentiment)
    
    polarity = analysis.sentiment.polarity
    if polarity < 0:
        sentiment = "negative"
    if polarity == 0:
        sentiment = "neutral"
    if polarity > 0:
        sentiment = "positive"

    outText = "sentiment = %s\npolarity = %f\n" % (sentiment, analysis.sentiment.polarity)
    # print (outText, flush=True)
    
    with open (outTextFile, 'w') as fid:
        fid.write (outText)
    
    return (sentiment, polarity)


def parseCommandLine () -> argparse.Namespace:
    """Parse commandline argurments.

    Returns:
      An argparse.Namespace object whose member variables correspond
      to commandline agruments. For example the "--debug" commandline
      option becomes member variable .debug.
    """
    
    description="""
    A sentiment analysis program based on textblob.
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("inTextFile", help="input text file")
    parser.add_argument ("outTxtFile", help="output text file")

    # Specify Example:
    parser.epilog='''Example:
        %s in.txt out.txt
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
    args = parseCommandLine()

    #---------------------------
    # run the program :
    #---------------------------
    run (args.inTextFile, args.outTxtFile)

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)
