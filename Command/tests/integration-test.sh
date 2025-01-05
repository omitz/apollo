#!/bin/bash

#flask app health check
echo 'health check'
curl localhost:8080/health/

#sends job to face and face-resnet
echo 'sending inputs/face/crowd.jpg to face and faceresnet'
curl localhost:8080/jobs/facial_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/face/three_faces_ewan.png", "add_to_db":"False", "num_milvus_results":1}'
curl localhost:8080/jobs/facial_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path": "/inputs/face/three_faces_ewan.png"}'

#sends job to face detection/recognition (video)
curl localhost:8080/jobs/face_vid/ -H 'Content-Type:application/json' -H "authorization: Bearer yourauth" -X POST -d '{"path":"inputs/face/leo_ellen.mp4"}'

#sends job to landmark
echo 'sending inputs/landmark/worcester_000194.jpg to landmark'
curl localhost:8080/jobs/landmark/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/landmark/worcester_000194.jpg"}'

#sends job to ner
echo 'sending inputs/ner/test.txt to ner'
curl localhost:8080/jobs/named_entity_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/ner/test.txt"}'

#sends job to object detection
echo 'sending inputs/obj_det/puppies.jpeg to object-detection'
curl localhost:8080/jobs/object_detection/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/obj_det/puppies.jpeg"}'

#sends job to object detection (video)
echo 'sending inputs/obj_det_vid/holo_clip.mp4 to object-detection-vid'
curl localhost:8080/jobs/object_detection_vid/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/obj_det_vid/holo_clip.mp4"}'

#sends job to speaker-recognition
echo 'sending inputs/audio/bill_gates-TED.wav to speaker-recognition'
curl localhost:8080/jobs/speaker_recognition/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/audio/bill_gates-TED.wav"}'

#sends job to virus scanner then file queue then face + face-resnet
echo 'sending inputs/face/dalai.png to virus-scanner'
curl localhost:8080/jobs/virus_scanner/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/face/dalai.png"}'
curl localhost:8080/jobs/virus_scanner/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/ner/test_doc.doc"}'

#sends job to virus scanner then file type checker then speaker-recognition and speech-to-text
echo 'sending inputs/audio/1272-128104-0000.wav to virus-scanner'
curl localhost:8080/jobs/virus_scanner/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/audio/1272-128104-0000.wav"}'
#sends job to virus scanner then file hash, then file type checker then ner
echo 'sending inputs/ner/test.txt to virus-scanner'
curl localhost:8080/jobs/virus_scanner/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/ner/test.txt"}'
#sends job to virus scanner then file hash, then file type checker then video
echo 'sending inputs/ner/test.txt to virus-scanner'
curl localhost:8080/jobs/virus_scanner/ -H 'Content-Type:application/json' -X POST -d '{"path":"inputs/obj_det_vid/holo_clip.avi"}'

#query face
echo 'querying face endpoint'
curl localhost:8080/search/facial_recognition/

#sends job to bulk upload
echo 'sending inputs/ner to bulk upload'
curl localhost:8080/jobs/ -X POST -H 'Content-type:application/json' -d '{"path": "inputs/ner/"}'

#sends job to scene classification
echo 'sending inputs/scene-classification/6.jpg to scene-classification'
curl localhost:8080/jobs/scene_classification/ -H 'Content-Type:application/json' -X POST -d '{"path": "inputs/scene-classification/6.jpg"}'

#query scene classification
echo 'querying scene classification'
curl localhost:8080/search/scene_hierarchy?tag=indoor
curl localhost:8080/search/scene_class?tag=food+court

#send job to speech-to-text
echo 'sending inputs/audio/demo3.wav to speech-to-text'
curl localhost:8080/jobs/speech_to_text/ -H 'Content-Type:application/json' -X POST -d '{"path": "inputs/audio/demo3.wav"}'
echo 'sending inputs/audio/bill_gates-TED.mp3 to speech-to-text'
curl localhost:8080/jobs/speech_to_text/ -H 'Content-Type:application/json' -X POST -d '{"path": "inputs/audio/bill_gates-TED.mp3"}'

#query speech-to-text as full-text 
echo "search both epidemic OR indian (should get demo3 and bill_gates)"
curl localhost:8080/search/full_text?query=epidemic+%7C+indian

#test database record
echo "testing specific entry in the postgres database"
curl 'localhost:8080/search/check_database?model=SearchFullTextModel&path=s3://apollo-source-data/inputs/audio/bill_gates-TED.mp3'
