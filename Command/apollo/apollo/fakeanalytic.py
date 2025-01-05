from .analytic import Analytic
from .models import DetectedObj

class FakeAnalytic(Analytic):
        """Minimal implementation of analytic class for testing purposes"""
        
        def run():
            return {"analytic": "response"}
        
        def get_closest_results(self, file_name, num_results=10):
            obj1 = DetectedObj()
            obj2 = DetectedObj()
            return [obj1, obj2]

        def cleanup(self):
            pass