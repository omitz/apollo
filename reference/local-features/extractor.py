import sys
import argparse
import logging
import copyreg

import cv2
import sqlalchemy as db

import models
import util

logging.basicConfig(stream=sys.stdout, level=logging.INFO)
LOGGER = logging.getLogger()


# https://stackoverflow.com/questions/10045363/pickling-cv2-keypoint-causes-picklingerror
def _pickle_keypoints(point):
    return cv2.KeyPoint, (*point.pt, point.size, point.angle,
                          point.response, point.octave, point.class_id)

copyreg.pickle(cv2.KeyPoint().__class__, _pickle_keypoints)


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', required=True, help='Image path')
    parser.add_argument('-v',
                        '--verbose',
                        action='store_true',
                        help='Increase logging verbosity')
    return parser.parse_args()


def write_features(session, imgpath, keypoints, descriptors):

    image = models.Image(filepath=imgpath, keypoints=keypoints, descriptors=descriptors)
    session.add(image)

    try:
        session.commit()
    except db.exc.IntegrityError:
        session.rollback()
        LOGGER.info(f"Not unique")


def main():
    args = parse_args()
    LOGGER.info(f"{args}")
    if args.verbose:
        LOGGER.setLevel(level=logging.DEBUG)

    img = cv2.imread(args.input, 0)
    if img is None:
        LOGGER.info(f"Invalid image path '{args.input}'")
        sys.exit()

    kp, des = util.extract_features(img)
    LOGGER.info(f"Found {len(kp)} features")
    LOGGER.debug(f"# Keypoints  : {len(kp)}")
    LOGGER.debug(f"# Descriptors: {len(des)}")

    engine = db.create_engine('sqlite:///features.db')
    models.Base.metadata.create_all(engine)
    Session = db.orm.sessionmaker(engine)
    session = Session()

    write_features(session=session,
                   imgpath=args.input,
                   keypoints=kp,
                   descriptors=des)

    if LOGGER.level == logging.DEBUG:
        # draw keypoints on input image
        kp_img = util.draw_keypoints(img, kp)
        # save keypoint drawing to disk
        # cv2.imwrite(f"kp_{args.input}", kp_img)
        # display keypoint drawing
        cv2.imshow('kp_img', kp_img)
        cv2.waitKey(0)

    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
