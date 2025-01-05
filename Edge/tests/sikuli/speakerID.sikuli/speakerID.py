#!/usr/bin/jython
#
# Speaker Recognition UI Testing 
#
# How to run:
#
#   ./sikulixide-2.0.4.jar -r speakerID.sikuli/speakerID.py 2> /dev/null
#
#   Or, open jython and:
#      jython speakerID.py
#
# 2020-08-17 (Mon) 
#
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

#
# One-time Settings
#
g_screen = Screen()               # for typing text
g_imagePath = ImagePath()
g_settings = Settings()
g_imagePath.setBundlePath (".")
# g_settings.ObserveScanRate = 10.0 # fast respond
# g_settings.WaitScanRate = 10.0    #  ..


def ConnectDevice ():
    #
    # Bring up scrcpy:
    #
    myApp = App("SM-G960U1")
    if not myApp.running:
        myApp.open("scrcpy")

    while not myApp.focus():
        myApp.focus()

    print "myApp.focus() = ", myApp.focus()
    scrcpyWindow = myApp.focusedWindow()
    assert (myApp.focus())
    scrcpyWindow.highlight (1.0)

    return scrcpyWindow


def Login (scrcpyWindow):
    """After this function, we are inside Android.

    Return True if success.
    """
    #
    # Look for Login screen
    #
    ## check if already
    allBlack_pat = Pattern ("all_black.png").similar(0.9)
    if not scrcpyWindow.has (allBlack_pat):
        print "Not currently locked"
        return True
    
    scrcpyWindow.rightClick (scrcpyWindow.getCenter())
    bottomRegion = scrcpyWindow.get(scrcpyWindow.SOUTH)
    
    try:
        # we only need to search for the bottom portion of the image
        bottomRegion.wait("little_phone_icon.png", 1.0) # wait at most 1 second
    except:
         # raise Exception('Can not find login screen')
        print 'Can not find login screen?'
        return False

    # Do a swipe motion to get password entry:
    borderSpace = 20            # +/- 20 --> avoid too close to the border
    leftCoord = (scrcpyWindow.getTopLeft().getX() + borderSpace, 
                 scrcpyWindow.getCenter().getY())
    rightCoord = (scrcpyWindow.getTopRight().getX() - borderSpace, 
                  scrcpyWindow.getCenter().getY())
    scrcpyWindow.hover (Location(*leftCoord))
    scrcpyWindow.mouseDown (Button.LEFT)
    relRightDist = rightCoord[0] - leftCoord[0]
    scrcpyWindow.mouseMove (relRightDist, 0) # horizontal move
    scrcpyWindow.mouseUp (Button.LEFT)       # default speed = 0.5

    # Enter password
    try:
        scrcpyWindow.wait ("enter_password.png", 1.0) # wait at most 1 second
    except:
        raise Exception('Can find password prompt')
    g_screen.type("apollo\n")
    return True
    

def CloseAll (scrcpyWindow):
    """After this function, we don't have any apps open.
    """
    bottomRegion = scrcpyWindow.get(scrcpyWindow.SOUTH)
    midRegion = scrcpyWindow.get(scrcpyWindow.MID_HORIZONTAL)
    
    try:
        bottomRegion.wait ("background_tasks_icon.png", 1.0)
    except:
        try:
            bottomRegion.wait ("background_tasks_icon_v2.png", 1.0)
        except:
            raise Exception('Can find background task icon!')
            
    bottomRegion.click()
    background_btn = bottomRegion.lastMatch

    if midRegion.has ("no_recently_used_apps.png", 1.0): # at most wait for 1 second
        print "No recently used app"
        return

    #
    # Make sure we get "Close all"
    #
    print "Need to clear all recently used app"
    if not bottomRegion.has ("close_all.png", 1.0): # at most wait for 1 second
        background_btn.click () # bring up the background task again
    assert (bottomRegion.has ("close_all.png", 1.0))
    bottomRegion.click ()

    # wait for close_all to vinish
    assert (bottomRegion.waitVanish ("close_all.png", 3.0))
    
    
    
def GoHomeScreen (scrcpyWindow):
    """After this function, we are at home screen"""
    bottomRegion = scrcpyWindow.get(scrcpyWindow.SOUTH)
    bottomRegion.wait ("home_screen_icon.png", 1.0) # at most wait for 1 sec
    bottomRegion.click ()


