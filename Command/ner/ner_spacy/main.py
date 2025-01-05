from __future__ import print_function
import os
import nltk
import magic
from commandutils import file_utils, s3_utils
from commandutils.rabbit_worker import RabbitWorkerArgs
from ner_file_utils import get_s3_outdir
from inference import inference
from commandutils import neo4j_utils


S3_BUCKET = 'apollo-source-data'

def run_inference(msg_dict):
    """
    :param source: eg "s3://apollo-source-data/inputs/ner/test.txt"
    """

    fm = file_utils.FileManager()
    source = msg_dict['name']
    print("processing " + source, flush=True)
    s3, target = s3_utils.access_bucket_and_download(source, fm.ram_storage)
    text_file = os.path.split(target)[1]
    local_filepath = os.path.join(fm.ram_storage, text_file)

    # The inference script needs the mime type in order to use the correct package to read the file. (We can't count on getting the mimetype from the filetype checker because messages can be sent directly to the NER queue.)
    file_checker = magic.Magic(mime=True)
    mime_type = file_checker.from_file(local_filepath)

    # Double quotes in case of spaces
    results_df = inference(local_filepath, mime_type)


    # Save to neo4j database
    if 'original_source' in msg_dict:
        original_source = msg_dict['original_source']
    else:
        original_source = msg_dict['name']

    graph = neo4j_utils.get_graph()
    neo4j_utils.save_dataframe_to_graph(graph, results_df, source, original_source)

    s3_utils.save_results_to_s3(fm.local_outdir, s3, get_s3_outdir())

    fm.cleanup()


if __name__ == '__main__':
    nltk.download('punkt')

    bunny = RabbitWorkerArgs('ApolloExchange', 'named_entity_recognition', heartbeat=180)

    bunny.service_fullname = "Named Entity Recognition"

    # Start getting rabbitMQ messages
    bunny.work(run_inference)
