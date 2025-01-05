# Installing necessary software
```
apt-get install -y ffmpeg
/usr/bin/python3.7 -m venv --prompt spkrIdTrain ./spkrIdTrain
source ./spkrIdTrain/bin/activate
python3 -m pip install --upgrade pip setuptools wheel
pip install -r requirements.txt
```


# Example for creating a VIP model from scratch [2021-06-04 (Fri)]

```
## 1.) Start with a clean state (delete all models and profile pictures)
yes | rm profiles.zip meta_vosk.pkl svm_vosk.json


## 2.) Create a list of audio files for training
## Note: pick 20 random audios for training and the rest for testing
yes | ./create_dataset_lists.py \
                     --nTrainPerVIP 20 \
                     --nTestPerVIP -1 \
                     vips_with_profile/ \
                     enroll_list.csv \
                     test_list.csv

## 3.) Extract embeddings from each audio file (one embeding per each audio).
## Note: An embedding is a 128-float vector
## Note2: The extracted embedding are cached in /tmp/joblib/
## Note3: Needs ffmpeg
yes | ./create_speech_embeddings.py enroll_list.csv speech-embeddings.pkl

## 4.) Classify the embeddings into corresponding VIPs.
yes | ./create_speech_classifier.py speech-embeddings.pkl svm.pkl label

## 5.) Calibrate the classifier score
yes | ./calibrate_classifier_score.py svm.pkl label meta_vosk.pkl 

## 6.) Convert classifier to json (needed by the phone)
yes | ./skSvm2Java.py svm.pkl svm_vosk.json

## 7.) Create profiles.zip (profile pictures).
./create_profile_zip.bash vips_with_profile/

## 8.) Create an ATAK data package
./create_atak_data_package.bash ApolloSpeakerID-10VIP

## 9.) Upload the data package to the phone
# NOTE: It is also possible to push data package to TAK Server
adb push ApolloSpeakerID-10VIP.zip /sdcard/Download/
```

There is also a `run.bash` script that automates the above steps (1 to 8).

Import the data package zip file `ApolloSpeakerID-10VIP.zip` into
ATAK.  (hint: Start ATAK, click "Import Manager", ...)

Now, you can run speakerID backend, and invoke LoadModel() as:
```
spkrID.LoadModels (appContext, "ApolloSpeakerID-10VIP")
```

You can run the unit test to verify everything is working.
```
cd ../SpeakerIdBackend/
./build_and_run_test.bash data_package
```
