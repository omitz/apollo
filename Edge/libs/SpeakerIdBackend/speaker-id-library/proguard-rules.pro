# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn org.kaldi.VoskJNI


-keep public class * {
    public protected *;
}

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,
                SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


-keepclassmembers enum * {*;}
-keepclassmembers class com.caci.apollo.speaker_id_library.SVC$Classifier {
     <fields>;
}

-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.SpeakerRecognitionModel { public protected *; }
-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.SpeakerRecognitionModel$SpeakerInfo { public protected *; }
-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.SpeakerRecognitionModel$RecognizerInfo { public protected *; }
-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.SpeakerRecognitionModel$EnrollmentMetaInfo { public protected *; }
-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.AudioDataReceivedListener { public protected *; }
-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.PlaybackListener { public protected *; }
-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.PlaybackThread { public protected *; }
-keep,allowobfuscation public class com.caci.apollo.speaker_id_library.RecordingThread { public protected *; }

-addconfigurationdebugging 
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable


