import pathlib, os, sys

def add_parent_to_syspath():
    '''
    Add the analytic's root to sys path so that imports in match_images don't fail.
    Currently we're not using this function, but it may be useful in the future if we create a script with which we can run all analytics' tests from apollo/Command.
    '''
    this_file = pathlib.Path(os.path.dirname(os.path.abspath(__file__)))
    landmark = this_file.parents[0]
    sys.path.insert(0, str(landmark))