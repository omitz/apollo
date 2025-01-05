# Sikuli

https://github.com/RaiMan/SikuliX1

## Installation (Tested on Ubuntu 18.04)

### Sikuli IDE:
1.) Download Sikuli IDE (sikulixide-2.0.4.jar or newer version):

https://launchpad.net/sikuli/sikulix/2.0.4/+download/sikulixide-2.0.4.jar

### Sikuli API:
2.) Download Sikuli java API (sikulixapi-2.0.4.jar or newer version):

https://launchpad.net/sikuli/sikulix/2.0.4/+download/sikulixapi-2.0.4.jar

3.) Install jython and other system packages
```
apt-get install jython wmctrl xdotool
```

### Sikuli OCR and OpenCV installation (for Ubuntu 18.04):

#### OCR
```bash
sudo add-apt-repository ppa:alex-p/tesseract-ocr
sudo apt-get update
sudo apt install tesseract-ocr
sudo apt install libtesseract-dev
sudo ldconfig
```

ref: https://github.com/RaiMan/SikuliX1/wiki/macOS-Linux:-Support-libraries-for-Tess4J-Tesseract-4-OCR

#### OpenCV
```bash
sudo apt install libopencv3.2-java
sudo ln -s /usr/lib/jni/libopencv_java320.so /usr/lib/libopencv_java.so
```

ref: https://sikulix-2014.readthedocs.io/en/latest/newslinux.html

## Running Sikuli IDE
Assuming `sikulixide-2.0.4.jar` is located in your home directory:

```bash
chmod a+x ~/sikulixide-2.0.4.jar
~/sikulixide-2.0.4.jar
```


## Loading Sikuli libraries into Jython
Assuming `sikulixapi-2.0.4.jar` is located in your home directory:

```python
#!/usr/bin/jython
import os
import sys

homeDir=os.getenv ("HOME")
sys.path.append (homeDir + "/sikulixapi-2.0.4.jar")
import org.sikuli
try:
    from org.sikuli.basics import * # A bug in Jypthon?
except:
    pass
from org.sikuli.basics import *
from org.sikuli.script import *

```

## Sikuli Documentation
https://sikulix-2014.readthedocs.io/en/latest/toc.html
