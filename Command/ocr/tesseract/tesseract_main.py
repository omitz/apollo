#!/usr/bin/env python3
"""
Stand-alone Tesseract OCR program.

2020-07-15
"""

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import sys
import os
import argparse
import textwrap
from pathlib import PosixPath, Path

from pytesseract import Output
import pytesseract
import cv2
import json
from langdetect import detect as ld
from langdetect import lang_detect_exception as ldexception

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------

from ocr import detect_script_and_orientation, get_script_languages


def ConvertPreditionToJson (width, height, predictions):
    """
    """

    class NpEncoder(json.JSONEncoder):
        def default(self, obj):
            if isinstance(obj, np.integer):
                return int(obj)
            elif isinstance(obj, np.floating):
                return float(obj)
            elif isinstance(obj, np.ndarray):
                return obj.tolist()
            else:
                return super(NpEncoder, self).default(obj)

    return json.dumps ({"width":width, "height":height, "pred":predictions}, cls=NpEncoder)
    


def run (inImgPath: PosixPath, outTxtPath: PosixPath,
         outImgPath: PosixPath=None, outInfoPath: PosixPath=None,
         tess_lang: str=None) -> bool:
    """Run tesseract OCR.

    Example taken from pyimagesearch.

    Args:
      inImgPath:
        Location of the input image file.
      outTxtPath:
        Location of the output text file.
      outImgPath:
        Location of the output overlay image file.
      outInfoPath:
        Location of the debgging text file.
      tess_lang:
        One of "spa" "eng", "rus", "ara", "fra".  If not provided, will try autodetect.

    Returns:
      True: successful.
      False: unsuccessful.  See error messages.
    """

    in_image_path_string = str(inImgPath)
    ## Read image with Tesseract
    image, results = run_image_to_data(in_image_path_string, tess_lang)
    print('results = ', results, flush=True)

    print('Filtering results...', flush=True)
    prediction_group, text = filter_results(results, image)
    print(f'Results filtered.')

    ## Save predition info out:
    if outInfoPath:
        (height, width, _) = image.shape
        outInfoPath.write_text (ConvertPreditionToJson (width, height, prediction_group))

    ## save output text and bounding box:
    outTxtPath.write_text (text + "\n")

    ## Plot the predictions [optional]
    if outImgPath:
        cv2.imwrite (str(outImgPath), image)

    return True


def run_image_to_data(image_path_string, tess_lang=None):
    # load the input image, convert it from BGR to RGB channel ordering,
    # and use Tesseract to localize each area of text in the input image
    # tess_lang = "spa" "eng", "rus", "ara", "fra"
    image = cv2.imread(image_path_string)
    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
    if tess_lang == None:
        tess_lang = determine_lang_arg(rgb)
    print(f'Running pytesseract image_to_data...', flush=True)
    results = pytesseract.image_to_data(rgb, output_type=Output.DICT, lang=tess_lang)
    print(f'Finished image_to_data.', flush=True)
    return image, results


def determine_lang_arg(image):
    '''
    Determine the optimal tesseract language model to use
    :return: 'eng', 'ara', 'fra', 'rus', or 'spa'
    '''
    # Use tesseract to determine the script type
    script = detect_script_and_orientation(image).get('script', 'Latin')
    print(f'script: {script}', flush=True)
    if script == 'Latin':
        # Run once with eng model, then use langdetect to determine whether to run with english or french model
        results = pytesseract.image_to_data(image, output_type=Output.DICT, lang='eng')
        _, text = filter_results(results, None)
        try:
            detected_lang = ld(text)
        except ldexception.LangDetectException:
            # Can be thrown for short or empty strings
            print(f'Unable to detect language. Falling back to default language (English).', flush=True)
            detected_lang = 'en'
        langdect_to_tesseract = {
            'en': 'eng',
            'fr': 'fra',
            'es': 'spa'
        }
        tess_lang = langdect_to_tesseract.get(detected_lang, 'eng')
    else:
        tess_script_to_tess_lang = {
            'Arabic': 'ara',
            'Cyrillic': 'rus'
        }
        tess_lang = tess_script_to_tess_lang.get(script, 'eng')
    print(f'Using Tesseract\'s {tess_lang} model', flush=True)
    return tess_lang


def filter_results(results, image=None):
    # loop over each of the individual text localizations
    min_conf = 0.4  # [0..1]
    wordList = []
    prediction_group = []
    for i in range(0, len(results["text"])):
        # extract the bounding box coordinates of the text region
        x = results["left"][i]
        y = results["top"][i]
        w = results["width"][i]
        h = results["height"][i]

        # extract the OCR text itself along with the confidence
        word = results["text"][i]
        conf = int(results["conf"][i]) / 100.0

        # throw away conf == -1
        if conf < 0:
            continue

        # Save prediction
        tl = (x,y); tr = (x+w,y); br = (x+w,y+h); bl = (x,y+h);
        bbox = (tl, tr, br, bl)
        prediction = (bbox, word, conf)  # (bbox, text, conf)
        prediction_group.append(prediction)

        # filter out weak confidence text localizations
        if conf > min_conf:
            wordList.append(word)
            # strip out non-ASCII text so we can draw the text on the image
            # using OpenCV, then draw a bounding box around the text along
            # with the text itself. (ref: pyimagesearch)
            ascii_word = "".join([c if ord(c) < 128 else "" for c in word]).strip()
            if image is not None:
                cv2.rectangle(image, (x, y), (x + w, y + h), (0, 255, 0), 2)  # can only do rectangle..
                cv2.putText(image, ascii_word, (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX,
                            0.5, (0, 0, 255), 1)

    text = "".join(word + " " for word in wordList)
    return prediction_group, text


def parse_args () -> argparse.Namespace:
    # Create a parser:
    description="""
    Tesseract text localization and OCR.
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("inImgFile", help="input image file")
    parser.add_argument ("outTxtFile", help="output text file")
    parser.add_argument ("outImgFile", help="output image overlay file")
    parser.add_argument ("outInfoFile", help="output predition info file.")

    # Specify Example:
    parser.epilog='''Example:
        %s tests/images/ocr.png out.txt out.png pred.json
    ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)

    return args


#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------
    args = parse_args()

        
    #---------------------------
    # run the program :
    #---------------------------
    run (Path(args.inImgFile), Path(args.outTxtFile),
         Path(args.outImgFile), Path(args.outInfoFile))

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

