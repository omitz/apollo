import sys
import argparse
import logging

import cv2
import numpy as np
import sqlalchemy as db

import models
import util

logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
LOGGER = logging.getLogger()


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', required=True, help='Image path')
    parser.add_argument('-b', '--bbox', nargs="+", type=int)
    parser.add_argument('-v',
                        '--verbose',
                        action='store_true',
                        help='Increase logging verbosity')
    return parser.parse_args()


def match(query_descriptors, image_descriptors):
    bf = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
    matches = bf.match(query_descriptors, image_descriptors)
    sorted_matches = sorted(matches, key=lambda x: x.distance)
    return sorted_matches


def main():
    args = parse_args()
    LOGGER.info(args)

    img = cv2.imread(args.input, 0)
    if img is None:
        LOGGER.info(f"Invalid image path '{args.input}'")
        sys.exit()

    # extract subimage if bbox was provided
    if args.bbox:
        LOGGER.info(f"BBOX provided")
        x1 = args.bbox[0]
        y1 = args.bbox[1]
        x2 = args.bbox[2]
        y2 = args.bbox[3]
        img = img[y1:y2, x1:x2]

    # connect to the database
    engine = db.create_engine('sqlite:///features.db')
    Session = db.orm.sessionmaker(engine)
    session = Session()

    # extract features and descriptors from the query image
    qkp, qdes = util.extract_features(img, 500)
    LOGGER.info(f"Found {len(qkp)} features in query image")
    LOGGER.debug(f"# Keypoints  : {len(qkp)}")
    LOGGER.debug(f"# Descriptors: {len(qdes)}")

    # query the database for all image features/descriptors
    result = session.query(models.Image).all()

    scored_results = {}

    # loop through query results
    for image in result:
        LOGGER.debug(f"Processing {image.filepath}")
        matches = match(qdes, image.descriptors)
        LOGGER.debug(f"Found {len(matches)} matches")

        # solve transform for good feature correspondences
        src_pts = np.float32([ qkp[m.queryIdx].pt for m in matches ]).reshape(-1,1,2)
        dst_pts = np.float32([ image.keypoints[m.trainIdx].pt for m in matches ]).reshape(-1,1,2)
        M, mask = cv2.findHomography(src_pts, dst_pts, cv2.RANSAC, 5.0)
        LOGGER.debug(f"Solved homography \n{M}")

        # transform query features to image
        dst = cv2.perspectiveTransform(src_pts,M)
        # use Euclidean distance for the best 10 points to get a score
        # note that sqrt is expensive and we could change this to
        # squared distance
        distance = np.sqrt(np.sum((dst[:10] - dst_pts[:10])**2))
        LOGGER.debug(f"Score for {image.filepath} is {distance}")

        # store distance score
        scored_results[image.filepath] = distance

    sorted_results = sorted(scored_results.items(), key=lambda x: x[1])
    LOGGER.debug(f"Final scores: {sorted_results}")
    LOGGER.info(f"Best match is {sorted_results[0]}")
    best_match = sorted_results[0][0]


if __name__ == '__main__':
    main()
