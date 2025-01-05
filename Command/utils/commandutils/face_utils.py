import os


def get_threshold_color_map():
    return {'low': {'min': 0,
                    'color': 'blue'},
            'medium': {'min': 0.4,
                       'color': 'yellow'},
            'high': {'min': 0.75,
                     'color': 'green'}}


def list_filepaths(dir, person_list):
    filepaths = []
    for person in person_list:
        path = os.path.join(dir, person)
        files = os.listdir(path)
        for file in files:
            filepaths.append(os.path.join(path, file))
    return filepaths