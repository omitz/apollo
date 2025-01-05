Genreated by TextRecognitionDataGenerator:

https://github.com/Belval/TextRecognitionDataGenerator

for lang in en fr ru ar es ; do
    mkdir -p out/$lang
    python3 run.py -w 5 -c 10  -ft Arial.ttf -l $lang --output_dir out/$lang
done
