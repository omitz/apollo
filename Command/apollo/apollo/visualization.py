"""
https://github.com/tensorflow/models/blob/master/research/object_detection/utils/visualization_utils.py
"""

import os
import sys

import numpy as np
import PIL

def draw_bounding_box_on_image(image: PIL.Image,
                               ymin: int,
                               xmin: int,
                               ymax: int,
                               xmax: int,
                               color: str='red',
                               thickness: int=4,
                               display_str_list: list=(),
                               use_normalized_coordinates: bool=True):
    """Adds a bounding box to an image.

    Bounding box coordinates can be specified in either absolute (pixel) or
    normalized coordinates by setting the use_normalized_coordinates argument.

    Each string in display_str_list is displayed on a separate line above the
    bounding box in black text on a rectangle filled with the input 'color'.
    If the top of the bounding box extends to the edge of the image, the strings
    are displayed below the bounding box.

    Args:
        image: a PIL.Image object.
        ymin: ymin of bounding box.
        xmin: xmin of bounding box.
        ymax: ymax of bounding box.
        xmax: xmax of bounding box.
        color: color to draw bounding box. Default is red.
        thickness: line thickness. Default value is 4.
        display_str_list: list of strings to display in box
                        (each to be shown on its own line).
        use_normalized_coordinates: If True (default), treat coordinates
        ymin, xmin, ymax, xmax as relative to the image.  Otherwise treat
        coordinates as absolute.
    """
    draw = PIL.ImageDraw.Draw(image)
    im_width, im_height = image.size
    if use_normalized_coordinates:
        (left, right, top, bottom) = (xmin * im_width, xmax * im_width,
                                      ymin * im_height, ymax * im_height)
    else:
        (left, right, top, bottom) = (xmin, xmax, ymin, ymax)
    draw.line([(left, top), (left, bottom), (right, bottom),
               (right, top), (left, top)], width=thickness, fill=color)
    try:
        font_path = find('Roboto-Black.ttf', '/')
        font = PIL.ImageFont.truetype(font_path, 18)
    except IOError:
        output('Couldn\'t find font')
        font = PIL.ImageFont.load_default()

    # If the total height of the display strings added to the top of the bounding
    # box exceeds the top of the image, stack the strings below the bounding box
    # instead of above.
    display_str_heights = [font.getsize(ds)[1] for ds in display_str_list]
    # Each display_str has a top and bottom margin of 0.05x.
    total_display_str_height = (1 + 2 * 0.05) * sum(display_str_heights)
    if top > total_display_str_height:
        text_bottom = top
    else:
        text_bottom = bottom + total_display_str_height

    # If the total width of the display strings exceeds the right of the image, shift to the left
    display_str_widths = [font.getsize(ds)[0] for ds in display_str_list]
    total_display_str_width = sum(display_str_widths)
    if (left + total_display_str_width) <= im_width:
        text_left = left
    else:
        text_left = right - total_display_str_width
        # If necessary, cut off right end instead of left
        if text_left < 0:
            text_left = 0

    # Reverse list and print from bottom to top.
    for display_str in display_str_list[::-1]:
        text_width, text_height = font.getsize(display_str)
        margin = np.ceil(0.05 * text_height)
        draw.rectangle(
            [(text_left, text_bottom - text_height - 2 * margin),
             (text_left + text_width, text_bottom)],
            fill=color)
        if color == 'blue':
            text_color = 'white'
        else:
            text_color = 'black'
        draw.text(
            (text_left + margin, text_bottom - text_height - margin),
            display_str,
            fill=text_color,
            font=font)
        text_bottom -= text_height - 2 * margin


def find(name: str, path: str):
    """Find a file in the path"""
    for root, dirs, files in os.walk(path):
        if name in files:
            return os.path.join(root, name)


def output(output: str):
    """Print a string"""
    print(output)
    sys.stdout.flush()