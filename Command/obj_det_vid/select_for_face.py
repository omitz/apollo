import os
import time
import cv2
from matplotlib import pyplot as plt
from imageai.Detection.det_and_classify import get_od_prob_key, get_person_class, get_img_arr_key, get_class_name_key


def select_persons_for_recognition(output_frames_dict, visualize=False):
    print('Selecting persons to pass to face recognition...', flush=True)

    start = time.time()
    potentially_pass_to_face = []
    frames = sorted(output_frames_dict.keys())
    orb = cv2.ORB_create(nfeatures=1000)
    high_prob_person_count = 0
    for frame in frames:
        if frame % 1000 == 0:
            print(f'Processing frame {frame}/{frames[-1]}', flush=True)
        detection_list = output_frames_dict[frame]
        for obj_dict in detection_list:
            obj_class = obj_dict[get_class_name_key()]
            # Extract just the high-confidence person detections
            if obj_class == get_person_class() and get_img_arr_key() in obj_dict:
                high_prob_person_count += 1
                obj_dict['frame'] = frame
                add_descriptor_to_dict(orb, obj_dict)
                # If we don't already have a person image that's similar, append this person image
                compare_to_prev(obj_dict, potentially_pass_to_face, visualize=visualize)
    print(f'Found {high_prob_person_count} detections to potentially pass to face recognition', flush=True)
    # Change to a more appropriate variable name
    pass_to_face = potentially_pass_to_face

    print(f'Found {len(pass_to_face)} detections to pass to face recognition')
    end = time.time()
    print(f'Took {end - start} seconds to select specific people', flush=True)

    if visualize:
        # Remove any lingering output from the previous run
        prev_output = os.listdir('output')
        for file in prev_output:
            os.remove(os.path.join('output', file))
        # Save the detections that would be passed on to face recognition
        for frame in sorted(set([d['frame'] for d in pass_to_face])):
            i = 0
            this_frame = [d for d in pass_to_face if d['frame'] == frame]
            for person_dict_vis in this_frame:
                plt.imshow(person_dict_vis[get_img_arr_key()])
                plt.title(frame)
                plt.savefig(f'output/after_{frame}_{i}')
                i += 1


def compare_to_prev(obj_dict, potentially_pass_to_face, visualize=False):
    for i in range(len(potentially_pass_to_face)):
        obj_dict_frame = obj_dict['frame']
        oth_frame = potentially_pass_to_face[i]['frame']
        if not obj_dict_frame == oth_frame: # Save ourselves a little time. If these two detections are in the same frame, they're not the same person
            num_matches = get_matches(obj_dict['desc'], potentially_pass_to_face[i]['desc'])
            if num_matches > 25:  # If we're basically looking at the same image from different frames # Why threshold at 25? This was chosen arbitrarily. There's tradeoffs here between runtime, the number of images we pass on, and the likelihood we missed someone unique. As the threshold increases, runtime increases, the number of images to pass on increases, and the likelihood we missed someone decreases.
                # Only send the highest confidence detection (we're making an assumption here that the highest-confidence detection will be the best capture of that face for face recognition)
                if visualize:
                    # Display the images we're comparing side-by-side
                    f, axarr = plt.subplots(1, 2)
                    axarr[0].imshow(obj_dict[get_img_arr_key()])
                    axarr[1].imshow(potentially_pass_to_face[i][get_img_arr_key()])
                    plt.title(f'frame {obj_dict_frame} and {oth_frame}: {num_matches} matches')
                    plt.show()
                if obj_dict[get_od_prob_key()] > potentially_pass_to_face[i][get_od_prob_key()]:
                    # Replace the old representation with the new one
                    potentially_pass_to_face[i] = obj_dict
                    return
                else: # The previous representation is a better one. We can stop iterating.
                    return
    # If we've reaching this point, that means this is a completely new image, so we need to add it to our list
    potentially_pass_to_face.append(obj_dict)


def add_detection(frame, pass_to_face, person_dict):
    added = person_dict.get('added', False)
    if not added:
        person_dict['frame'] = frame
        person_dict['added'] = True
        pass_to_face.append(person_dict)


def add_descriptor_to_dict(orb, person_dict):
    if 'desc' not in person_dict:
        _, desc = orb.detectAndCompute(person_dict[get_img_arr_key()], None)
        person_dict['desc'] = desc


def get_matches(desc1, desc2):
    if desc1 is None or desc2 is None:
        return 0
    bf = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
    num_matches = len(bf.match(desc1, desc2))
    return num_matches


# Local debugging
# with open('output_frames_dict.pkl', 'rb') as handle:
#     output_frames_dict = pickle.load(handle)
#     select_persons_for_recognition(output_frames_dict)