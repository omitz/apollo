import cv2


def extract_features(img, max_features=1000):
    '''Extract ORB features from the image'''
    orb = cv2.ORB_create(nfeatures=max_features)
    kp, des = orb.detectAndCompute(img, None)
    return kp, des


def draw_keypoints(img, kp):
    '''Draw keypoints on the image and return the marked up image'''
    return cv2.drawKeypoints(img, kp, None, flags=0)


def draw_matches(img1, img2, kp1, kp2, matches, n):
    '''Draw feature correspondences on two images and return marked up image'''
    return cv2.drawMatches(img1,kp1,img2,kp2,matches[:n],None,flags=cv2.NOT_DRAW_SINGLE_POINTS)
