#!/bin/bash

set -e

#####################
## Exp1:
#####################
# do k-fold cross validation  (each class has 30 examples)
yes | ./create_cross_validatation_data.py vips_with_profile 3  # get 20 examples for training
yes | ./show_cross_validation_result.py 3 50

# do rejection validation
./run.bash
yes | ./create_unknown_evaluation.py unknown_dataset svm.pkl unknown_scores.pkl
yes | ./show_unknown_evaluation.py unknown_scores.pkl 50

# show threshold studies
yes | ./show_threshold_study.py 3 50 unknown_scores.pkl speakerid_threshold_study
evince speakerid_threshold_study.pdf


#####################
## Exp2: Small training
#####################
# do k-fold cross validation
yes | ./create_cross_validatation_data.py vips_with_profile 2 20
yes | ./show_cross_validation_result.py 2 50

# do rejection validation
./run.bash 10
yes | ./create_unknown_evaluation.py unknown_dataset svm.pkl unknown_scores.pkl
yes | ./show_unknown_evaluation.py unknown_scores.pkl 50

# show threshold studies
yes | ./show_threshold_study.py 2 50 unknown_scores.pkl speakerid_threshold_study_small
evince speakerid_threshold_study_small.pdf
