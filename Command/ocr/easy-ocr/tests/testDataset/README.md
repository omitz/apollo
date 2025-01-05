Genreated by TextRecognitionDataGenerator:

https://github.com/Belval/TextRecognitionDataGenerator

for lang in en ar; do
    mkdir -p out/$lang
    python3 run.py -w 5 -f 64 -b 0 -c 10 -d 1 \
            -ft Arial.ttf -l $lang --output_dir out/$lang
done
