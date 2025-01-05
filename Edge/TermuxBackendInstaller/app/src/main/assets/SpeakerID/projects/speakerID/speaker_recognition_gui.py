#!/bin/env python3
""" 
To start the vgg tensorflow tmux, do:
Run example:
  

TC 2020-02-11 (Tue) -- Added tmux workaround for Termux-API
"""
import argparse, sys
import textwrap
from shutil import copyfile

import remi.gui as gui
# from remi import start, App
from remi import Server, App
import threading
import os
import signal
import time
import platform

import logging
import watchdog.observers
import watchdog.events 

logging.basicConfig(level=logging.INFO,
                    format='%(asctime)s - %(message)s',
                    datefmt='%Y-%m-%d %H:%M:%S')

## Directories:
g_termuxHomeFull = "/data/data/com.termux/files/home/"
g_speakerIdDirFull = g_termuxHomeFull + "projects/speakerID/"
g_VggDirDebian = "/root/Projects/vggvox-speaker-identification/"
g_debianVggDirFull = (g_termuxHomeFull +
                      "/projects/AnLinux/debian/debian-fs/" + g_VggDirDebian)

## Audio process input/output file locations:
g_inWAVName = "inAudio.wav"
g_saveAudioFileNameFull = g_debianVggDirFull + g_inWAVName
g_saveAudioFileNameDebian = g_VggDirDebian + g_inWAVName
g_outCSVName = "outSpeakerID.csv"
g_outSpeakerFileNameFull = g_debianVggDirFull + g_outCSVName
g_outSpeakerFileNameDebian = g_VggDirDebian + g_outCSVName

## done files:  poor man's method to glue software together
g_outDoneName = "doneID.pid"
g_outDoneFileNameFull = g_debianVggDirFull + g_outDoneName
g_localOutDoneFileName = g_VggDirDebian + g_outDoneName
g_vggReadyFileName = "vggTmux.pid"
g_vggReadyFileNameFull = g_debianVggDirFull + g_vggReadyFileName

g_closeAndExitName = "exit.pid" # hard-coded in .shortcuts/tasks/vgg-sre-demo.sh
g_closeAndExitNameFull = g_debianVggDirFull + g_closeAndExitName

## Commands:
g_sendAudioCmd = (g_speakerIdDirFull + "send-audio-to-vgg.bash %s %s %s" %
                  (g_saveAudioFileNameDebian, g_outSpeakerFileNameDebian,
                   g_localOutDoneFileName))
g_firstTime_flg = True

def tmux (cmd):
    retCodeFile = g_speakerIdDirFull + "tmuxRet_" + str(os.getpid()) + ".out"
    
    # tmuxCmd = (("tmux send-keys -t spkrID " +
    #            "'%s; echo $? > %s; tmux wait-for -S done' " +
    #            "ENTER\; wait-for done") % (cmd, retCodeFile))

    tmuxCmd = (("tmux wait-for -L my3\; send-keys -t spkrID " +
                "'%s; echo $? > %s; tmux wait-for -U my3; " + 
                "tmux wait-for -U my3' " +
                "Enter\; wait-for -L my3") % (cmd, retCodeFile))


    
    retVal = os.system (tmuxCmd)
    assert (retVal == 0)      # always return 0 doesn't matter tmuxCmd

    ## We need to wait for retCodeFile?
    fid = open (retCodeFile, "r")
    retVal = int(fid.read())
    fid.close()
    os.remove (retCodeFile)
    return retVal           


def CreateSymlinkToIntendAudio (intendAudioPath):
    """
    We assume that intendAudioPath is can be accessible from within debian.  
    This means, "/storage/emulated/0/..."
    """
    if (os.path.isfile (g_saveAudioFileNameFull) or
        os.path.islink (g_saveAudioFileNameFull)):
        os.remove (g_saveAudioFileNameFull)
    os.symlink (intendAudioPath, g_saveAudioFileNameFull)


