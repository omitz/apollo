"""
Unit test for OCR.

This self-contained test does not depend on Apollo in any way.

How to run:
  cd ..; python3 -m unittest; cd -

"""

import os
import unittest
import pathlib

if os.getenv ("JENKINS"):
    import easyOcr_main
else:
    import docker

@unittest.skipIf(os.path.isfile('/.dockerenv'), "Skip this test if it's being run via `docker-compose run ocr-easy python -m unittest`")
class OcrTest(unittest.TestCase):
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
        print ("Testing ocr decode.")
        if os.getenv ("JENKINS"): # inside Docker container already
            imagePath = self.currentPath / "images" / "ocr.png"
            outTxtPath = self.currentPath / f"out{self.pid}.txt"
            outImgPath = self.currentPath / f"out{self.pid}.png"
            if outTxtPath.is_file():
                outTxtPath.unlink()

            succeed = easyOcr_main.run (imagePath, outTxtPath, outImgPath)
            self.assertTrue(succeed == True)
            self.assertTrue("brown fox" in outTxtPath.read_text())
        else:
            retExec = self.container.exec_run (
                "./easyOcr_main.py tests/images/ocr.png " + 
                "/dev/stdout out.png debug.json")
            print ("outStr =", retExec.output.decode("utf-8"))
            self.assertTrue(retExec.exit_code == 0)
            self.assertTrue("brown fox" in retExec.output.decode("utf-8"))
    