def LaunchATAK (scrcpyWindow):
    """After this function, ATAK is running.
    """
    bottomRegion = scrcpyWindow.get(scrcpyWindow.SOUTH)

    # check to see if ATAK is already running
    retVal = os.system ('adb shell "dumpsys activity | grep atakmap" > /dev/null')
    if retVal == 0:
        # bring atak to front
        os.system ("adb shell am start -n com.atakmap.app/.ATAKActivity")
        waitForAnimationStop (scrcpyWindow)
        # scrcpyWindow.rightClick (scrcpyWindow.getCenter()) # exit any plugin
        return
    
    print "Starting ATAK"
    # bottomRegion.wait ("auto_map_selection_on.png", g_settings.FOREVER)
    bottomRegion.wait ("auto_map_selection_on.png", 60.0)
    assert (bottomRegion.waitVanish ("auto_map_selection_on.png", 30.0))


def StartApolloPlugin (scrcpyWindow):
    """After this function, the apollo plugin menu will be shown.
    Pre-condition: ATAK is running.
    Post-Condition: Apollo Plugin Menu is shown.
    """
    bottomRegion = scrcpyWindow.get(scrcpyWindow.SOUTH)
    topRegion = scrcpyWindow.get(scrcpyWindow.NORTH)
    midRegion = scrcpyWindow.get (scrcpyWindow.MID_HORIZONTAL)

    ## Check to see if Apollo plug-in is already running.
    if midRegion.hasText ("Apollo Edge App"):
        print "Apollo plugin already activated"
    else:
        scrcpyWindow.rightClick (scrcpyWindow.getCenter()) # exit any plugin
        topRegion.wait ("atak_menu.png", 1.0)
        topRegion.click()

        ## scroll down the menu:
        matchPt1 = bottomRegion.wait ("elevation_tools.png", 1.0)
        matchPt2 = topRegion.find ("contacts.png")

        matchPt1.hover()
        scrcpyWindow.mouseDown (Button.LEFT)
        scrcpyWindow.mouseMove (matchPt2)
        scrcpyWindow.mouseUp (Button.LEFT)       # default speed = 0.5

        ## click on apollo plugin:
        bottomRegion.find ("apollo_edge_menuitem.png") # 
        bottomRegion.click()

    ## Wait for menu to show up
    # midRegion.wait ("apollo_edge_app.png", 1.0)
    matched = midRegion.waitText ("Apollo Edge App", 2.0) # save matched loation
    
    ## srcoll to top of menu
    while not (bottomRegion.hasText ("speech")):
        g_screen.type(Key.UP)
        scrcpyWindow.wait (0.1)
    matched.click()               # get rid of highlight


def waitForAnimationStop (region):
    for idx in range (10):
        img = g_screen.capture (region)
        imgFile = img.getFile("/dev/shm/")
        region.wait (0.3)       # allow animation to make difference in appearance.
        try:
            match = region.find (imgFile)
            similarity = match.getScore()
            print "similarity = ", similarity
            os.unlink (imgFile)
            if similarity >= 0.99:
                region.wait (0.3) # extra delay just in case animation is still on-going
                return
        except:
            print "Not found? animation  .. ", idx
            # print "imgFile = ", imgFile
            # print "region = ", region
            os.unlink (imgFile)
        
    os.unlink (imgFile)
    raise Exception('Animation going on too long?')


def selectFileItem (region, itemText):
    """
    Addresses the animation issue.
    """
    waitForAnimationStop (region)
    region.waitText (itemText, 1.0)
    region.click()
    

def SelectAudioFile (scrcpyWindow, fileTitle):
    """Select a particular audio file from ApolloDataSet.
    
    Pre-condition: We are on file selection screen.

    """
    midRegion = scrcpyWindow.get (scrcpyWindow.MID_HORIZONTAL)
    topRegion = scrcpyWindow.get(scrcpyWindow.NORTH)
    
    assert (topRegion.find ("file_selecttion_setting_icon.png"))
    waitForAnimationStop (topRegion)

    # Try to see if the audio file is already shown
    if scrcpyWindow.hasText (fileTitle):
        print "Has %s" % fileTitle
        ## Bug, doesn't work:
        # matched = scrcpyWindow.getLastMatch()
        # matched.click()
        scrcpyWindow.findText (fileTitle)
        scrcpyWindow.click()
        return
    
    #
    # Goto main menu
    topRegion.find ("file_selection_menu_icon.png")
    topRegion.click ()

    #
    # Open phone folder
    midRegion.wait ("open_from_phone_icon.png", 3.0)
    midRegion.click()

    #
    # Change to list view
    listView_pat = Pattern ("list_view_icon.png").similar(0.9)
    thumbView_pat = Pattern ("thumbnail_view_icon.png").similar(0.9)
    topRegion.wait ("file_selecttion_setting_icon.png", 3.0)
    try:
        topRegion.find (thumbView_pat)
        topRegion.click()
        print "Change to thumbnail view"
    except:
        pass
    topRegion.wait (listView_pat, 1.0)
        
    #
    # Go inside ApolloData/spkrID_eval
    selectFileItem (scrcpyWindow, "ApolloData")
    selectFileItem (scrcpyWindow, "spkrID")
    
    
    #
    # change to list view to see files better
    try:
        topRegion.find (listView_pat)
        topRegion.click()
        print "Change to list view"
    except:
        pass
    topRegion.wait (thumbView_pat, 1.0)

    #
    # Select requested audio file
    selectFileItem (scrcpyWindow, fileTitle)
    
    