counter = 0
def idle (self):
    """Do to termux not returning status, we need to pull the status
    periodically.
    """
    # global counter
    # print ("IDLE wasting cpu ", counter); counter+=1
    # Hack for now: if server is gone, there is no point to continue
    if len (self.websockets) == 0:
        print ("websockets gone")
        tmux ("termux-media-player stop") # make stop playing
        self.close()
    self.showRecrodingStatus ()

def enableIdle ():
    if MyApp.idle == App.idle:
        MyApp.idle = idle

def disableIdle ():
    if MyApp.idle != App.idle:
        del MyApp.idle


class MyApp(App):
    current_audioSelRec_status = None
    audio_source = None
    current_page = None
    init_called = False
    saveAudioCount = 0

    def __new__(cls,*args,**kwargs):
        # print("From new")
        # print("cls = ", cls)
        # print("args = ", args)
        # print("kwargs= ", kwargs)
        # create our object and return it
        obj = super().__new__(cls)
        # super(MyApp, obj).__init__(*args)
        # obj.fake__init__(*args)

        global g_firstTime_flg
        if g_firstTime_flg:
            if (os.path.isfile (g_saveAudioFileNameFull) or
                os.path.islink (g_saveAudioFileNameFull)):
                os.remove (g_saveAudioFileNameFull)
            if os.path.isfile (g_outSpeakerFileNameFull):
                os.remove (g_outSpeakerFileNameFull)
            if os.path.isfile (g_outDoneFileNameFull):
                os.remove (g_outDoneFileNameFull)
            g_firstTime_flg = False
        
        return obj
            
    def __init__(self, *args):
        """
        init may be called again when switching page.
        """
        assert (self.init_called == False)

        print ("CALLING __init__()")
        self.audio_size = 0
        self.current_audioSelRec_status = None
        self.audio_source = None

        self.init_called = True

        res_path = os.path.join(
            os.path.dirname(os.path.abspath(__file__)), 'res')
        print ("res_path = ", res_path)
        super(MyApp, self).__init__(
            *args, static_file_path={'my_resources':res_path})
            

    def removeAudio (self, msg):
        tmux ("termux-media-player stop") # make sure audio is not playing
        if (os.path.isfile (g_saveAudioFileNameFull) or
            os.path.islink (g_saveAudioFileNameFull)):
            os.remove (g_saveAudioFileNameFull)
        if os.path.isfile (g_outSpeakerFileNameFull):
            os.remove (g_outSpeakerFileNameFull)
        if os.path.isfile (g_outDoneFileNameFull):
            os.remove (g_outDoneFileNameFull)
            
        self.audio_size = 0
        self.current_audioSelRec_status = None
        self.audio_source = None
        self.currentAudioLbl.set_text ('Current Audio:%s' %
                                       self.current_audioSelRec_status)
        self.audioOKBtn.set_enabled(False)
        self.audioPlayBtn.set_enabled(False)

            
    def create_record_audio_dialog (self):
        """ we show a recording progress..
        """
        page = gui.VBox(width=300, height=200)
        lbl = gui.Label('Recording 5-second Audio...')
        lbl.style['font-size'] = '20px'
        page.append(lbl)

        img = gui.Image('/my_resources:progress.gif', width=100)
        page.append(img)

        self.recordSizeLbl = gui.Label ("Audio File Size = 0")
        self.recordSizeLbl.style['font-size'] = '20px'
        page.append (self.recordSizeLbl)

        container = gui.VBox (margin='10px')
        container.append (page, "page")
        return container

    
    def showRecrodingStatus (self):
        assert (os.path.isfile (g_saveAudioFileNameFull))
        bytes = os.path.getsize (g_saveAudioFileNameFull)
        elaps_sec = time.time() - self.startrec_Sec
        self.recordSizeLbl.set_text ('%d/5 sec: Audio File Size = %s' %
                                     (elaps_sec-1, bytes))
        if (bytes > 0) and (bytes == self.audio_size):
            self.sameSizeCount += 1
            if self.sameSizeCount >= 5: 
                self.sameSizeCount = 0

                # We convert to wav file
                tmpOut = g_saveAudioFileNameFull + ".wav"
                if os.path.isfile (tmpOut): 
                    os.remove (tmpOut)
                cmd = ("ffmpeg -i " + g_saveAudioFileNameFull +
                       " -ar 16000 -sample_fmt s16 -ac 1 " + tmpOut)
                # ret = os.system (cmd)
                # assert (ret == 0)
                ret = tmux (cmd)
                print ("ret = ", ret)
                assert (ret == 0)
                os.rename (tmpOut, g_saveAudioFileNameFull)

                self.set_different_root_widget (None, self.page1, 1)
                self.updateInputWaveFile ()
                disableIdle ()
        else:
            self.audio_size = bytes
        
    
    def on_record_audio (self, widget):
        self.removeAudio ("on_record_audio")
        self.audio_source = "recording"

        self.sameSizeCount = 0
        self.startrec_Sec = time.time ()
        self.set_different_root_widget (None, self.recordingDialg, 1.2)

        # NOTE: remember to grand Termux-API for recording audio permission
        tmux ("termux-microphone-record -q")
        tmux ("termux-microphone-record -r 16000 -c 1 -l 5 -f %s" %
              g_saveAudioFileNameFull)
        

    def on_playback_audio (self, widget):
        tmux ("termux-media-player stop")
        tmux ("termux-media-player play %s" % g_saveAudioFileNameFull)
        
    def open_fileselection_dialog(self, widget):
        self.removeAudio ("open_fileselection_dialog")
        self.audio_source = "file"
        tmux ("termux-storage-get %s" % g_saveAudioFileNameFull)

    def CreatePage1 (self):
        #creating a container VBox type, vertical
        page = gui.VBox(width=300, height=240)

        # backend status:
        self.backendStatusLbl = gui.Label ('')
        self.backendStatusLbl.style['font-size'] = '25px'

        #a button for simple interaction
        audioFileBtn = gui.Button('Audio File', width=200, height=30)
        audioRecordingBtn = gui.Button('Record Audio', width=200, height=30)
        self.recordingDialg = self.create_record_audio_dialog ()

        self.currentAudioLbl = gui.Label ('Current Audio: %s' %
                                          self.current_audioSelRec_status)
        self.currentAudioLbl.style['font-size'] = '25px'

        
        ## a playback button
        self.audioPlayBtn = gui.Button('Playback', width=100, height=30)
        self.audioPlayBtn.set_enabled(False)

        ## a Process button
        self.audioOKBtn = gui.Button('Process', width=100, height=30)
        self.audioOKBtn.set_enabled(False)        
        
        ## package play and process button together
        audioSourceHbox = gui.HBox (margin='10px', width=300)
        audioSourceHbox.append (self.audioPlayBtn)
        audioSourceHbox.append (self.audioOKBtn)
        
        #setting up the listener for the click event
        audioRecordingBtn.onclick.do (self.on_record_audio)
        audioFileBtn.onclick.do (self.open_fileselection_dialog)
        self.audioPlayBtn.onclick.do (self.on_playback_audio)
        
        #adding the widgets to the main container
        page.append (self.backendStatusLbl)
        page.append (audioFileBtn)
        page.append (audioRecordingBtn)
        page.append (self.currentAudioLbl)
        page.append (audioSourceHbox)

        container = gui.VBox (margin='10px')
        container.append (page, "page")

        return container

    def CreatePage2 (self):
        page = gui.VBox(width=300, height=200)
        lbl = gui.Label('Processing Audio...')
        lbl.style['font-size'] = '25px'
        page.append(lbl)

        img = gui.Image('/my_resources:progress.gif', width=100)
        page.append(img)

        container = gui.VBox (margin='10px')
        container.append (page, "page")
        return container

    
    def CreatePage3 (self):
        """
        termux-notification --id 8080 --sound -t hello -c hello 
        termux-notification --ongoing --id 8080 --sound -t hello -c hello 
        termux-vibrate -f -d 250
        """
        page = gui.VBox(width=300, height=320)

        lbl = gui.Label("Speaker's voice is most similar to:", width=320)
        lbl.style['font-size'] = '25px'
        page.append(lbl)

        lbl = gui.Label('Tommy Chang')
        lbl.style['font-size'] = '25px'
        lbl.style['font-weight'] = 'bold'
        page.append(lbl)
        self.speakerLbl = lbl

        lbl = gui.Label('Similarity Score:', width=320)
        lbl.style['font-size'] = '25px'
        page.append(lbl)

        img = gui.Image (width=300)
        page.append (img)
        self.resultScoreImg = img

        okBtn = gui.Button ('OK', width=100, height=30)
        page.append(okBtn)
        self.finalOkBtn = okBtn

        # saveAudioBtn = gui.Button ('Save audio data', width=100, height=30)
        # saveAudioBtn.onclick.do (self.on_save_audio)
        # page.append(saveAudioBtn)
        
        container = gui.VBox (margin='10px')
        container.append (page, "page")
        return container


    def updateVggBackEndReady (self):
        print ("checking g_vggReadyFileNameFull", g_vggReadyFileNameFull)
        if (self.backendStatusLbl.get_text() == ''):
            if os.path.isfile (g_vggReadyFileNameFull):
                print ('  BackEnd Status: READY')
                self.backendStatusLbl.set_text ('BackEnd Status: READY')
            else:
                self.backendStatusLbl.set_text ('BackEnd Status: WAITING')


    def updateInputWaveFile (self):
        if os.path.isfile (g_saveAudioFileNameFull):
            if self.audio_source == "file":
                self.current_audioSelRec_status = "SELECTED"
            elif self.audio_source == "recording":
                self.current_audioSelRec_status = "RECORDED"
            else:
                self.current_audioSelRec_status = "ERR"
                print ("ERROR self.audio_source = ", self.audio_source)
                # assert (0)      # not suppose to be here
            self.currentAudioLbl.set_text ('Current Audio: %s' %
                                           self.current_audioSelRec_status)
            self.audioOKBtn.set_enabled(True)
            self.audioPlayBtn.set_enabled(True)

    def showResult (self):
        self.set_different_root_widget (None, self.page3, 3)
        """
        The output csv file has the format:
        speaker,minDist,maxDist
        """
        assert (os.path.isfile (g_outSpeakerFileNameFull))
        csvContent = open (g_outSpeakerFileNameFull, "r").readlines ()
        [speaker, minDist,
         maxDist, score] = (csvContent[1].strip().split(','))
        self.speakerLbl.set_text (speaker.replace("_", " "))

        scoreImgBaseName = "scores/score%s.png" % score
        print ("scoreImgBaseName = '%s'" % scoreImgBaseName)
        self.resultScoreImg.set_image (
            "/my_resources:" + scoreImgBaseName)
        

    def on_file_created (self, event):
        fileName = os.path.basename (event.src_path)
        # TODO: We should be using directory intead of file name.
        # mkdir is atomic.
        if fileName == g_vggReadyFileName:
            print ("vgg backend ready")
            self.updateVggBackEndReady ()

        if fileName == g_inWAVName:
            print ("got input wave file")
            if self.current_page == 1:
                self.updateInputWaveFile ()
            else:
                print ("self.current_page = ", self.current_page)
                assert (self.current_page == 1.2)
                enableIdle ()

        if fileName == g_outDoneName:
            print ("got done prcessing file")
            self.showResult ()

        # if fileName == g_closeAndExitName:
        #     print ("exit gracefully")
        #     self.close ()

            
    def main(self):
        if (self.init_called == False):
            return
        page1 = self.CreatePage1 ()
        page2 = self.CreatePage2 ()
        page3 = self.CreatePage3 ()
        print ("keys are:", page1.children.keys())
        self.audioOKBtn.onclick.do (self.on_Processing_audio)
        self.finalOkBtn.onclick.do (self.set_different_root_widget, page1, 1)
        self.page1 = page1
        self.page2 = page2
        self.page3 = page3
        
        #
        # Setup done file watchdog
        #
        fileHandler = watchdog.events.FileSystemEventHandler()
        fileHandler.on_created = self.on_file_created
        self.observer = watchdog.observers.Observer()
        self.observer.schedule (fileHandler, g_debianVggDirFull)
        self.observer.start ()
        self.updateVggBackEndReady ()

        #
        # check passed audio
        #
        self.current_page = 1
        if parse_args.audio:
            self.audio_source = "file"
            CreateSymlinkToIntendAudio (parse_args.audio)
            return self.on_Processing_audio (None)
        
        print ("CALLING main()")
        return page1
    

    def on_save_audio (self, emitter):
        self.saveAudioCount += 1
        (dirName, fileName) = os.path.split(g_saveAudioFileNameFull)
        copyfile (g_saveAudioFileNameFull,
                  os.path.join (dirName, "%02d_savedAudio.wav" %
                                (self.saveAudioCount,)))
        
    
    def on_Processing_audio (self, emitter):
        # assert (self.current_page == 2)   # start processing audio
        tmux ("termux-media-player stop") # make sure not playing
        # GUI may pickup left-over output
        if os.path.isfile (g_outSpeakerFileNameFull): 
            os.remove (g_outSpeakerFileNameFull) 
        if os.path.isfile (g_outDoneFileNameFull):
            os.remove (g_outDoneFileNameFull) 
        # os.system (g_sendAudioCmd)
        tmux (g_sendAudioCmd)
        print ("g_sendAudioCmd= ", g_sendAudioCmd)
        self.set_different_root_widget (None, self.page2, 2)
        return self.page2
        
    
    def set_different_root_widget (self, emitter,
                                   page_to_be_shown, current_page):
        self.current_page = current_page
        self.set_root_widget (page_to_be_shown)

    def on_close(self):
        # super(MyApp, self).on_close() # do i need this?
        print("calling on_close")

        #here you can handle the unload
        tmux ("termux-media-player stop") # make sure not playing

        print("stopping watchdog")
        self.observer.stop ()
        # self.observer.join ()
        print("app closing")
        super(MyApp, self).on_close() # do i need this?

        
