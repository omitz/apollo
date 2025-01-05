from unittest import TestCase
from apollo.models import DetectedObj, VideoDetections, DetectedFace, Landmark, ClassifyScene


class TestModels(TestCase):

    def test_keys(self):
        '''
        The UI uses certain keys (eg 'id' or 'path') to identify each search result element as unique.
        This test is just to serve as a reminder that if we change the key in the flask processor, we need to change it in the UI as well.
        '''
        for model in [VideoDetections]:
            model_instance = model()
            serialized = model_instance.serialize()
            ui_msg = f'Update UI with new key for {model}.'
            self.assertIn('path', serialized, ui_msg)
        for model in [DetectedFace, Landmark, ClassifyScene]:
            model_instance = model()
            serialized = model_instance.serialize()
            ui_msg = f'Update UI with new key for {model}.'
            self.assertIn('id', serialized, ui_msg)
            self.assertIn('path', serialized, ui_msg)
        for model in [DetectedObj]:
            model_instance = model()
            model_instance.bb_ymin_xmin_ymax_xmax = []
            model_instance.detection_scores = []
            serialized = model_instance.serialize()
            ui_msg = f'Update UI with new key for {model}.'
            self.assertIn('id', serialized, ui_msg)
            self.assertIn('path', serialized, ui_msg)