from apollo import RabbitConsumer
from apollo import PostgresDatabase
from apollo.models import SearchFullText

class FullTextSearchRabbitConsumer (RabbitConsumer):
    database = None
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase (table=SearchFullText.__table__)

        
    def save_results_to_database(self, msg_dict, analytic_results):
        """
        Args:
          msg_dict: the rabbit message is a python dictionary.
          json_results: The output of analytic.run

        """
        assert (analytic_results != None)    # catch error.

        # populate the record into the table:
        row = analytic_results
        servicename = row['service_name']
        s3filepath = row['path']
        if self.database.check_processed (s3filepath, SearchFullText, servicename):
            print (f"{s3filepath} by service '{servicename}' already in the database -- Skip it")
        else:
            self.database.save_record_to_database (row, SearchFullText)


