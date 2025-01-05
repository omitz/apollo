from apollo import RabbitConsumer
from apollo import PostgresDatabase
from apollo import ApolloMessage
from apollo.models import AnalyzeSentiment

class TextBlobRabbitConsumer (RabbitConsumer):
    database = None

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase (table=AnalyzeSentiment.__table__)
        pass
        
    def save_results_to_database(self, msg_dict, analytic_results):
        """
        Args:
          msg_dict: the rabbit message, which is a python dictionary.
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
        self.database.save_record_to_database (row, AnalyzeSentiment)
