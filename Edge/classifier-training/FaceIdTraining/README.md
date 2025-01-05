# Installing necessary software
```
/usr/bin/python3.7 -m venv  --prompt faceIdTrain ./faceIdTrain
source ./faceIdTrain/bin/activate
python3 -m pip install --upgrade pip setuptools wheel
python3 -m pip install -r requirements.txt
```


# Example for creating a VIP model from scratch [2021-06-04 (Fri)]

```
## 1.) Start with a clean state (delete all models and profile pictures)
yes | rm label model profiles.zip

## 2.) Create a pickle file to store detected faces (their locations and raw pixels)
## Note: A detected face is 160x160 pixels
## Note2: The detected faces are cached in /tmp/joblib/
yes | ./create_face_dataset.py ./vips_with_profile/ faces-dataset.pkl

## 3.) Extract embedings from the detected faces (one embeding per each face).
## Note: An embedding is a 512-float vector.
yes | ./create_face_embeddings_v2.py faces-dataset.pkl faces-embeddings.pkl

## 4.) Classify the embeddings into corresponding VIPs.
yes | ./create_face_classifier.py faces-embeddings.pkl svm.pkl label

## 5.) Convert the classifer to libsvm format (needed by the phone)
yes | ./skSvm2LibSvm.py -s 0 svm.pkl model 

## 6.) Create profiles.zip (profile pictures).
./create_profile_zip.bash vips_with_profile/

## 7.) Create an ATAK data package
./create_atak_data_package.bash faceID_celebrity10

## 8.) Push the classifer and profile pictures to the phone.
# NOTE: It is also possible to push data package to TAK Server
adb push ApolloFaceID-10VIP.zip /sdcard/Download/
```

Note that we don't use `data` file anymore.  Just `label`, `model`,
and the new addition `profiles.zip`.  There is also a `run.bash`
script that automates the above steps (1 to 7).

Now, you can run faceID backend, and change model loading directory to:
```
faceID.LoadModels (appContext, "ApolloFaceID-10VIP");
```

You can run the unit test to verify everything is working.
```
cd ../FaceIdBackend/
./build_and_run_test.bash data_package
```


# TRAINING FACEID CLASSIFIER  (More detail)

## Create python environment
```
/usr/bin/python3.7 -m venv  --prompt faceIdTrain venv
source ./venv/bin/activate
python3 -m pip install --upgrade pip setuptools wheel
python3 -m pip install -r requirements.txt
```

## Create dataset (only needed to be done once when data changes)
### Create a VIPs directory

The `vips` directory should have the following structure: each
subdirectory is the label for the person, and each file in that
subdirectory is a picture containing that person (and no other faces).
A special profile picture named "profile.jpg" is used for the front-end
GUI to display an representative image.
    
           ├── vips
           │   ├── John_Smith
           │   │   ├── profile.jpg
           │   │   ├── 1.jpg
           │   │   ├── 2.jpg
           │   │   ├── 3.jpg
           │   ├── Jane_Doe
           │   │   ├── profile.jpg
           │   │   └── 1.jpg
           |   |   (etc...)
### Create a training dataset 
```
./create_face_dataset.py ../../../Command/vip/vips/ faces-dataset.pkl
```

Note that if you run again, the second time runs a lot faster because
the outputs are cached in `/tmp/joblib/`.

## Create Embeddings (only needed to be done once when data changes)
```
./create_face_embeddings_v2.py faces-dataset.pkl faces-embeddings.pkl
```

## Create Classifier for dekstop
```
./create_face_classifier.py faces-embeddings.pkl svm.pkl label
```

## Convert classifier libsvm format
```
./skSvm2LibSvm.py -s 0 svm.pkl model   # for phone
./skSvm2LibSvm.py -s 1 svm.pkl model.desktop   # for desktop
```

## Test libsvm classifer
```
./predict_face_v2.py jim_gaffigan.jpg model.desktop label
```
The typical output looks like:
```
Face is  Jim_Gaffigan
Prob is  0.7213582523270458
```

## Create a profile package (profiles.zip) of all VIPs


