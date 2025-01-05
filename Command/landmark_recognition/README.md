# Landmark image retrieval using deep local and global image features (DELF)
This package could alternatively be called "query landmarks by image" or "content-based landmark image retrieval".

## Terminology

inliers: The number of data points from one image that are inliers (as opposed to outliers) when comparing to the data points from another image. The number of inliers is ultimately the indicator for how similar the two images are. The higher the number, the more likely the images are of the same location.

## What it does

Given a image, this analytic will try to find other images of that specific building/landmark/area.

Sending a job to the landmark_route ultimately calls `match_images.py`, where most of this analytic's work is done. First, that calls `extract_and_add_feats` (in `extract_features.py`) to extract the DELF features from the input image and add them to the `landmarks` postgres table. In the `landmarks` postgres table, we store the S3 image path, DELF locations (as in x,y coordinates in the image), and DELF descriptors (feature vectors that we use for image matching). We then compare the query image against every image in the postgres table.

The landmark matching this analytic performs is not limited to buildings. If we have a picture of Person A standing in <some courtyard with trees in the background> in our database and query a picture of Person B standing in the same courtyard, the expected result would be the picture of Person A. 

## What it doesn't do

The model in this analytic is not a traditional classifier. It matches instances of buildings (or scenes, or landmarks), not classes. For example, if our database contains images of some modern buildings in the UK + neoclassical buildings in the UK, and we query MoMA (a modern building in the US), we shouldn't necessarily expect our resulting txt file to list all the modern buildings, then all the neoclassical buildings.

## Environment Setup

```bash
# From apollo/Command
docker-compose build flask-apollo-processor landmark 
docker-compose up -d flask-apollo-processor landmark

```
    
## Optimize query execution time

In the current setup, Dask takes advantage of all of the CPU cores available on the Kubernetes node the `landmark-search` pod is running on. Our current nodes are `r5.xlarge` instances, which have 4 vCPU. 
Given additional cores, there are a couple options for speeding things up:
 1) Requires no code change (aside from infrastructure code to define the instance type). Dask will create one worker per CPU (as determined by os.cpu_count()); 
 2) Dask’s `KubeCluster` can be used to launch short-lived deployments of workers by launching additional pods from the pod in which the KubeCluster is created (ie the landmark-search pod could create a KubeCluster which would scale up pods when it gets a job and scale them back down when it’s done).

## Resources

This analytic is based on the DELF repo: https://github.com/tensorflow/models/tree/master/research/delf