# https://www.geeksforgeeks.org/extract-images-from-video-in-python/

# Importing all necessary libraries
import cv2
import os
import imutils

# Read the video from specified path
cam = cv2.VideoCapture("/home/HQ/lneff/Downloads/20200117_111429.mp4")

try:

    # creating a folder named data
    path = '/tmp/testing_video_to_images'
    if not os.path.exists(path):
        os.makedirs(path)

    # if not created then raise error
except OSError:
    print('Error: Creating directory of data')

# frame
currentframe = 0

while True:

    # reading from frame
    ret, frame = cam.read()

    if ret:
        # For some reason, we have to rotate this to correct the orientation
        frame = imutils.rotate_bound(frame, -90)

        # if video is still left continue creating images
        name = os.path.join(path, 'frame' + str(currentframe) + '.jpg')
        print('Creating...' + name)

        # writing the extracted images
        cv2.imwrite(name, frame)

        # increasing counter so that it will
        # show how many frames are created
        currentframe += 1
    else:
        break

# Release all space and windows once done
cam.release()
cv2.destroyAllWindows()
