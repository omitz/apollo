#! /usr/bin/env python3
#
#
# TC 2010-08-01 (Sun)


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import json
import csv
from ... imoprt textblob_main
from pathlib import Path
import random
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
    parser.add_argument ("jsonFile", help="sentiment json file")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")

    # Specify Example:
    parser.epilog='''Example:
        %s tests/Appliances_5.json
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


def EvalPerformance (all_positives, all_negatives, all_neutral):
    # 2.) Create a balanced dataset
    # random.seed ()
    positives = random.sample (all_positives, 20)
    negatives = random.sample (all_negatives, 20)
    neutrals = random.sample (all_neutral, 20)
    
    # 3.) Test and eval accuracy
    #  #correct / #total data  (sum of confusing matrix diagonal)
    correct = {"positive":0, "negative":0, "neutral":0}
    failures = {"positive":[], "negative":[], "neutral":[]}
    totalCorrect = 0
    for (truth, score, reviewText) in positives + negatives + neutrals:
        ram_target_Path = Path ("/dev/shm/sentiment_in_%d.txt" % os.getpid())
        ram_output_Path = Path ("/dev/shm/sentiment_out_%d.txt" % os.getpid())
        with ram_target_Path.open("a") as f:
            f.write(reviewText)

        (sentiment, polarity) = textblob_main.run (ram_target_Path,
                                                   ram_output_Path)
        if (sentiment == truth):
            correct[truth] += 1
            totalCorrect += 1
        else:
            failures[truth].append ((sentiment, polarity, reviewText))
        os.remove (ram_target_Path)
        os.remove (ram_output_Path)

    totalData = len(positives) + len(negatives) + len(neutrals)
    overall_accuracy = (totalCorrect / totalData * 100, totalCorrect, totalData)
    positive_accuracy = (correct['positive'] / len(positives) * 100,
                         correct['positive'], len(positives))
    negative_accuracy = (correct['negative'] / len(negatives) * 100,
                         correct['negative'], len(negatives))
    neutral_accuracy = (correct['neutral'] / len(neutrals) * 100,
                        correct['neutral'], len(neutrals))
    print (f"overall accuracy = {overall_accuracy}")
    print (f"positive accuracy = {positive_accuracy}")
    print (f"ngative accuracy = {negative_accuracy}")
    print (f"neutral accuracy = {neutral_accuracy}")

    accuracies = {"overall":overall_accuracy, "positive":positive_accuracy,
                  "negative":negative_accuracy, "neutral":neutral_accuracy}
    return (failures, accuracies)



def AmazonRevewExp (jsonFile):
    """
         - https://nijianmo.github.io/amazon/index.html#subsets
            - (Appliances_5.json.gz)
            - Amazon Review Data (2018) "this dataset includes Amazon
              reviews (ratings, text, helpfulness votes), of appliances"

    fiedls: "overall", "reviewText"
    """
    # 1.) load reviews
    jsons = open (jsonFile, 'rb').readlines()
    jsons = [item.strip() for item in jsons]
    all_positives = []
    all_negatives = []
    all_neutral = []
    for json_str in jsons:
        res = json.loads (json_str)
        reviewText = res['reviewText']
        score = int (res['overall'])
        # print ("score = ", score)

        if (score > 3):
            truth = "positive"
            all_positives.append ((truth, score, reviewText))

        elif (score < 3):
            truth = "negative"
            all_negatives.append ((truth, score, reviewText))

        else:
            truth = "neutral"
            assert (score == 3)
            all_neutral.append ((truth, score, reviewText))

    # 2.) Do performance evaluation
    (failures, accuracies) = EvalPerformance (all_positives,
                                              all_negatives, all_neutral)
    return (failures, accuracies)



def TweetsReviewExp (csvFile):
    """
         - https://www.kaggle.com/crowdflower/twitter-airline-sentiment
            - "tweets since Feb 2015 about each of the major US
              airline. Each tweet is classified either positive, negative
              or neutral."

    fiedls: "airline_sentiment", "text"
    """

    # 1.) load reviews
    all_positives = []
    all_negatives = []
    all_neutral = []
    cf = csv.DictReader (open (csvFile,'r'))
    for row in cf:
        # print ("row = ", row["airline_sentiment"])
        # print ("row = ", row["text"])

        reviewText = row["text"]
        truth = row["airline_sentiment"]
        score = row["airline_sentiment_confidence"]
        # print ("score = ", score)

        if truth == "positive":
            all_positives.append ((truth, score, reviewText))

        elif truth == "negative":
            all_negatives.append ((truth, score, reviewText))

        else:
            assert (truth == "neutral")
            all_neutral.append ((truth, score, reviewText))

    # 2.) Do performance evaluation
    return EvalPerformance (all_positives, all_negatives, all_neutral)


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
    # print ("args.jsonFile = ", args.jsonFile)


    #---------------------------
    # run the program :
    #---------------------------
    (amz_failures, amz_accuracies) = AmazonRevewExp ("tests/Appliances_5.json")
    (tweet_failures, tweet_accuracies) = TweetsReviewExp ("tests/Tweets.csv")

    # save for plotting/summarizing
    print ("Saving to performance.pkl")
    pickle.dump ([amz_failures, amz_accuracies,
                  tweet_failures, tweet_accuracies],
                 open ("performance.pkl", 'wb'))
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

