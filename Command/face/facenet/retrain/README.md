

## Retraining the classification model

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
           
   Each image should have only one face in it. It's recommended to have a dataset with at least 10 pictures for each person. The more pictures of each person, the better.
   Save the `vips` directory to `/tmp/`.
   
   
### Prerequisites

A Python 3.6 environment with all of the dependencies listed in `../../facenet_rabbit_consumer/requirements.txt`. (Using `venv` with `pip 9.0.1` is recommended.)

You may need to specify the grpcio packages:

    grpcio==1.26.0
    grpcio-tools==1.35.0

### Train the classification model

    # From apollo/Command/face/facenet/retrain
    sh retrain.sh
    
The new classification model will be saved to `/tmp/clf.pkl`. Replace the current `clf.pkl` model on AWS S3 at `apollo-source-data/local/facenet/model/clf.pkl` with the new one from `/tmp/`.
The label mapping (`Command/face/facenet/label_mapping.pkl`) will be updated as well. Commit and push the changes to `label_mapping.pkl` so that future face recognition service instances will have the correct integer:string label mapping.