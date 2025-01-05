# Run using the Sikuli 2 IDE. https://sikulix-2014.readthedocs.io/en/latest/index.html
from sikuli import *

secs = 2.0
# Define region as entire screen (1912x1080 is the screen resolution)
screen_region = Region(0,0,1912,1080)
if not screen_region.has("imgs/mozilla_firefox.png", secs):
    click("imgs/firefox_icon.png")
if not screen_region.has("imgs/command.png", secs):
    click("imgs/apollo_icon.png")
click("imgs/refresh.png")
screen_region.wait(2.0)

def check_analytic(text_field, input_text, expected_result, secs=2.0):
   try:
       click(text_field)
   except:
       type(Key.PAGE_DOWN)
       click(text_field)
   type(input_text)
   type(Key.ENTER)
   assert(screen_region.has(expected_result, secs))
   click(expected_result)


def check_upload_analytic(field, upload_img, search_button, expected_result, secs=10.0):
   try:
       click(field)
   except:
       type(Key.PAGE_DOWN)
       click(field)
   click(upload_img)
   type(Key.ENTER)
   # Click search
   # Get all search buttons
   search_btns = list(screen_region.findAll(search_button))
   # Figure out which one is the best match
   best_btn = search_btns[0]
   cur_best = best_btn.getScore()
   for btn in search_btns[1:]:
       print('cur best: {}'.format(cur_best))
       if btn.getScore() > best_btn.getScore():
           print('replacing')
           best_btn = btn
   click(best_btn)
   try:
       assert(screen_region.has(expected_result, secs))
   except:
       type(Key.PAGE_UP)
       assert(screen_region.has(expected_result, secs))
   click(expected_result)
   

def check_expected_large(expected_large, secs=2.0):
    assert(screen_region.has(expected_large, secs))
    # Wait for the video still to render and thus for the close button to reach its final position
    screen_region.wait(secs)
    click("imgs/close.png")

type(Key.PAGE_UP)
check_analytic("imgs/enter_object_tag.png", "bear", "imgs/expected_bear.png")
check_expected_large("imgs/bear_large.png")
check_analytic("imgs/object_in_video.png", "person", "imgs/play_video.png")
check_expected_large("imgs/seconds.png", 4.0)
check_upload_analytic("imgs/upload_face_field.png", "imgs/keira1.png", "imgs/face_search_button.png", "imgs/keira.png")
check_expected_large("imgs/keira.png")
check_upload_analytic("imgs/landmark_field.png", "imgs/all_souls_college_png.png", "imgs/landmark_search_button.png", "imgs/all_souls.png", 60.0)
check_expected_large("imgs/all_souls.png")
check_analytic("imgs/scene_field.png", "food court", "imgs/food_court_result.png", secs=9.0)
check_expected_large("imgs/fc_result_large.png")
check_analytic("imgs/entity_field.png", "Hezbollah", "imgs/lebanons_txt.png", secs=4.0)
check_analytic("imgs/audio_file_or_img_field.png", "nuclear", "imgs/play_audio.png")
check_expected_large("imgs/seconds.png")

