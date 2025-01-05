"""
Unit test for ASR (Automatic Speech Recogntion).  

This self-contained test does not depend on Apollo in any way.

How to run:
  cd ..; python3 -m unittest; cd -

"""

import os
import unittest
import pathlib

if os.getenv ("INSIDE_DOCKER") or os.getenv ("JENKINS"):
    import vosk_main
else:
    import docker

class AsrTest(unittest.TestCase):
    client = None
    pid = None
    dockerTag = None
    currentPath = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))

    @unittest.skipIf(os.getenv('JENKINS'), "already have docker image built")
    def setUp(self):
        self.pid = os.getpid()

        # Skip if ran inside Docker container:
        if os.getenv ("INSIDE_DOCKER"):
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

    @unittest.skipIf(os.getenv('JENKINS'), "already have docker image built")
    def tearDown(self):
        # Skip if ran inside Docker container:
        if os.getenv ("INSIDE_DOCKER"):
           return
       
        print ("tearDown: deleting docker image", flush=True)
        self.container.kill()
        self.container.stop()
        self.container.remove()
        self.client.images.remove (self.dockerTag)

    @unittest.skipIf(os.getenv('JENKINS'), "this tests test the accuracy of the model")
    def test_decode(self):
        print ("Testing decode.")

        def runTest ():
            if os.getenv ("INSIDE_DOCKER"): # inside Docker container already
                audioPath = self.currentPath / "audios" / "indian.mp3"
                outTxtPath = self.currentPath / f"out{self.pid}.txt"
                outMetadataPath = self.currentPath / f"out{self.pid}_metadata.json"
                if outTxtPath.is_file():
                    outTxtPath.unlink()
                if outMetadataPath.is_file():
                    outMetadataPath.unlink()
            
                succeed = vosk_main.run (audioPath, outTxtPath, outMetadataPath)
                self.assertTrue(succeed == True)
                text = outTxtPath.read_text() 
                self.assertTrue("not an indian" in text)
                metadata = outMetadataPath.read_text() 
                print ("metadata = ", metadata)
                print ("""testing '[1.0, 1.86, 1.41, "knew"]'""")
                self.assertTrue('[1.0, 1.86, 1.41, "knew"]' in metadata)
            else:
                audioPath = pathlib.Path("tests") / "audios" / "indian.mp3"
                outMetadataPath = pathlib.Path("tests") / f"out{self.pid}_metadata.json"
                if outMetadataPath.is_file():
                    outMetadataPath.unlink()
                    
                print (f"./vosk_main.py {audioPath} /dev/stdout {outMetadataPath}", flush=True)
                retExec = self.container.exec_run (
                    f"./vosk_main.py {audioPath} /dev/stdout {outMetadataPath}")
                print ("outStr =", retExec.output.decode("utf-8"), flush=True)
                self.assertTrue(retExec.exit_code == 0)
                self.assertTrue("not an indian" in retExec.output.decode("utf-8"))

                retExec = self.container.exec_run (f"cat {outMetadataPath}")
                self.assertTrue(retExec.exit_code == 0)
                print ("metadata = ", metadata)
                print ("""testing '[1.0, 1.86, 1.41, "knew"]'""")
                self.assertTrue('[1.0, 1.86, 1.41, "knew"]' in retExec.output.decode("utf-8"))
                
        runTest()
        runTest()    # run again to make sure timestamp does not shift

    

