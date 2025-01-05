#!/usr/bin/env python
#
# The program generates enroll and eval data from the Apollo vip dataset.
#
# VIP file structure Assumption:
#  - vip/
#    - speaker/
#      - file1.wav
#      - ...
#
# Output file example:
#   cfg_apollo/enroll_list.csv:
#       filename,speaker
#       data/wav/enroll/19-227-0000.wav,19
#       data/wav/enroll/26-495-0000.wav,26
#       data/wav/enroll/27-123349-0000.wav,27
#
#   cfg_apollo/test_list.csv
#       filename,speaker
#       data/wav/test/19-198-0001.wav,19
#       data/wav/test/27-123349-0001.wav,27
#
# TC 2020-02-09 (Sun) -- Now that tensorflow lite need input dimension
# hard-coded, we removed all sound files are < 4 seconds, (or 128200 byes
# in wave file) from vip/.  We may also need to check in the front-end
# to see if this is true.
#
# TC 2019-12-31 (Tue) --

import random
import os

if __name__ == "__main__":

    #
    # For each speaker in the enrollment data, we randomly select 20
    # wav files.
    #
    spkrDirs = [os.path.join ("vip/",d) for d in os.listdir ("vip/")]
    spkrDirs = [spkrDir for spkrDir in spkrDirs if os.path.isdir (spkrDir)]
    enrollDict = {}
    for spkrDir in spkrDirs:
        audFiles = [os.path.join (spkrDir,f) for f in os.listdir (spkrDir)]
        audFiles = [audFile for audFile in audFiles if audFile.endswith("wav")]
        audSample = random.sample (audFiles, 20)
        speaker = spkrDir.split('/')[-1]
        enrollDict [speaker] = audSample

    ## write to cfg/enroll_list.csv
    with open ("cfg_apollo/enroll_list.csv", "w") as csvFile:
        csvFile.write ("filename,speaker\n")
        for speaker in enrollDict.keys():
            for audFile in enrollDict[speaker]:
                csvFile.writelines ("%s,%s\n" %(audFile,speaker))
    
    
    #
    # Write a single unknown speaker
    #
    with open ("cfg_apollo/test_list.csv", "w") as csvFile:
        # csvFile.write ("vip/Aishwarya_Rai_Bachchan/00387.wav,"
        #                "Aishwarya_Rai_Bachchan\n")
        csvFile.write ("filename,speaker\n")
        csvFile.write ("unknown_eval_set/Aishwarya_Rai_Bachchan_00387.wav,"
                       "unknown\n")
        csvFile.write ("unknown_eval_set/Ewan_McGregor_00084.wav," "unknown\n")
        csvFile.write ("unknown_eval_set/Frankie_Muniz_00044.wav," "unknown\n")
        csvFile.write ("unknown_eval_set/Haley_Joel_Osment_00057.wav,"
                       "unknown\n")
        csvFile.write ("unknown_eval_set/Jim_Gaffigan_00208.wav," "unknown\n")
        csvFile.write ("unknown_eval_set/Katie_Holmes_00141.wav," "unknown\n")
        csvFile.write ("unknown_eval_set/Leonardo_DiCaprio_00451.wav,"
                       "unknown\n")
        csvFile.write ("unknown_eval_set/Liam_Neeson_00314.wav," "unknown\n")
        csvFile.write ("unknown_eval_set/Liza_Minnelli_00034.wav," "unknown\n")
        csvFile.write ("unknown_eval_set/Mohammad_Ali_00267.wav," "unknown\n")


        
