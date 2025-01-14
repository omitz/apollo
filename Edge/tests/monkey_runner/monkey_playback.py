#!/usr/bin/env monkeyrunner
# Copyright 2010, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import subprocess
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice, MonkeyImage

# sets a variable with the package's internal name
package = 'com.yourPackage'

# sets a variable with the name of an Activity in the package
activity = '.MainActivity'

# sets the name of the component to start
runComponent = package + '/' + activity

#vars for use in screen comparison later
REF = 'reference.png'
SCR = 'screenshot.png'
CMP = 'comparison.png'

#the acceptance threshold for image comparison
ACCEPTANCE = 0.9

# The format of the file we are parsing is very carfeully constructed.
# Each line corresponds to a single command.  The line is split into 2
# parts with a | character.  Text to the left of the pipe denotes
# which command to run.  The text to the right of the pipe is a python
# dictionary (it can be evaled into existence) that specifies the
# arguments for the command.  In most cases, this directly maps to the
# keyword argument dictionary that could be passed to the underlying
# command. 

# Lookup table to map command strings to functions that implement that
# command.
CMD_MAP = {
    'TOUCH': lambda dev, arg: dev.touch(**arg),
    'DRAG': lambda dev, arg: dev.drag(**arg),
    'PRESS': lambda dev, arg: dev.press(**arg),
    'TYPE': lambda dev, arg: dev.type(**arg),
    'WAIT': lambda dev, arg: MonkeyRunner.sleep(**arg)
    }

# Process a single file for the specified device.
def process_file(fp, device):
    for line in fp:
        (cmd, rest) = line.split('|')
        try:
            # Parse the pydict
            rest = eval(rest)
        except:
            print 'unable to parse options'
            continue

        if cmd not in CMD_MAP:
            print 'unknown command: ' + cmd
            continue

        print cmd
        CMD_MAP[cmd](device, rest)


def main():
    file = sys.argv[1]
    fp = open(file, 'r')

    device = MonkeyRunner.waitForConnection()
	
    # # Runs the component
    # device.startActivity(component=runComponent)
	
    process_file(fp, device)
	
    # result = device.takeSnapshot()
    # result.writeToFile('shot2.png','png')
    # reference = MonkeyImage.loadFromFile('reference.png')
    # if not screenshot.sameAs(reference, ACCEPTANCE):
    #    print "comparison failed, getting visual comparison..."
    #    subprocess.call(["/usr/bin/compare", REF, SCR, CMP])
    # fp.close()
    

if __name__ == '__main__':
    main()



