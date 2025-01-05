#! /usr/bin/env python3
#
# This program takes in a directory of additional vip and append it to
# the the enrollmenet and test list.
#
#
# VIP file structure Assumption:
#  - vip/
#    - FirstName_LastName/
#      - profile.jpg
#      - file1.m4a
#      - ...
#
# Output file example:
#   cfg_apollo/enroll_list.csv:
#       filename,speaker
#       vip/FirstName_LastName/file1.m4a,FirstName_LastName
#       ...
#
#   cfg_apollo/test_list.csv
#       filename,speaker
#       vip/FirstName_LastName/filex.m4a,FirstName_LastName
#       ...
#
# TC 2020-11-10 (Tue) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import io
import numpy as np
import random

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------


def GetFiles (dirName, endWith):
    # Returns a list of file names under a directory
    imageFiles = [os.path.join (dirName, f) for f in os.listdir (dirName) 
                  if f.endswith (endWith)]
    return list (sorted (imageFiles))   # listdir has arbitrary sorting order

def GetFilesExceptProfilePic (dirName):
    # Returns a list of file names under a directory
    imageFiles = [os.path.join (dirName, f) for f in os.listdir (dirName) 
                  if not f.endswith ("profile.jpg")]
    return list (sorted (imageFiles))   # listdir has arbitrary sorting order

def GetDirs (dirName):
    # Returns a list of subdirectory under a directory
    imageDirs = [os.path.join (dirName, f) for f in os.listdir (dirName) 
                 if os.path.isdir (os.path.join (dirName, f))]
    return list (sorted (imageDirs))   # listdir has arbitrary sorting order


def SplitDataSet (vipDir, singleVip_flg,
                  nSamplesPerVip_train=30,
                  nSamplesPerVip_test=-1):
    """
    """
    
    # get list of directories under vip
    if singleVip_flg:
        vips = [vipDir]
    else:
        vips = GetDirs (vipDir)
    
    enrollList = []
    evalList = []
    
    for vip in vips:

        # get all wav files belonging to the vip
        # vipWavs = GetFiles (vip, "m4a")
        vipWavs = GetFilesExceptProfilePic (vip)

        # if we don't have nSamplesPerVip_train, then, we set it to
        # nSamplesPerVip_train - 1 and let nSamplesPerVip_test = 1
        nWavs = len (vipWavs)
        nSPV_train = nSamplesPerVip_train
        nSPV_test = nSamplesPerVip_test
        if nWavs < nSamplesPerVip_train:
            nSPV_train = nWavs - 1
            nSPV_test = 1

        # Adjsut nSamplesPerVip_test if we asked too many
        if (nSPV_train + nSPV_test) > nWavs:
            nSPV_test = nWavs - nSPV_train
            
        # split into two subsets
        np.random.shuffle (vipWavs)
        enrollSamples = vipWavs [0:nSPV_train]
            
        if nSamplesPerVip_test == -1:
            evalSamples = vipWavs [nSPV_train:]
        else:
            evalSamples = vipWavs [nSPV_train:nSPV_train + nSPV_test]
        enrollList.append (enrollSamples)
        evalList.append (evalSamples)

    # only want eval/test dataset
    if nSamplesPerVip_train == 0:
        assert (len (enrollList[0]) == 0)
        return evalList
    return (enrollList, evalList)


def AppendEnrollOrEval (enrollFile, enrollList, overwrite_flg):
    
    # Write csv header
    if overwrite_flg or (not os.path.isfile (enrollFile)):
        print (f"\tCreating {enrollFile}")
        with open(enrollFile, "wb") as myfile:
            myfile.write(b"filename,speaker\n")
    else:
        print (f"\tAppending {enrollFile}")

    # Append to file
    with open(enrollFile, "a") as myfile:
        for vipWavFiles in enrollList:
            for wavFile in vipWavFiles: # vip/Aishwarya_Rai_Bachchan/00166.wav
                vipName = wavFile.split('/')[-2] # Aishwarya_Rai_Bachchan
                myfile.write(f"{wavFile},{vipName}\n")
    pass


def getEntireDataset (vipTopDir, maxSamples=None):
    wavList = []
    vips = GetDirs (vipTopDir)
    for vip in vips:
        vipWavs = GetFiles (vip, "m4a")
        if (maxSamples != None) and (len (vipWavs) > maxSamples):
            vipWavs = random.sample (vipWavs, maxSamples) 
        wavList.append (vipWavs)

    myFile = io.StringIO()
    myFile.write ("filename,speaker\n")
    
    for vipWavFiles in wavList:
        for wavFile in vipWavFiles: # vip/Aishwarya_Rai_Bachchan/00166.wav
            vipName = wavFile.split('/')[-2] # Aishwarya_Rai_Bachchan
            myFile.write(f"{wavFile},{vipName}\n")
            
    # print ("myfile.getvalue () = ", myFile.getvalue ())
    myFile.seek(0)

    return myFile



def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("vipTopDir", help="the vip top directory")
    parser.add_argument ("enrollFile", nargs='?', help="the enrollment file, can skip")
    parser.add_argument ("evalFile", help="the eval file")
    parser.add_argument ("-s", "--singleVip",
                         help="The vipTopDir is treated as a vip itself ", 
                         action="store_true")
    parser.add_argument ("-a", "--append",
                         help="Append the enrollment and eval file (instead of overwrite) ", 
                         action="store_true")
    parser.add_argument ("--nTrainPerVIP",
                         help="Number of random training sample per VIP (-1 = no limit)", 
                         default="30")
    parser.add_argument ("--nTestPerVIP",
                         help="Number of random test sample per VIP (-1 = no limit)", 
                         default="-1")

    # Specify Example:
    parser.epilog=f'''Example:
        {sys.argv[0]} vips/ enroll_list.csv test_list.csv
        {sys.argv[0]} -a vips/ enroll_list.csv test_list.csv
        {sys.argv[0]} -a -s vips/John_Smith enroll_list.csv test_list.csv 
        {sys.argv[0]} vips/ new_test_list.csv
        {sys.argv[0]} vips/ enroll_list.csv test_list.csv --nTestPerVIP 1
        {sys.argv[0]} vips/ all_test_list.csv --nTestPerVIP -1
        {sys.argv[0]} vips/ enroll_list.csv test_list.csv --nTrainPerVIP 20
        '''

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
    if not args.enrollFile:
        args.nTrainPerVIP = 0
        
    # print ("overwrite = ", args.overwrite)
    print ("append = ", args.append)
    print ("single vip = ", args.singleVip)
    print ("vipTopDir = ", args.vipTopDir)
    print ("enrollFile = ", args.enrollFile)
    print ("evalFile = ", args.evalFile)
    print ("nTrainPerVIP = ", args.nTrainPerVIP)
    print ("nTestPerVIP = ", args.nTestPerVIP)
    input ("press enter to continue")

    nTrainPerVIP = int (args.nTrainPerVIP)
    nTestPerVIP = int (args.nTestPerVIP)
    
    #---------------------------
    # run the program :
    #---------------------------
    if args.enrollFile:         
        (enrollList, evalList)= SplitDataSet (args.vipTopDir, args.singleVip,
                                              nTrainPerVIP, nTestPerVIP)
        AppendEnrollOrEval (args.enrollFile, enrollList, not args.append)
    else:
        # only want eval/test dataset
        evalList = SplitDataSet (args.vipTopDir, args.singleVip, 0, nTestPerVIP)

    AppendEnrollOrEval (args.evalFile, evalList, not args.append)
    

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