if __name__ == "__main__":
    # Create a parser:
    description="""
    Speaker Recognition Demo...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("--browserless", help="don't start a new browser", 
                         action="store_true")
    parser.add_argument ("--audio", help="the audio file to use")
    parser.add_argument ("--readyFile", help="file to create after the server is running")

    # Specify Example:
    parser.epilog='''Example:
        %s 
        %s --browserless
        %s --browserless --audio test.wav --readyFile serverReady.pid
        ''' % (sys.argv[0], sys.argv[0], sys.argv[0])

    print ("argv = " + str (sys.argv))
    
    # Parse the commandline:
    try:
        parse_args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)

    # Do not have webview.
    if not parse_args.browserless:
        if platform.machine() == "x86_64":
            os.system ("am start -n org.chromium.webview_shell/"
                       ".WebViewBrowserActivity -d http://127.0.0.1:8080 "
                       "--activity-clear-task")
        else:
            os.system ("am start --user 0 "
                       "-n com.sec.android.app.sbrowser/"
                       "com.sec.android.app.sbrowser.SBrowserMainActivity "
                       "http://127.0.0.1:8080")

    tmux ("termux-media-player stop") # make sure not playing
    print ("g_closeAndExitNameFull = ", g_closeAndExitNameFull)

    # start (MyApp, address='0.0.0.0', port=8080, start=False,
    #        start_browser=False, debug=False, multiple_instance=False)
    s = Server(MyApp, address='0.0.0.0', port=8080, start=False)
    s._myid = threading.Thread.ident
    print ("here1")

    s.start()
    print ("server started")
    print ("parse_args.readyFile = ", parse_args.readyFile)
    if parse_args.readyFile:
        print ("   --> writing readyfile: " + parse_args.readyFile)
        with open (parse_args.readyFile, "a") as readyFile:
            readyFile.write (str(os.getpid()))

#    s.serve_forever()
    s._sth.join()
    print ("here3")
