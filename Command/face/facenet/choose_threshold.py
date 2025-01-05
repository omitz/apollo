from src.classifier import *

'''
This script was run on the KNOWN test images (ie people who had images in the training data). At a 40% threshold, we still get >80% accuracy).
'''

args_list = sys.argv[1:]
args = parse_arguments(args_list)
_, emb_array, label_mapping, labels,unknown_label = extract_embeddings_array(args)

preds_path = 'predictions.npy'
if os.path.exists(preds_path):
    predictions = np.load(preds_path)
else:
    classifier_filename_exp = os.path.expanduser(args.classifier_filename)
    class_names, predictions = save_predictions(classifier_filename_exp, emb_array)

# For each image, the index of the most-likely person
best_class_indices = np.argmax(predictions, axis=1)
# For each image, the probability of that most-likely person
best_class_probabilities = predictions[
    np.arange(len(best_class_indices)),
    best_class_indices]

# Threshold
threshold = 0.5
while threshold <= 1:
    unknown_label = len(label_mapping) + 1
    best_class_indices_thresholded = list()
    for i, prob in enumerate(best_class_probabilities):
        if prob > threshold:
            best_class_indices_thresholded.append(best_class_indices[i])
        else:
            best_class_indices_thresholded.append(unknown_label)

    thresholded_acc = np.mean(np.equal(best_class_indices_thresholded, labels))
    thresholded_acc = f'Thresholded at {threshold} accuracy: %.3f\n' % thresholded_acc
    print(thresholded_acc)
    threshold -= .05