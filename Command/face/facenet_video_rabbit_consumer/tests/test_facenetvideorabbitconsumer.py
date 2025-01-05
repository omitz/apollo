import os
from unittest import TestCase
from unittest.mock import MagicMock

from sqlalchemy_utils import database_exists, drop_database

from apollo.models import VideoFaceDetections
from face.facenet_video_rabbit_consumer.main import FacenetVideoRabbitConsumer
from face.facenet_video_rabbit_consumer.analytic import FacenetVideoAnalytic


class TestFacenetVideoRabbitConsumer(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.s3_filestore = MagicMock()
        cls.test_file_s3path = 's3://apollo-source-data/inputs/face/leo_ellen.mp4'
        cls.expected_analytic_result = {
              'prediction': [ 'Leonardo_DiCaprio',
                              'Unknown',
                              'Unknown',
                              'Unknown',
                              'Unknown',
                              'Unknown'],
              'recog_probability': [ 0.8382239361669306,
                                     0.18223601505487025,
                                     0.15052318700918932,
                                     0.15368119072964526,
                                     0.24739545208087707,
                                     0.2167231415563854],
              'seconds': [[1, 2, 3, 4], [1], [1], [2], [5], [6]]}

    def setUp(self):
        analytic = FacenetVideoAnalytic('face_vid', self.s3_filestore, download_recog_model=False)
        self.rabbit_consumer = FacenetVideoRabbitConsumer('face_vid', 'ApolloExchange', analytic, heartbeat=180)

    def tearDown(self):
        self.rabbit_consumer.database.delete_all_from_table(VideoFaceDetections)
        self.rabbit_consumer.database.close()

    def test_save_results_to_database(self):
        self.rabbit_consumer.save_results_to_database({'name': self.test_file_s3path, 'description':'face_vid'}, self.expected_analytic_result)

        #check postgres
        session = self.rabbit_consumer.database.get_session()
        query_result = session.query(VideoFaceDetections).filter_by(path=self.test_file_s3path).all()
        preds_in_pg = [query.prediction for query in query_result]
        self.assertIn('Leonardo_DiCaprio', preds_in_pg)
        seconds_in_pag = [query.seconds for query in query_result]
        self.assertIn([1, 2, 3, 4], seconds_in_pag)
        session.close()

    def test_save_results_to_database_no_detections(self):
        # Check that save_results_to_database runs without error if there were no faces detected
        expected_analytic_result = {}
        self.rabbit_consumer.save_results_to_database({'name': 'some_video_without_faces.mp4', 'description':'face_vid'}, expected_analytic_result)
