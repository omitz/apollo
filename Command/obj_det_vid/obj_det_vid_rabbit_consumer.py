import ast
import boto3 as boto3
import mpu.aws as mpuaws
import itertools

from apollo import RabbitConsumer, PostgresDatabase
from apollo.models import VideoDetections

class ObjDetVidRabbitConsumer(RabbitConsumer):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase(table=VideoDetections.__table__)

    def callback(self, ch, method, properties, body):
        """
            Read a rabbitmq message, check if file exists in s3

            We expect messages like:
            {"name": "<s3 image path>", "num_milvus_results": 5, "description": "face"}

            It can be read back as a python dictionary.
        """

        # Show the message:
        print(f"[x] Received {body}")

        # Evaluate the the message as a python statement
        try:
            body = body.decode('utf-8')
            msg_dict = ast.literal_eval(body)
        except:
            print("message is not a valid Python expression")
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print(f'sent acknowledgement to rabbitmq route {method.routing_key}')
            return

        if self.analytic.name.strip('-').strip('_') in msg_dict['description'].strip('-').strip('_'):
            # ignore dashes, underscores in description and service_name
            s3_client = boto3.client('s3')

            s3_file_path = msg_dict['name']
            print(f's3 filepath: {s3_file_path}')
            bucket, prefix = mpuaws._s3_path_split(s3_file_path)

            try:
                s3 = boto3.resource('s3')
                print(f'about to load {bucket}, {prefix}')
                s3.Object(bucket, prefix).load()

                results = self.analytic.run(msg_dict['name'])
                self.save_results_to_database(results, msg_dict)

                # If necessary, reinitialize the model, so it's ready for the next job
                if not self.analytic.model_initialized:
                    self.analytic.init_model()

            except s3_client.exceptions.ClientError as e:
                if e.response['Error']['Code'] == "404":
                    print(f"\"{s3_file_path}\" is not an s3 object that exists")
        else:
            print(f"input is not intended for {self.analytic.name}")

        # debug: we are done
        if ch and method:
            # send ack
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print('sent acknowledgement to rabbitmq route {}'.format(method.routing_key), flush=True)

        print(f"[x] Finished {body}", flush=True)

    def save_results_to_database(self, detections_dict, msg_dict):

        # Create a dictionary formatted for `save_record_to_database
        formatted = []
        for obj_class, sec_prob in detections_dict.items():
            obj_class_data = {}
            obj_class_data['path'] = msg_dict['name']
            obj_class_data['detection_class'] = obj_class
            probability = detections_dict[obj_class][self.analytic.obj_and_sec_prob_key]
            obj_class_data['detection_score'] = round(probability, 2)  # Round to 2 decimal places
            # Turn the seconds sets into ranges
            seconds_list = list(detections_dict[obj_class][self.analytic.obj_and_sec_sec_key])
            ranges = list(get_ranges(seconds_list))
            obj_class_data[self.analytic.obj_and_sec_sec_key] = ranges
            
            if 'original_source' in msg_dict:
                source = msg_dict['original_source']
            else:
                source = msg_dict['name']

            obj_class_data['original_source'] = source
            
            formatted.append(obj_class_data)

        for obj_class in formatted:
            self.database.save_record_to_database(obj_class, VideoDetections)


def get_ranges(sorted_list_of_ints):
    # https://stackoverflow.com/questions/4628333/converting-a-list-of-integers-into-range-in-python
    grouped = itertools.groupby(enumerate(sorted_list_of_ints), lambda pair: pair[1] - pair[0])
    for a, b in grouped:
        b = list(b)
        yield b[0][1], b[-1][1]