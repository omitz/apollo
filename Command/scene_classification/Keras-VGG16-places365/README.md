# Keras | VGG16 Places365 - VGG16 CNN models pre-trained on Places365-Standard for scene classification 

Note, the input image is assumed to be a square image.  If not, it is
first cropped to retain the largest square subimage.  This may not be
necessary..

If needed, the official test data can be downloaded at:
```bash
 wget -r http://data.csail.mit.edu/places/places365/test_256.tar
```

### Runing Unittest
```bash
# from outside docker container:
python3 -m unittest

# from inside docker container:
export JENKINS=True
python3 -m unittest
```

## Apollo Integration:
Assumptions:
    - Docker-Compose is installed.
    - The Apollo Command directory located at ../../..
    - There is a .env file in the Apollo Command directory.
### Runing Unittest
```bash
./tests/integration_test.sh
```
### Live debugging
```
# cd to Command dir.
docker-compose run -v $(pwd)/scene_classification/Keras-VGG16-places365/:/code --rm scene-places365 bash
```

### Ingest an image
```bash
curl -X POST -H 'Content-Type: application/json' -d '{"path":"/inputs/load_test/test_256/Places365_test_00000006.jpg"}' https://api.apollo-cttso.com/jobs/scene_classification/

curl -X POST -H 'Content-Type: application/json' -d '{"path":"/inputs/load_test/test_256/Places365_test_00000006.jpg"}' https://api.apollo-cttso.com/jobs/virus_scanner/
```

## Process official test dataset  (900 per category: 900*365=328500 images)
The test dataset has been uploaded to S3 bucket at:

`apollo-source-data/inputs/load_test/test_256/`

All jpg file thre has already been processed on a local GPU machine
and the result saved to `result.txt`.

### Run test dataset on GPU machine:
```bash
docker build -t apollo/vgg16-place365 .
docker run --rm -it -v$(pwd):/app apollo/vgg16-place365 bash
rm test_256/result.txt
./runTestset.py test_256/ test_256/result.txt
```

### Ingest test dataset result (result.txt) into database directly:
```bash
cd apollo/Command

# remove database
docker stop command_postgres_1       2> /dev/null  || true
docker rm command_postgres_1         2> /dev/null  || true
docker stop command_scene-places365_1 2> /dev/null  || true
docker rm command_scene-places365_1   2> /dev/null  || true

# build if needed
docker-compose build scene-places365 flask-apollo-processor

# bring up the services
docker-compose up -d scene-places365 flask-apollo-processor
sleep 10

# tap into scene-places365 and ingest result to database directly
PID=`docker ps | grep scene | awk {'print $1;'}`
echo $PID
docker exec -it $PID bash -c "./testDatasetIngest_v3.py >/dev/null"

# finally, test the query.  Note: result is sorted by top ranking
curl localhost:8080/search/scene_class?tag=food+court # "+" is a space

```
