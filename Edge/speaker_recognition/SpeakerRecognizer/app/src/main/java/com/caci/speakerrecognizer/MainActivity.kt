package com.caci.speakerrecognizer

import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import androidx.core.app.ActivityCompat.finishAffinity
import kotlin.system.exitProcess
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.*
import java.io.File


// Hard-coded file paths:
val g_termuxHomeFull = "/data/data/com.termux/files/home/"
val g_speakerIdDirFull = g_termuxHomeFull + "projects/speakerID/"
val g_readyFile = "spkrSvrRdy.pid"
val g_readyFileFull = g_speakerIdDirFull + g_readyFile


class WebServerObserver(path: String, actvity : MainActivity) : FileObserver(path, CREATE) {
    val activity = actvity

    override fun onEvent(event: Int, path: String?) {
        Log.d("test", "calling onEvent")
        // finishAffinity()
        // Log.d("test", "event dectect " + event + " " + path);
        // exitProcess(-1)
        Log.d("test", "path is " + path);
        if (path == g_readyFile) {
            Log.d("test", "web server ready");
            activity.loadUrl ()
        }
    }
}


class MainActivity : AppCompatActivity() {
    lateinit var webServerFileObs : WebServerObserver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView.settings.javaScriptEnabled = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        //        webView.settings.setDefaultFontSize (10)
        //        webView.settings.setDefaultFixedFontSize (10)
        webView.settings.textZoom = 95
        //        webView.loadUrl("file:///android_asset/overview.html");

        // We watch for the Termux web server to get ready.
        webServerFileObs = WebServerObserver (g_speakerIdDirFull, this@MainActivity)
        webServerFileObs.startWatching ()
        
	// We ssh into Termux to start the remi web server
        val readyFile = File (g_readyFileFull)
        if (readyFile.exists()) {
            readyFile.delete()}
        val cmdStr :String = (".shortcuts/tasks/vgg-sre-demo.sh " +
                " --readyFile " + g_readyFileFull +
                " --browserless")
        val tmuxCmdStr : String = ("tmux wait-for -L my3\\; send-keys -t spkrID '" + cmdStr +
                "; tmux wait-for -U my3;tmux wait-for -U my3' ENTER\\; wait-for -L my3")
        Log.v("testing", tmuxCmdStr)
        SshTask().execute(*arrayOf(tmuxCmdStr))
    }


    fun loadUrl () {
        // The function is called by another thread to actually load the web page.
        webView.post { // webView will be handled by the main GUI thread
            webView.loadUrl("http://127.0.0.1:8080/")
        }
        webServerFileObs.stopWatching () // searver already running, can stop watching now.
    }
    
    
//    /** Called when the user taps the Send button */
//    fun startGUI(view: View) {
//        SshTask().execute()
//    }

//    override fun onStop() {
//        super.onStop()
//        Log.d("test", "on stop")
//    }


//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d("test", "on destory")
//        SshTask().execute(*arrayOf("~/projects/kill-remi-gui.bash"))
//    }

//    () {
//        super.onStop()
//        SshTask().execute(*arrayOf("~/projects/kill-remi-gui.bash"))
//    }

}

class SshTask : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg p0: String): String {
        print (p0[0])
        val output = executeRemoteCommand("demo", "apollo",
                                          "localhost", p0[0])
        print(output)
            return output
    }
}

fun executeRemoteCommand(username: String,
                         password: String,
                         hostname: String,
                         command: String,
                         port: Int = 8022): String {
    val jsch = JSch()
    val session = jsch.getSession(username, hostname, port)
    session.setPassword(password)

    // Avoid asking for key confirmation.
    val properties = Properties()
    properties.put("StrictHostKeyChecking", "no")
    session.setConfig(properties)

    session.connect()

    // Create SSH Channel.
    val sshChannel = session.openChannel("exec") as ChannelExec
    val outputStream = ByteArrayOutputStream()
    sshChannel.outputStream = outputStream

    // Execute command.
    sshChannel.setCommand (command)
    sshChannel.connect()

    // Sleep needed in order to wait long enough to get result back.
    Thread.sleep(1_000)
    sshChannel.disconnect()

    session.disconnect()

    return outputStream.toString()
}
