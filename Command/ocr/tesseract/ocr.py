import sys
import logging
import argparse
import pytesseract

from typing import List
from PIL import Image
import numpy as np

logging.basicConfig(level=logging.INFO)


def detect_script_and_orientation(img: Image) -> dict:
    try:
        osd_dict = pytesseract.image_to_osd(img, output_type='dict')
    except pytesseract.pytesseract.TesseractError:
        # Can be thrown for an image with too few characters (e.g. ara.png)
        # Try to handle by passing an image which is multiple copies of the original
        img = np.concatenate((img, img, img, img, img), axis=1)
        try:
            osd_dict = pytesseract.image_to_osd(img, output_type='dict')
        except pytesseract.pytesseract.TesseractError:
            print(f'Unable to detect script. Falling back to default script (Latin).')
            osd_dict = {'script': 'Latin', 'script_conf': 0, }
    print ("---> osd_dict = ", osd_dict, flush=True)
    return osd_dict


def get_script_languages(script: str):
    # would be nice to make sense of the string
    # that tesseract needs
    # vs displaying information to the user
    # TODO add scripts and languages as needed
    switcher = {
        'Han': "eng+chi_sim+chi_tra+kor+jpg",
        'Latin': "eng+fra",
        'Arabic': "eng+ara+gas"
    }
    # get the language list associated with the script
    # defaults to English if script is not in the list
    return switcher.get(script, 'eng') 


def decode(img: Image, langs: str) -> List:
    data = pytesseract.image_to_data(img, lang=langs, output_type='dict')
    # pivot the dictionary so that it makes more sense
    # data has many keys with the same-sized value arrays
    # we would rather have a list of dictionaries for each text detection
    vals = [dict(zip(data, col)) for col in zip(*data.values())]
    return vals


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--image')
    return parser.parse_args()


def main():
    args = parse_args()
    logging.info(f"Running with {args} arguments")
    tess_version = pytesseract.get_tesseract_version()
    logging.info(f"Using Tesseract version {tess_version}")

    logging.info(f"Opening image file {args.image}")
    img = Image.open(args.image)
    logging.info(f"Detection script")
    osd = detect_script_and_orientation(img) 
    logging.debug(f"{osd}")
    script = osd['script']
    script_conf = osd['script_conf']
    # rotation is always 0 or 180. Script confidence is all over the place.
    # 0.95 might imply 95% but then another script gives a score of 83.33
    logging.info(f"Detected {script} script with {script_conf} confidence")

    langs = get_script_languages(script)
    logging.info(f"Using {langs} language models for decoding")
    output = decode(img, langs)
    for o in output:
        top = o['top']
        left = o['left']
        width = o['width']
        height = o['height']
        conf = o['conf']
        text = o['text']
        logging.info(f"{text} {conf} ({left} {top} {width} {height})")
    img.close()


if __name__ == '__main__':
    main()
