package com.caci.termuxbackendinstaller
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import android.content.Context
import android.os.Environment
import android.content.res.AssetManager
import java.io.*
import android.content.pm.PackageManager

import android.widget.Button
import android.widget.Toast
import android.content.pm.PackageInstaller
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.content.IntentSender
import android.app.PendingIntent
import android.os.FileObserver
import androidx.core.content.FileProvider
import android.app.ActivityManager




val SCRIPTS_DIR : String = "installerScripts/"
val installerDirPath : String = "/data/data/com.termux/" + SCRIPTS_DIR
val successFileName : String = "installerSuceeded.txt"
val successFilePath : String = installerDirPath + successFileName
val failFileName: String = "installerFailed.txt"
val failFilePath: String = installerDirPath + failFileName

// EDIT here: Add more future backend installers
val assetScriptFileNames =  arrayOf (
    SCRIPTS_DIR + "installBase.bash",
    SCRIPTS_DIR + "installSpeakerID.bash",
    SCRIPTS_DIR + "installAntiVirus.bash",
    SCRIPTS_DIR + "trimFS.bash"
)

// EDIT here: Add more future backends files:  "ApolloData/" is always copied.
val assetFolders =  arrayOf (
    arrayOf ("termux/dot_termux", "/data/data/com.termux/files/home/.termux"),
    arrayOf ("termux/projects_AnLinux_debian_debian-fs_root/dot_termux",
             "/data/data/com.termux/files/home/projects/AnLinux/debian/debian-fs/root/.termux"),
    arrayOf ("SpeakerID/projects", "/data/data/com.termux/files/home/projects"),
    arrayOf ("SpeakerID/dot_termux", "/data/data/com.termux/files/home/.termux"),
    arrayOf ("SpeakerID/dot_shortcuts",  "/data/data/com.termux/files/home/.shortcuts")
)


fun isAppInstalled(context: Context, packageName: String): Boolean {
    try {
        context.packageManager.getApplicationInfo(packageName, 0)
        return true
    } catch (e: PackageManager.NameNotFoundException) {
        return false
    }
}



fun copyAssetToDir(mgr: AssetManager, srcFilename: String,
                   destDirpath: String, skipLevels : Int = 0) {
    val truncated :String = srcFilename.splitToSequence("/").drop(skipLevels).joinToString("/")
    val afile = mgr.open(srcFilename)
    val bfile = destDirpath + "/" + truncated
    Log.v("installer srcFilename", srcFilename)
    Log.v("installer bfile", bfile)
    
    var inStream: InputStream? = null
    var outStream: OutputStream? = null

    inStream = afile
    outStream = FileOutputStream(bfile)

    val buffer = ByteArray(1024)
    var length = inStream.read(buffer)
    while (length > 0 )
    {
        outStream.write(buffer, 0, length)
        length = inStream.read(buffer)
    }

    inStream.close()
    outStream.close()

    if (bfile.endsWith (".py") || bfile.endsWith (".bash") ||
        bfile.endsWith (".sh") || bfile.endsWith (".so"))
        ensureFileReadableAndExecutable (File(bfile))
}


fun copyAssetToFile(mgr: AssetManager, srcFilename: String, destFilepath: String) {
    val afile = mgr.open(srcFilename)
    val bfile = destFilepath
    Log.v("installer srcFilename", srcFilename)
    Log.v("installer bfile", bfile)
    
    var inStream: InputStream? = null
    var outStream: OutputStream? = null

    inStream = afile
    outStream = FileOutputStream(bfile)

    val buffer = ByteArray(1024)
    var length = inStream.read(buffer)
    while (length > 0 )
    {
        outStream.write(buffer, 0, length)
        length = inStream.read(buffer)
    }

    inStream.close()
    outStream.close()

    if (bfile.endsWith (".py") || bfile.endsWith (".bash") ||
        bfile.endsWith (".sh") || bfile.endsWith (".so"))
        ensureFileReadableAndExecutable (File(bfile))
}


private fun ensureFileReadableAndExecutable(file: File) {
    if (!file.canRead()) file.setReadable(true)
    if (!file.canExecute()) file.setExecutable(true)
}



class InstallerObserver(path: String, _actvity : MainActivity) : FileObserver(path, CREATE) {
    val myactivity = _actvity

