"""
Unit test for scene classification.

This self-contained test does not depend on Apollo in any way.

How to run:
  cd ..; python3 -m unittest; cd -
"""

import os
import unittest
import pathlib

if os.getenv ("JENKINS"):
    import vgg16Places365_main
else:
    import docker


class Scene365Test(unittest.TestCase):
    client = None
    pid = None
    dockerTag = None
    currentPath = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))

    def setUp(self):
        self.pid = os.getpid()

        # Skip if ran inside Docker container:
        if os.getenv ("JENKINS"):
           return
       
        self.client = docker.from_env()
        self.dockerTag = f"delme_{self.pid}"
        
        print ("setUp: building docker image.  Please wait...", flush=True)
        parent_path = self.currentPath / ".."

        # Build the docker image:
        os.environ["DOCKER_BUILDKIT"] = "1"
        self.dockerImg = self.client.images.build(
            path = str(parent_path), dockerfile="Dockerfile")[0]
        self.dockerImg.tag (self.dockerTag)
        print ("imageId = ", self.dockerImg.short_id)
        
        # Start the container:
        self.container = self.client.containers.run (
            self.dockerTag, "sleep infinity", detach=True)

    def tearDown(self):
        # Skip if ran inside Docker container:
        if os.getenv ("JENKINS"):
           return
       
        print ("tearDown: deleting docker image", flush=True)
        self.container.kill()
        self.container.stop()
        self.container.remove()
        self.client.images.remove (self.dockerTag)

    @unittest.skipIf(os.getenv('JENKINS'), "this tests test the accuracy of the model")
    def test_decode(self):
        print ("Testing decode.")

        if os.getenv ("JENKINS"): # inside Docker container already
            audioPath = self.currentPath / "images" / "6.jpg"
            outTxtPath = self.currentPath / f"out{self.pid}.txt"
            if outTxtPath.is_file():
                outTxtPath.unlink()
            
            vgg16Places365_main.run (audioPath, outTxtPath)
            self.assertTrue("food_court" in outTxtPath.read_text())
        else:
            retExec = self.container.exec_run (
                "./vgg16Places365_main.py tests/images/6.jpg /dev/stdout")
            print ("outStr =", retExec.output.decode("utf-8"))
            self.assertTrue(retExec.exit_code == 0)
            self.assertTrue("food_court" in retExec.output.decode("utf-8"))
    

    
