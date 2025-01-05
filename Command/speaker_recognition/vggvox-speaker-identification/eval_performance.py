#!/usr/bin/env python
#
# Some performance evaluation metrics for multi-class classifier.
# 
# TC 2020-03-11 (Wed) 
import os
import numpy as np
import pandas as pd
from sklearn import metrics

def printHeader (msg):
    print ("-" * 50)
    blanks = int ((50 - len (msg)) / 2) - 1
    print ("|" + " " * blanks + msg + " " * blanks + "|")
    print ("-" * 50)


if __name__ == '__main__':
    #
    # 1.) Load result csv file as a panda dataframe
    #
    df = pd.read_csv ("all_results.csv")
    # df = pd.read_csv ("all_results_3nn.csv")


    #
    # 2.) get ground-truth and prediction
    #
    y_truth = df.test_speaker
    y_pred = df.result

    #
    # 3.) classifier summary
    #
    # Print the confusion matrix
    printHeader ("Confusion Matrix")
    print (metrics.confusion_matrix (y_truth, y_pred))
    print ()
    
    # Print the precision and recall, among other metrics
    printHeader ("Multi-class Classifier Performance")
    print (metrics.classification_report (y_truth, y_pred, digits=3))
    print ()

    #
    # 4.) print out degScore distribution
    #
    printHeader ("Correct Score Distribution")
    print (df.degScore[df.correct == 1.0].describe())
    print ()
    printHeader ("Incorrect Score Distribution")
    print (df.degScore[df.correct == 0.0].describe())
    print ()


    #
    # 5.) what is the probablity of a correct detection given < score?
    #
    printHeader ("Probablity of a correct detection given < score")

    scoresList = range(10,91,10)
    probList = []
    for score in scoresList:
        degScores = df.correct[df.degScore < score]
        probList += [(degScores.sum() / degScores.count())]

    close ('all')
    figure()
    plot (scoresList, probList) 
    title ("True Positive Rate vs Minimum Score")
    xlabel ("Score")
    ylabel ("Probability of Correct Classification")
    show()

    df = pd.DataFrame()
    df["Max Score"] = scoresList
    df["P(Correct)"] = probList
    with pd.option_context('display.colheader_justify','center'): 
        print (df)
