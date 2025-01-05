
import os
import sys

from PIL import Image, UnidentifiedImageError
import PIL.ImageDraw as ImageDraw
import PIL.ImageFont as ImageFont
import numpy as np
import pandas as pd



#from commandutils.vis_utils import draw_bounding_box_on_image


def draw_bounding_box_on_image(image,
                               ymin,
                               xmin,
                               ymax,
                               xmax,
                               color='red',
                               thickness=4,
                               display_str_list=(),
                               use_normalized_coordinates=True):
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
    draw = ImageDraw.Draw(image)
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
        font = ImageFont.truetype(font_path, 18)
    except IOError:
        output('Couldn\'t find font')
        font = ImageFont.load_default()

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


def find(name, path):
    for root, dirs, files in os.walk(path):
        if name in files:
            return os.path.join(root, name)


def output(output):
    print(output)
    sys.stdout.flush()


def bb_area(box_df):
    # https://stackoverflow.com/questions/25349178/calculating-percentage-of-bounding-box-overlap-for-image-detector-evaluation
    return (box_df.lrx[0] - box_df.ulx[0]) * (box_df.lry[0] - box_df.uly[0])


def calc_iou(box_df1, box_df2):
    # https://stackoverflow.com/questions/25349178/calculating-percentage-of-bounding-box-overlap-for-image-detector-evaluation
    both = pd.concat([box_df1, box_df2])
    # Coordinates of intersection rectangle
    iulx = max(both.ulx)
    iuly = max(both.uly)
    ilrx = min(both.lrx)
    ilry = min(both.lry)
    if ilrx < iulx or ilry < iuly:
        return 0

    intersection_area = (ilrx - iulx) * (ilry - iuly)
    b1_area = bb_area(box_df1)
    b2_area = bb_area(box_df2)
    # Compute the intersection over union
    iou = intersection_area / float(b1_area + b2_area - intersection_area)
    return iou


def draw_results(colors, df, full_size):
    # Draw the result on the image
    try:
        im = Image.open(full_size)
    except UnidentifiedImageError: # Occurs when input file is empty (0 bytes)
        exception = f'{full_size} may contain 0 bytes.'
        raise Exception(exception)
    # If there were faces detected
    if len(df.index) > 0:
        for i, row in df.iterrows():
            prob = df.probability.iloc[i]
            prob_as_str = '{0:.2%}'.format(prob)
            name_and_prob = f'{df.prediction.iloc[i]}: {prob_as_str}'
            color = colors[i]
            draw_bounding_box_on_image(im, df.uly[i], df.ulx[i], df.lry[i], df.lrx[i], color=color,
                                       display_str_list=[name_and_prob], use_normalized_coordinates=False)
    return im


def rm_overlap(df):
    df.columns = ['path', 'ulx', 'uly', 'lrx', 'lry']  # ulx = upper-left x, etc.
    no_overlap = {key: [] for key in df.columns}
    # Remove overlapping boxes
    for i, row in df.iterrows():
        box_df = df.iloc[[i]].reset_index(drop=True)
        # Check against all boxes below
        max_iou = 0
        n = len(df) - i - 1
        # dfs which are slices of other dfs by default have their original indices. reset_index guards allows us to iterate in a standard manner
        remaining_rows = df.tail(n=n).reset_index(drop=True)
        for j, rowj in remaining_rows.iterrows():
            box2_df = remaining_rows.iloc[[j]].reset_index(drop=True)
            iou = calc_iou(box_df, box2_df)
            if iou > max_iou:
                max_iou = iou
        # if no overlap, add to no_overlap
        if max_iou < 0.5:
            no_overlap['path'].append(box_df.path[0])
            no_overlap['ulx'].append(box_df.ulx[0])
            no_overlap['uly'].append(box_df.uly[0])
            no_overlap['lrx'].append(box_df.lrx[0])
            no_overlap['lry'].append(box_df.lry[0])
    df = pd.DataFrame.from_dict(no_overlap)
    print(f'Removed overlapping detected boxes.')
    return df