    override fun onEvent(event: Int, path: String?) {
        Log.d("test", "calling onEvent")
        Log.d("test", "path is " + path);
        if (path == successFileName) {
            Log.d("test", "successFileName");
            myactivity.webView.post { // webView will be handled by the main GUI thread
                                      myactivity.webView.loadUrl("file:///android_asset/step2.html")
            }
            myactivity.installerFileObs.stopWatching ()
        }
        if (path == failFileName) {
            Log.d("test", "failFileName");
            myactivity.webView.post { // webView will be handled by the main GUI thread
                                      myactivity.webView.loadUrl("file:///android_asset/fail.html")
            }
            myactivity.installerFileObs.stopWatching ()
        }
    }
}


class MainActivity : AppCompatActivity() {
    val REQUEST_INSTALL_TERMUX = 0
    val REQUEST_INSTALL_TERMUX_BOOT = 1
    val REQUEST_INSTALL_TERMUX_API = 2
    val REQUEST_INSTALL_TERMUX_WIDGET = 3
    val REQUEST_UNINSTALL_TERMUX = 10
    val REQUEST_UNINSTALL_TERMUX_BOOT = 11
    val REQUEST_UNINSTALL_TERMUX_API = 12
    val REQUEST_UNINSTALL_TERMUX_WIDGET = 13
    
    lateinit var installerFileObs : InstallerObserver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        /*
         * Start from a clean state
         */
        var file = File(successFilePath)
        if (file.exists()) {
            file.delete ()
        }
        file = File(failFilePath)
        if (file.exists()) {
            file.delete ()
        }

        /*
         * Setup buttons.
         */
        SetupTermuxButton ()
        SetupAptButton ()
        SetupBackEndButton ()

