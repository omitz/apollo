#!/bin/bash

#flask app health check
echo 'health check'
curl https://api.apollo-cttso.com/health/

#sends job to face and face-resnet
echo 'sending inputs/face/crowd.jpg to face and faceresnet'
curl https://api.apollo-cttso.com/jobs/facial_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/face/three_faces_ewan.png", "add_to_db":"False", "num_milvus_results":1}'
curl https://api.apollo-cttso.com/jobs/facial_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path": "/inputs/face/ewan_new.png"}'

#sends job to landmark
echo 'sending inputs/landmark/worcester_000194.jpg to landmark'
curl https://api.apollo-cttso.com/jobs/landmark/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/landmark/worcester_000194.jpg"}'

#sends job to speaker-recognition
echo 'sending inputs/audio/bill_gates-TED.wav to speaker-recognition'
curl https://api.apollo-cttso.com/jobs/speaker_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/audio/bill_gates-TED.wav"}'

#sends job to ner
echo 'sending inputs/ner/test.txt to ner'
curl https://api.apollo-cttso.com/jobs/named_entity_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/ner/test.txt"}'

#sends job to object detection
echo 'sending inputs/obj_det/puppies.jpeg to object-detection'
curl https://api.apollo-cttso.com/jobs/object_detection/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/obj_det/puppies.jpeg"}'

#sends job to virus scanner then file queue then face + face-resnet
echo 'sending inputs/face/dalai.png to virus-scanner'
curl https://api.apollo-cttso.com/jobs/virus_scanner/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/face/dalai.png"}'

#sends job to virus scanner then file type checker then speaker-recognition and speech-to-text
echo 'sending inputs/audio/1272-128104-0000.wav to virus-scanner'
curl https://api.apollo-cttso.com/jobs/virus_scanner/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/audio/1272-128104-0000.wav"}'

#query face
echo 'querying face endpoint'
curl https://api.apollo-cttso.com/search/facial_recognition/

#sends job to bulk upload
echo 'sending inputs/ner to bulk upload'
curl https://api.apollo-cttso.com/jobs/ -X POST -H 'Content-type:application/json' -d '{"path": "inputs/ner/"}'

curl https://api.apollo-cttso.com/jobs/ -X POST -H 'Content-type:application/json' -d '{"path": "inputs/face/", "ignore_hash": "True"}'
