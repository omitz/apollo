## Face Detection and Recognition

#### Description

Detect faces
Run classification (i.e. predict who the person is)
Send image to Command
Download results from Command face recognition

### Buttons

#### SAVE RESULT

Clicking this button will save the current image, the image with the drawn bounding box, and a json file of the detection to the 'facerecognizer' directory

#### SYNC RESULTS

Clicking this button will (for images for which there is no Command result yet) 
    1) send the image to Command for processing and 
    2) attempt to download the Command result.

### Retraining the classification model

The app installs with a pre-trained classification model. Follow the instructions below to retrain it.

Prerequisite: `adb` - https://developer.android.com/studio/command-line/adb. Connect the phone via usb. (USB Debugging must be enabled. Also, the phone will prompt you to allow access to "Allow access to phone data". Click "Allow".) Confirm that `adb` is working and recognizes your device by running `adb devices`. You should see your device listed. e.g. 
    '''
    List of devices attached
    3658324553443498	device
    '''
    
* Create a VIPs dataset named `vips`
    The vips directory should have the following structure: each subdirectory is the label for the person, and each file in that subdirectory is a picture containing that person (and no other faces):
    
           ├── vips
           │   ├── John_Smith
           │   │   ├── 1.jpg
           │   │   ├── 2.jpg
           │   │   ├── 3.jpg
           │   ├── Jane_Doe
           │   │   └── 1.jpg
           |   |   (etc...)
           
    Each image should have only one face in it. It's recommended to have a dataset with at least 10 pictures for each person. For reference, the original mobile version of the VIP dataset can be found at `apollo/Command/vip/vips`.
* Run the retraining script, passing the path to your `vips` dataset as an argument
    ```
    # From apollo/Edge/atak/apolloedge/app/src/main/java/com/atakmap/android/apolloedge/pp/facerecognizer
    sh retrain_setup.sh <path to new vips dataset>
    # e.g.
    sh retrain_setup.sh /tmp/vips/
    ```
  This will delete the contents of the current `data` and `label` files, then reinstall the plugin.
* After the new install finishes, open the face recognition module. This will automatically trigger retraining of the face recognition model.
* If you want future installs of the app to have the newly-trained model, run `sh retrain_post.sh`. This will copy the updated `data`, `label`, and `model` files from the phone to your local `apollo` repository. Commit and push those files' changes for future installs to use the new model.

### Asset file descriptions

`data`: Each row represents one face in the training data. The first element is the integer label for that person. The following values make up the embedding array for that face.
`label`: One string label for each class known to the classification model.
`model`: The Support Vector Machine (Support Vector Classifier)
  
### Resources

* Original repo: https://github.com/pillarpond/face-recognizer-android