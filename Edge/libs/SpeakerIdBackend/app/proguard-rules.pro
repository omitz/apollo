
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jventura/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# -keepclassmembers enum * {*;}
# -keepclassmembers class com.caci.apollo.speaker_id_library.SVC$Classifier {
#      <fields>;
# }
## See speaker-id-library/consumer-rules.pro  

-addconfigurationdebugging 
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
