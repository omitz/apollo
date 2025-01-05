from commandutils.models import VideoDetections
from commandutils import postgres_utils

entry = 's3://apollo-source-data/inputs/obj_det_vid/plus+space mp4.mp4'

engine = postgres_utils.get_engine()
sess = postgres_utils.get_session(engine)
d = sess.query(VideoDetections).filter(VideoDetections.path == entry).all()

for res in d:
    sess.delete(res)
    sess.commit()

sess.close()
engine.dispose()