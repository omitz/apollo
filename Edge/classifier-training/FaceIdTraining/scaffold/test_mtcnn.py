#! /usr/bin/env python3
#
# Test 
# TC 2021-05-06 (Thu) 

# face detection with mtcnn on a photograph
from matplotlib import pyplot
from matplotlib.patches import Rectangle
from mtcnn.mtcnn import MTCNN
import skimage.io

# draw an image with detected objects
def draw_image_with_boxes(data, result_list):
    # plot the image
    pyplot.imshow(data)
    # get the context for drawing boxes
    ax = pyplot.gca()
    # plot each box
    for result in result_list:
            # get coordinates
            x, y, width, height = result['box'] #  (col, row)  top of image is (0,0)
            # create the shape
            rect = Rectangle((x, y), width, height, fill=False, color='red')
            # draw the box
            ax.add_patch(rect)
    # show the plot
    pyplot.show()

# load image from file
filename = 'test_image.png' 
#filename = 'test2.jpg'
# pixels = pyplot.imread(filename)
# must be unin8 data formt
pixels = skimage.io.imread (filename)
pixels = pixels[:,:,0:3]        # get rid of alpha


# create the detector, using default weights
detector = MTCNN()

# detect faces in the image
faces = detector.detect_faces(pixels)
for face in faces:
    print(face)
draw_image_with_boxes (pixels, faces)
