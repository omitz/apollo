import os, shutil

class FileManager():
    def __init__(self):
        self.ram_storage = '/dev/shm'
        if not os.path.exists(self.ram_storage):
            os.mkdir(self.ram_storage)

        self.local_outdir = os.path.join(self.ram_storage, 'outdir')
        if not os.path.exists(self.local_outdir):
            os.mkdir(self.local_outdir)

    def cleanup(self):
        shutil.rmtree(self.local_outdir)