        val quitdBtn = findViewById(R.id.quitBtn) as Button
        quitBtn.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked me.", Toast.LENGTH_SHORT).show()
            finish();
            System.exit(0);
            // finishAffinity()
        }

        /*
         * Setup Webview.
         */
        webView.settings.setLoadsImagesAutomatically(true);
        webView.settings.javaScriptEnabled = true;
        webView.settings.setSupportZoom(true);
        webView.settings.setBuiltInZoomControls(true);
        webView.settings.setDefaultFontSize (10)
        webView.settings.setDefaultFixedFontSize (10)
        webView.clearCache(true)

        webView.loadUrl("file:///android_asset/step1.html");

        Log.v("installer", "leave onCreate ")
    }

    fun SetupTermuxButton () {
        val termuxBtn = findViewById(R.id.termuxBtn) as Button
        termuxBtn.setOnClickListener {
            installApk ("termux/apks/termux-apollo.apk", REQUEST_INSTALL_TERMUX) //get started
        }


        val uninstallBtn = findViewById(R.id.uninstallBtn) as Button
        uninstallBtn.setOnClickListener {
            if (isAppInstalled (applicationContext, "com.termux")) {
                uninstallApp ("package:com.termux", REQUEST_UNINSTALL_TERMUX)
            } else if (isAppInstalled (applicationContext, "com.termux.boot")) {
                uninstallApp ("package:com.termux.boot", REQUEST_UNINSTALL_TERMUX_BOOT)
            } else if (isAppInstalled (applicationContext, "com.termux.api")) {
                uninstallApp ("package:com.termux.api", REQUEST_UNINSTALL_TERMUX_API)
            } else if (isAppInstalled (applicationContext, "com.termux.widget")) {
                uninstallApp ("package:com.termux.widget", REQUEST_UNINSTALL_TERMUX_WIDGET)
            }
        }
        
    }



    fun SetupAptButton () {
        val aptBtn = findViewById(R.id.aptBtn) as Button
        aptBtn.setOnClickListener {
            if (! (isAppInstalled (applicationContext, "com.termux") &&
                   isAppInstalled (applicationContext, "com.termux.boot") &&
                   isAppInstalled (applicationContext, "com.termux.api"))) {
                // aptBtn.isEnabled = false
                // backEndBtn.isEnabled = false
                webView.loadUrl("file:///android_asset/depNotMet.html")
                return@setOnClickListener
            }

            // Log.v("installer", "killing com.termux")        
            // val am = getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
            // am.killBackgroundProcesses ("com.termux")

            // start installing all scripts in another thread.
            executeInstallerScripts (assetScriptFileNames)

            // We watch for Apt install to finish.
            installerFileObs = InstallerObserver (installerDirPath, this@MainActivity)
            installerFileObs.startWatching ()            
        }
    }

    fun SetupBackEndButton () {
        val backEndBtn = findViewById(R.id.backEndBtn) as Button
        backEndBtn.setOnClickListener {
            if (! (isAppInstalled (applicationContext, "com.termux") &&
                   isAppInstalled (applicationContext, "com.termux.boot") &&
                   isAppInstalled (applicationContext, "com.termux.api"))) {
                // aptBtn.isEnabled = false
                // backEndBtn.isEnabled = false
                webView.loadUrl("file:///android_asset/depNotMet.html");
                return@setOnClickListener
            }
            Toast.makeText(this@MainActivity, "Please Wait...", Toast.LENGTH_SHORT).show()

            // Install back-end projects.  
            for (assetFolder in assetFolders) {
                syncAssetsToTermux (assetFolder[0], assetFolder[1])
            }
            
            // Insall media and data files.
            copyAssetsToStorage ("ApolloData") // to /storage/emulated/0/
            
            Toast.makeText(this@MainActivity, "Done", Toast.LENGTH_SHORT).show()
            var successFile = File(successFilePath)
            if (successFile.exists()) {
                webView.loadUrl("file:///android_asset/success.html");
            }
        }
    }
    
    
    
    private fun executeInstallerScripts (assetScriptFileNames : Array<String>) {
        // We can just add more installer scripts to the array
        val targetScriptTopDir : String = "/data/data/com.termux/"
        val file = File(targetScriptTopDir  + SCRIPTS_DIR)
        if (! file.exists()) {
            file.mkdirs ()
        }
    
        for (scriptFileName in assetScriptFileNames) {
            copyAssetToDir (assets, scriptFileName, targetScriptTopDir)
        }

        executeScripts_helper (applicationContext, targetScriptTopDir, assetScriptFileNames)
    }


    private fun getApkUri(assetName: String): Uri {
        /**
         * Before N, a MODE_WORLD_READABLE file could be passed via the ACTION_INSTALL_PACKAGE
         * [Intent]. Since N, MODE_WORLD_READABLE files are forbidden, and a [FileProvider] is
         * recommended.
         */
        val useFileProvider = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        /**
         * Copy the given asset out into a file so that it can be installed.
         * Returns the path to the file.
         */
        val tempFilename = "_tmp_.apk"
        val file = File(this.filesDir, tempFilename)
        if (file.exists()){
            file.delete ()}
        
        val buffer = ByteArray(16384)
        val fileMode = if (useFileProvider) Context.MODE_PRIVATE else Context.MODE_WORLD_READABLE
        try {
            assets.open(assetName).use { inputStream ->
                openFileOutput(tempFilename, fileMode).use { fout ->
                    var n: Int
                    while (inputStream.read(buffer).also { n = it } >= 0) {
                        fout.write(buffer, 0, n)
                    }
                }
            }
        } catch (e: IOException) {
            Log.i("test", "Failed to write temporary APK file", e)
        }
        return if (useFileProvider) {
            val toInstall = File(this.filesDir, tempFilename)
            FileProvider.getUriForFile(
                    this, "com.caci.termuxbackendinstaller", toInstall)
        } else {
            Uri.fromFile(getFileStreamPath(tempFilename))
        }
    }


    fun uninstallApp (appName : String, requestCode : Int) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE)
        intent.data = Uri.parse(appName)
        // startActivity(intent)
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        startActivityForResult(intent, requestCode)
    }
    
    fun installApk (assetPath : String, requestCode : Int) {
        val intent = Intent (Intent.ACTION_INSTALL_PACKAGE)
        intent.data = getApkUri (assetPath)
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        // startActivity (intent)
        // intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        startActivityForResult(intent, requestCode);
    }


    override fun onActivityResult (requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_UNINSTALL_TERMUX -> {
                if (isAppInstalled (applicationContext, "com.termux.boot"))
                    uninstallApp ("package:com.termux.boot", REQUEST_UNINSTALL_TERMUX_BOOT)
            }
            REQUEST_UNINSTALL_TERMUX_BOOT -> {
                if (isAppInstalled (applicationContext, "com.termux.api"))
                    uninstallApp ("package:com.termux.api", REQUEST_UNINSTALL_TERMUX_API)
            }
            REQUEST_UNINSTALL_TERMUX_API -> {
                if (isAppInstalled (applicationContext, "com.termux.widget"))
                    uninstallApp ("package:com.termux.widget", REQUEST_UNINSTALL_TERMUX_WIDGET)
            }
            REQUEST_UNINSTALL_TERMUX_WIDGET -> {
            }
            REQUEST_INSTALL_TERMUX -> {
                installApk ("termux/apks/termux-boot-apollo.apk", REQUEST_INSTALL_TERMUX_BOOT)
            }
            REQUEST_INSTALL_TERMUX_BOOT -> {
                installApk ("termux/apks/termux-api-apollo.apk", REQUEST_INSTALL_TERMUX_API)
            }
            REQUEST_INSTALL_TERMUX_API -> {
                installApk ("termux/apks/termux-widget-apollo.apk", REQUEST_INSTALL_TERMUX_WIDGET)
            }
            REQUEST_INSTALL_TERMUX_WIDGET -> {
                // make sure we kill termux before we call the back-end installer
                amKillProcess ("com.termux") 
            }
        }
    }
    
    fun amKillProcess (package_name : String) {
        val am = getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = am.runningAppProcesses

        for (runningProcess in runningProcesses) {
            Log.v ("test", "processName " + runningProcess.processName)
            if (runningProcess.processName == package_name) {
                android.os.Process.killProcess(runningProcess.pid)
                android.os.Process.sendSignal(runningProcess.pid, android.os.Process.SIGNAL_KILL)
                am.killBackgroundProcesses(runningProcess.processName);
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        Log.v("installer", "calling onStart")        
    }


    override fun onResume() {
        super.onResume()
        Log.v("installer", "calling onResume")
        Log.v("installer", "exit onResume")
    }

    override fun onStop() {
        super.onStop()
        Log.v("installer", "calling onStop")
    }

    override fun finish() {
        super.finishAndRemoveTask();
    }


    
    private fun executeScripts_helper (context : Context, scriptDir : String,
                                       assetScriptFileNames : Array<String>) {
        /*
         * Create a combined installer script:
         */
        val combinedScriptPath : String = scriptDir + SCRIPTS_DIR + "installerAll.bash"
        copyAssetToFile (assets, SCRIPTS_DIR + "installerBoilerPlate.bash", combinedScriptPath)

        var file = File(combinedScriptPath)
        file.appendText ("successFilePath=$successFilePath\n" +
                         "failFilePath=$failFilePath\n" +
                         "rm -f \$failFilePath\n" +
                         "rm -f \$successFilePath\n")

        var scriptList : String = ""
        for (scriptFileName in assetScriptFileNames) {
            scriptList += scriptDir + "/" + scriptFileName + "\n"
        }
        file.appendText(scriptList)

        file.appendText ("echo \$\$ > \$successFilePath\n")
        file.appendText ("date >> \$successFilePath\n")
        
        
        /*
         * Execute the combined installer script:
         */
        val TERMUX_SERVICE = "com.termux.app.TermuxService"
        val ACTION_EXECUTE = "com.termux.service_execute"
        val EXTRA_EXECUTE_IN_BACKGROUND = "com.termux.execute.background"
        Log.v("installer", "executeScript " + combinedScriptPath)
        
        val scriptUri = Uri.Builder().scheme("com.termux.file").path(combinedScriptPath).build()
        val executeIntent = Intent(ACTION_EXECUTE, scriptUri)
        executeIntent.setClassName("com.termux", TERMUX_SERVICE)
        executeIntent.putExtra(EXTRA_EXECUTE_IN_BACKGROUND, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // https://developer.android.com/about/versions/oreo/background.html
            context.startForegroundService(executeIntent)
        } else {
            context.startService(executeIntent)
        }
    }
        
    private fun copyAssetsToStorage (folderName : String) {
        val workDir : String = Environment.getExternalStorageDirectory().getPath() 
        copyAssetFileOrDir (workDir, folderName)
    }


    private fun syncAssetsToTermux (folderName : String, targetDir : String) {
        assert (! folderName.endsWith ("/"))
        assert (! targetDir.endsWith ("/"))
        val skipLevels = folderName.split("/").count() 
        Log.v("skipLevels", skipLevels.toString())
        copyAssetFileOrDir (targetDir, folderName, skipLevels)
    }

    
    private fun copyAssetFileOrDir(TARGET_BASE_PATH: String,
                                   path: String, skipLevels : Int = 0) {
        val assetManager = assets
        var myassets: Array<String>? = null
        try {
            Log.v("tag", "copyFileOrDir() $path")
            myassets = assetManager.list(path)
            if (myassets!!.size == 0) {
                copyAssetToDir (assetManager, path, TARGET_BASE_PATH, skipLevels)
            }
            else {
                val truncated :String = path.splitToSequence("/").drop(skipLevels).joinToString("/")
                val fullPath = "$TARGET_BASE_PATH/$truncated"
                Log.v("tag", "outpath=$fullPath")
                val dir = File(fullPath)
                dir.mkdirs()
                for (i in myassets.indices) {
                    val p: String
                    if (path == "") {
                        p = ""
                    }
                    else {
                        p = "$path/"
                    }
                    copyAssetFileOrDir(TARGET_BASE_PATH, p + myassets[i], skipLevels)
                }
            }
        } catch (ex: IOException) {
            Log.e("tag", "I/O Exception", ex)
        }
    }
}
