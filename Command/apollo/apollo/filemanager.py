import os
import shutil

class FileManager():
    def __init__(self):
        self.ram_storage = '/dev/shm'
        self.local_outdir = os.path.join(self.ram_storage, 'outdir')

        if not os.path.exists(self.local_outdir):
            os.mkdir(self.local_outdir)

    def cleanup(self):
        if os.path.exists(self.local_outdir):
            shutil.rmtree(self.local_outdir)