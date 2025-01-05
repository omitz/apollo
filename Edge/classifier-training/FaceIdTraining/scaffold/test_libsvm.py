#! /usr/bin/env python3
#
#
# TC 2021-05-05 (Wed) 
from libsvm.svmutil import *


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
from sklearn.datasets import load_iris


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------



#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    # # Load some data
    # iris = load_iris()
    # X, y = iris.data, iris.target

    # Load celebrity data
    y, X = svm_read_problem('faceID.data', return_scipy=True) # faceID embedding is 512-D?
    
    # train a SVM
    # m = svm_load_model('faceID.model')
    m = svm_train(y, X, '-t 0 -b 1') # linear and probability
    m.is_probability_model()

    # save the svm
    svm_save_model('libsvm.model', m)

    # load the svm back
    m = svm_load_model('libsvm.model')

    
    # predict
    p_label, p_acc, p_val = svm_predict(y[:10], X[:10], m, '-b 1') # probability
