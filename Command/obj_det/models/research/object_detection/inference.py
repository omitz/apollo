import numpy as np
import pandas as pd
import os
import tensorflow as tf
import cv2
import argparse
import json
from PIL import Image
from collections import defaultdict
from obj_det.models.research.object_detection.obj_det_utils import label_map_util
from obj_det.models.research.object_detection.obj_det_utils import obj_det_vis_utils
from obj_det.od_file_utils import make_outfile_name

'''
Run inference on image and save the results to a json file.
'''

def run_inference_for_image(img, graph):
    with graph.as_default():

        # ln When trying to run inference using the frozen graph of a model that I had trained, I got the following error:
        # Failed to get convolution algorithm. This is probably because cuDNN failed to initialize
        # The allow growth option (the following 3 lines) seems to fix it.
        config = tf.ConfigProto()
        config.gpu_options.allow_growth = True
        with tf.Session(config=config) as sess:

            # Get handles to input and output tensors
            ops = tf.get_default_graph().get_operations()
            all_tensor_names = {output.name for op in ops for output in op.outputs}
            tensor_dict = {}
            for key in [
                'num_detections', 'detection_boxes', 'detection_scores',
                'detection_classes'
            ]:
                tensor_name = key + ':0'
                if tensor_name in all_tensor_names:
                    tensor_dict[key] = tf.get_default_graph().get_tensor_by_name(
                        tensor_name)
            image_tensor = tf.get_default_graph().get_tensor_by_name('image_tensor:0')

            # Run inference
            results_dict = sess.run(tensor_dict,
                                  feed_dict={image_tensor: np.array([img])})
    return results_dict


def visualize_bounding_boxes(category_index,
                             image_np,
                             bounding_boxes,
                             labels_array,
                             confidence_array,
                             outdir,
                             name):
    # Visualization of the results of a detection.
    for_det = np.copy(image_np)
    labels_array = labels_array.astype(np.int)
    # Make line thickness relative to image size (but never < 4)
    thickness = int(round(4 + (image_np.shape[0] * image_np.shape[1] * 2e-6)))
    obj_det_vis_utils.visualize_boxes_and_labels_on_image_array(
        for_det,
        np.array(bounding_boxes),  # Order of ymin, xmin, ymax, xmax
        labels_array,
        confidence_array,
        category_index,
        use_normalized_coordinates=True,
        line_thickness=thickness,
        min_score_thresh=get_min_score_thresh())
    filename = '{}_result.png'.format(name)
    outfile = os.path.join(outdir, filename)
    im = Image.fromarray(for_det)
    im.save(outfile)


def get_min_score_thresh():
    return 0.5


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', default='obj_det/models/research/object_detection/test_images/imgs/image1.jpg',
                        help="Image to process")
    parser.add_argument('-s', '--source',
                        help="S3 image path")
    parser.add_argument('-g', '--graph', default='obj_det/models/samples/ssd_mobilenet_v1_ppn_coco/frozen_inference_graph.pb',
                        help="Path to the frozen model graph")
    parser.add_argument('-l', '--labels', default='obj_det/models/research/object_detection/data/mscoco_complete_label_map.pbtxt',
                        help="Path to file which maps integers to their label")
    parser.add_argument('-o', '--output', default='obj_det/models/research/object_detection/test_images/imgs',
                        help="Directory where results json will be written out")
    parser.add_argument('-v', '--visualize', action='store_true', help="Save out the images with drawn bounding boxes.")

    return parser.parse_args()


def main():
    args = parse_args()
    process(args.input, args.output, graph=args.graph, labels=args.labels, visualize=args.visualize)

def process(img_file, outdir, PATH_TO_FROZEN_GRAPH='obj_det/models/samples/ssd_mobilenet_v1_ppn_coco/frozen_inference_graph.pb', PATH_TO_LABELS='obj_det/models/research/object_detection/data/mscoco_complete_label_map.pbtxt', visualize=False):

    detection_graph = tf.Graph()
    with detection_graph.as_default():
        od_graph_def = tf.GraphDef()
        with tf.gfile.GFile(PATH_TO_FROZEN_GRAPH, 'rb') as fid:
            serialized_graph = fid.read()
            od_graph_def.ParseFromString(serialized_graph)
            tf.import_graph_def(od_graph_def, name='')

    category_index = label_map_util.create_category_index_from_labelmap(PATH_TO_LABELS, use_display_name=True)

    img = read_image(img_file)

    # OpenCV loads images in as BGR by default, so fix by reversing
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    output_dict = run_inference_for_image(img, detection_graph)
    
    outname = make_outfile_name(img_file)
    out = os.path.join(outdir, outname)
    write_results_to_file(out, img_file, output_dict)
    
    if visualize:
        # Save out images with bounding box drawings
        name = os.path.splitext(os.path.basename(img_file))[0]
        visualize_bounding_boxes(category_index,
                                 img,
                                 output_dict['detection_boxes'][0],
                                 output_dict['detection_classes'][0],
                                 output_dict['detection_scores'][0],
                                 outdir,
                                 name)

    return output_dict

def read_image(img_file):
    if not os.path.isfile(img_file):
        raise FileNotFoundError
    if os.stat(img_file).st_size == 0:
        msg = f'{img_file} is empty.'
        raise Exception(msg)
    img = cv2.imread(img_file)
    return img


def format_for_postgres(path, category_index, output_dict):
    # Format data to save results to postgres
    # Remove low probability detections and vars we don't need in postgres
    probabilities_sorted_high_low = output_dict['detection_scores'][0]
    num_higher_than_thresh = len([i for i in probabilities_sorted_high_low if i > get_min_score_thresh()])
    reduced_dict = dict()
    boxes = output_dict['detection_boxes'][0][:num_higher_than_thresh]
    reduced_dict['bb_ymin_xmin_ymax_xmax'] = boxes.astype(float) #psycopg2.ProgrammingError: can't adapt type 'numpy.float32'
    class_ints = output_dict['detection_classes'][0][:num_higher_than_thresh]
    class_strs = []
    for class_float in class_ints:
        class_str = obj_det_vis_utils.get_class_str(category_index, int(class_float))
        class_strs.append(class_str)
    reduced_dict['detection_class'] = class_strs
    reduced_dict['detection_score'] = output_dict['detection_scores'][0][:num_higher_than_thresh].astype(float)
    combine_class_instances = defaultdict(list)
    unique_classes = set(reduced_dict['detection_class'])
    for unique_class in unique_classes:
        combine_class_instances['detection_class'].append(unique_class)
        bbs = []
        scores = []
        for i in range(len(reduced_dict['detection_score'])):
            if reduced_dict['detection_class'][i] == unique_class:
                bbs.append(list(reduced_dict['bb_ymin_xmin_ymax_xmax'][i]))
                scores.append(reduced_dict['detection_score'][i])
        combine_class_instances['bb_ymin_xmin_ymax_xmax'].append(bbs)
        combine_class_instances['detection_scores'].append(scores)
    combine_class_instances['path'] = [path for i in range(len(unique_classes))]
    return combine_class_instances


def write_results_to_file(out_file, img_file, output_dict):
    results = dict()
    results[img_file] = {'boxes': output_dict['detection_boxes'].tolist(),
                         'classes': output_dict['detection_classes'].tolist(),
                         'confidence': output_dict['detection_scores'].tolist()}
    
    with open(out_file, 'w') as write_file:
        json.dump(results, write_file)
    write_file.close()

    return json.dumps(results)



if __name__ == '__main__':
    main()
