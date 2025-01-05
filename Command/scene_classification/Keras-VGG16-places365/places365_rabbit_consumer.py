from apollo import RabbitConsumer
from apollo import PostgresDatabase
from apollo import ApolloMessage
from apollo.models import ClassifyScene

class Places365RabbitConsumer (RabbitConsumer):
    database = None
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase (table=ClassifyScene.__table__)
        
    def save_results_to_database(self, msg_dict, analytic_results):
        """
        Args:
          msg_dict: the rabbit message is a python dictionary.
          json_results: The output of analytic.run

        """
        assert (analytic_results != None)    # catch error.

        # populate the record into the table:
        row = analytic_results
        
        if 'original_source' in msg_dict:
            row['original_source'] = msg_dict['original_source']
        else:
            row['original_source'] = msg_dict['name']

        # print(f'row: {row}', flush=True)
        self.database.save_record_to_database (row, ClassifyScene)

        # Send outdoor detections to the Landmarks analytic
        if row['class_hierarchy'] == 'outdoor':
            s3path = row['path']
            print(f'Sending message for {s3path} to landmark_queue', flush=True)
            apollomsg = ApolloMessage({'name': s3path, 'description': 'landmark', 'original_source': row['original_source']})
            apollomsg.publish('landmark_route')
