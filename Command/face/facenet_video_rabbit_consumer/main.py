
from apollo import RabbitConsumer, PostgresDatabase
from apollo.models import VideoFaceDetections
from face.facenet_video_rabbit_consumer.analytic import FacenetVideoAnalytic


class FacenetVideoRabbitConsumer(RabbitConsumer):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase('apollo', VideoFaceDetections.__table__)

    def save_results_to_database(self, msg_dict, results: dict):

        if 'original_source' in msg_dict:
            source = msg_dict['original_source']
        else:
            source = msg_dict['name']

        # Convert the dict to have keys matching the column names for the VideoFaceDetections model
        prediction_results = results.get('prediction', None)
        if prediction_results is None:
            return
        num_entries = len(results['prediction'])
        results['path'] = [msg_dict['name'] for _ in range(num_entries)]
        for i in range(num_entries):
            row_dict = {k:v[i] for k, v in results.items()}
            row_dict['original_source'] = source
            print(f'row: {row_dict}', flush=True)
            self.database.save_record_to_database(row_dict, VideoFaceDetections)


def main():

    face_vid_analytic = FacenetVideoAnalytic('face_vid')

    rabbit_consumer = FacenetVideoRabbitConsumer('face_vid', 'ApolloExchange', face_vid_analytic, heartbeat=180)
    rabbit_consumer.run()


if __name__ == '__main__':

    main()
