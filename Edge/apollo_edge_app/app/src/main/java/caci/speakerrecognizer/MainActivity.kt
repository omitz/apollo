package caci.speakerrecognizer

import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.os.FileObserver
import kotlinx.android.synthetic.main.speaker_activity_main.*
import pp.facerecognizer.R
import java.io.File

// Hard-coded file paths:
val g_termuxHomeFull = "/data/data/com.termux/files/home/"
val g_speakerIdDirFull = g_termuxHomeFull + "projects/speakerID/"
val g_readyFile = "spkrSvrRdy.pid"
val g_readyFileFull = g_speakerIdDirFull + g_readyFile


class WebServerObserver(path: String, actvity : MainActivity) : FileObserver(path, CREATE) {
    val activity = actvity

    override fun onEvent(event: Int, path: String?) {
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
        setContentView(R.layout.speaker_activity_main)

        webView.settings.javaScriptEnabled = true;
        webView.settings.setSupportZoom(true);
        webView.settings.setBuiltInZoomControls(true);
        //        webView.settings.setDefaultFontSize (10)
        //        webView.settings.setDefaultFixedFontSize (10)
        webView.settings.setTextZoom (95)


        val bundle :Bundle? = intent.extras;
        var cmdStr :String;
        cmdStr = (".shortcuts/tasks/vgg-sre-demo.sh " +
                " --readyFile " + g_readyFileFull + " --browserless")

        // Check for intend
        if (bundle!=null && bundle.getBoolean("isScanned")) {
            val inputPathQ:String? = bundle.getString("filePath")
            if (inputPathQ != null){
                val inputPath :String  = inputPathQ.toString()
                cmdStr = (".shortcuts/tasks/vgg-sre-demo.sh" +
                          " --readyFile " + g_readyFileFull +
                          " --audio " + inputPath + " --browserless")
            }
            else {
                Log.d("test!!!!", "Why is it Null?");
                return;
            }
        }


        // We watch for the Termux web server to get ready.
        webServerFileObs = WebServerObserver (g_speakerIdDirFull, this@MainActivity)
        webServerFileObs.startWatching ()

        
	// We ssh into Termux to start the remi web server
        val readyFile = File (g_readyFileFull)
        if (readyFile.exists()) {
            readyFile.delete() }
        // send command to tmux session
        val tmuxCmdStr : String = ("tmux wait-for -L my3\\; send-keys -t spkrID '" + cmdStr +
                "; tmux wait-for -U my3;tmux wait-for -U my3' ENTER\\; wait-for -L my3")
        SshTask().execute(*arrayOf(tmuxCmdStr))
    }

    fun loadUrl () {
        // The function is called by another thread to actually load the web page.
        webView.post { // webView will be handled by the main GUI thread
            webView.loadUrl("http://127.0.0.1:8080/")
        }
        webServerFileObs.stopWatching () // searver already running, can stop watching now.
    }
    
   override fun onStop() {
       super.onStop()
       Log.d("test!!!", "Stop Speaker ID")
       // We want to stop any audio that is currently being played
   }


   // override fun onPause() {
   //     super.onPause()
   //     Log.d("test!!!", "Pause Speaker ID")
   // }
   

//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d("test", "on destory")
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

    Log.d("SSH_COMMAND", command);
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