def TestSpeakerID (scrcpyWindow, speakerName, expectedScore):
    """
    pre-condition:  Apollo ATAK Plugin is shown
    post-condition:  Apollo ATAK Plugin is shown
    """
    bottomRegion = scrcpyWindow.get(scrcpyWindow.SOUTH)
    topRegion = scrcpyWindow.get(scrcpyWindow.NORTH)
    midRegion = scrcpyWindow.get (scrcpyWindow.MID_HORIZONTAL)

    ## Where to click to start the speaker ID app:
    matchStart = bottomRegion.findText ("Speaker Recognition")

    ## Where to click to exit the speaker ID app:
    matchExit = bottomRegion.find ("back_icon.png")
    
    def checkBackEnd ():
        ## Check to see BackEnd Status is READY
        backendReady = False
        try:
            topRegion.find ("READY.png")
            backendReady = True
        except:
            pass
        return backendReady

    ## Restart the speaker ID app until Backend is ready:
    matchStart.click()
    topRegion.wait ("backend_status.png", 10.0)
    backendReady = False
    for idx in range(5):
        if not checkBackEnd():
            matchExit.click()
            topRegion.wait (0.2)
            matchStart.click()
            topRegion.wait ("backend_status.png", 10.0)
        else:
            backendReady = True
            break
        print "Backend NOT READY, try again later."
        
    if not backendReady:
        print "Backend not ready after 5 tries"
        raise Exception("Backend not ready after 5 tries")
        return False
    
    print "Backend READY"

    ## Select an audio
    topRegion.click ("speakerID_audio_file_btn.png")
    topRegion.wait ("file_selecttion_setting_icon.png", 10.0)
    SelectAudioFile (scrcpyWindow, speakerName)

    ## process the audio
    topRegion.wait ("speakerID_process_btn.png", 3.0)
    waitForAnimationStop (topRegion) # might have animation
    topRegion.find ("speakerID_process_btn.png")
    topRegion.click ()
    
    ## get score and check it with expectedScore
    topRegion.wait ("speakerID_score_bar.png", 5.0) # makesure similar score has been drawn already
    
    ret = topRegion.wait ("speakerID_similarity_score.png", 3.0) # make it low matching similarity
    region = Region (ret.getImage().lastSeen)
    outText = region.text()
    print "speaker score text is ", outText
    scoreText = outText.split()[-1] # text should be like u'Similarity Score:\n \n73'
    assert (int(scoreText) == expectedScore)

    ## we are done.
    topRegion.click ("speakerID_ok_btn.png")
    topRegion.wait ("backend_status.png", 10.0)

    ## exit spearkID app
    bottomRegion.click ("back_icon_v2.png")
    midRegion.waitText ("Apollo Edge App", 1.0)
    
    
    
if __name__ == "__main__":
    pass

    #
    # 1.) connect and try to login
    #
    scrcpyWindow = ConnectDevice ()
    Login (scrcpyWindow)
    
    #
    # 2.) close all apps
    #
    # CloseAll (scrcpyWindow)

    #
    # 3.) Get to home screen
    #
    # GoHomeScreen (scrcpyWindow)

    #
    # 4.) Launch or Bring up ATAK
    #
    LaunchATAK (scrcpyWindow)

    #
    # 5.) Start Apollo Plugin
    #
    StartApolloPlugin (scrcpyWindow)

    #
    # 6.) Start Speaker ID
    #
    # TestSpeakerID (scrcpyWindow, "McGregor", 73)
    TestSpeakerID (scrcpyWindow, "Muniz", 55)


    print "DONE!"
