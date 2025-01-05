## Text Localization and OCR

### Setup

Open OCR in Android Studio.

You may need to import the .aar and javadoc:

    Go to File>New>New Module
    Select "Import .JAR/.AAR Package" and click next.
    Enter the path to .aar file and click finish.
    Go to File>Project Structure (Ctrl+Shift+Alt+S).
    Under "Modules," in left menu, select "app."
    Go to "Dependencies tab.
    Click the green "+" in the upper right corner.
    Select "Module Dependency"
    Select the new module from the list.

You might need to increase the min SDK (The Tesseract4Android minimum is 16)

    File > Project Structure
    Click "Modules" in the left menu
    Click "app" in the second to left menu
    Click "Default Config" tab
    In Min SDK Version dropdown, select 16

#### Setup background

This project contains the releases (.aar and javadoc.jar) from https://github.com/adaptech-cz/Tesseract4Android/releases/tag/v2.0.0

### Orientation issue

In some cases, the bitmap passed to the OCR algorithm might be rotated, which breaks the OCR functionality. Pay attention to the preview of the image displayed. If the preview is rotated, the results will be nonsense.

Potential solutions for the above scenario:

* Correct the Exif orientation: The exif orienation isn't always accurate.
* Get the orientation from MediaStore: Attempts have thrown errors (e.g. database has no columns)
* Use Camera or Camera2 API instead of intent: Time-consuming to implement. 

### Resources

https://github.com/adaptech-cz/Tesseract4Android
https://stackoverflow.com/questions/29826717/how-to-import-a-aar-file-into-android-studio-1-1-0-and-use-it-in-my-code/38749847
https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=3&cad=rja&uact=8&ved=2ahUKEwi5ltPtsZzlAhXDrVkKHUWeB_oQFjACegQIDBAI&url=http%3A%2F%2Fwww.apnatutorials.com%2Fandroid%2Fhow-to-change-api-level-in-android-studio.php%3FcategoryId%3D2%26subCategoryId%3D59%26myPath%3Dandroid%2Fhow-to-change-api-level-in-android-studio.php&usg=AOvVaw1deAh7ft6o7tBxOnUJcjs1
https://stackoverflow.com/questions/20478765/how-to-get-the-correct-orientation-of-the-image-selected-from-the-default-image




