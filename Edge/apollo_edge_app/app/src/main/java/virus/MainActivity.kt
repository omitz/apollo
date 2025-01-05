package virus

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import java.io.ByteArrayOutputStream
import java.util.*
import android.app.Activity
import android.net.Uri
import android.os.Environment
import java.io.IOException
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import apollo_utils.ApolloFileUtils
import kotlinx.coroutines.runBlocking
import org.KaldiActivity
import pp.facerecognizer.MainActivity
import pp.facerecognizer.R
import java.io.File
import java.io.FileOutputStream


const val R_CODE = 9630 //arbitrary request code to allow for file search

class MainActivity : AppCompatActivity() {

    private val INPUT = "/input.ok"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.virus_activity_main)
        performFileSearch(R_CODE) // open Android's file explorer for user.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var isInfected: Boolean = false
        var inputPath : String = ""
        runBlocking {
            val (_isInfected, _inputPath) = scanFile(requestCode, resultCode, data) // scan user-selected file
            isInfected = _isInfected
            inputPath = _inputPath
        }
        //Log.v("INFECTED_ONACTIVITY", isInfected.toString())
        if (!isInfected) {
            val t = Toast.makeText(this, "Scan complete - File Not Infected!", Toast.LENGTH_LONG)
            addButtons(data, inputPath)
            t.show()
        }
    }

    private fun addButtons(data: Intent?, inputPath : String){

            val layout = findViewById<LinearLayout>(R.id.layout)
            val button = Button(this) // define button and set attributes
            button.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            val button2 = Button(this) // define button and set attributes
            button2.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)

            val inputUri: Uri? = data!!.data
            val uriString = inputUri.toString()
            val ext = ApolloFileUtils.extensionFromFullpath(uriString)
            val imageExts = listOf("png", "jpg", "jpeg")
            val audioExts = listOf("wav", "mp3")

            if (uriString.contains("image") or (ext in imageExts)) { // address input file path with according extension
                button.text = getString(R.string.face)
                button2.text = getString(R.string.ocr)
                button.setOnClickListener {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("isScanned", true)
                    intent.putExtra("uri", uriString)
                    startActivity(intent)
                }
                button2.setOnClickListener {
                    val intent = Intent(this, ocr.MainActivity::class.java)
                    intent.putExtra("isScanned", true)
                    intent.putExtra("uri", uriString)
                    startActivity(intent)
                }
                layout.addView(button) // add Button to LinearLayout
                layout.addView(button2) // add Button2 to LinearLayout
            }
            else if((uriString.contains("audio")) or (ext in audioExts)) { // see if it starts the same

                button.text = getString(R.string.speech)
                button2.text = getString(R.string.speaker)
                button.setOnClickListener {
                    val intent = Intent(this, KaldiActivity::class.java)
                    intent.putExtra("isScanned", true)
                    intent.putExtra("filePath", inputPath)
                    startActivity(intent)
                }
                button2.setOnClickListener {
                    val intent = Intent(this, caci.speakerrecognizer.MainActivity::class.java)
                    intent.putExtra("isScanned", true)
                    intent.putExtra("filePath", inputPath)
                    startActivity(intent)
                }
                layout.addView(button) // add Button to LinearLayout
                layout.addView(button2) // add Button2 to LinearLayout
            }
            else {
                val t = Toast.makeText(this, "Please try again and input an Audio or Image File.", Toast.LENGTH_LONG)
                t.show()
            }

    }

    private suspend fun scanFile(requestCode: Int, resultCode: Int, data: Intent?): Pair<Boolean, String> {

        var infected: Boolean = true // assume infected unless "OK", assume infected if null.
        var inputPath: String = ""

        if (requestCode === R_CODE && resultCode === Activity.RESULT_OK) { // weird warnings

            var inputUri: Uri? = null
            val workDir = File(Environment.getExternalStorageDirectory().path + "/work")
            if (!workDir.exists()) {
                workDir.mkdir()
            }
            inputPath = workDir.path //filesDir.path // provide a basis for which to save input.
            val outputPath = workDir.path + "/output.txt" // output
            val outputDonePath = workDir.path + "/output.txt.done" // output

            // Start from a clean state.  Remove output from previous run
            var myFile : File
            myFile = File(outputPath)
            if (myFile.exists())
               myFile.delete()
            myFile = File(outputDonePath)
            if (myFile.exists())
               myFile.delete()


            if (data != null) {
                inputUri = data.data // if we have intent, determine uri
                var uriString = inputUri.toString()

                inputPath += INPUT

                //Log.v("IN", inputPath)

                val inp = contentResolver.openInputStream(inputUri!!) // open the uri as input
                val out = FileOutputStream(File(inputPath)) // open new controllable path as output
                val buf = ByteArray(1024)
                var len: Int
                len = inp!!.read(buf)
                while (len > 0) {
                    out.write(buf, 0, len)
                    len = inp.read(buf)
                }
                out.close()
                inp.close()
                
                
                try {
                    // ssh into Termux to run clamd SCAN: read from new input file, write to output
                            var cmdstr = "clamdscan " + inputPath + " > " + outputPath
                            cmdstr += "; touch " + outputDonePath
                    SshTask().execute(*arrayOf(cmdstr))
                    
                        // add a sleep for one or two seconds
                //    Log.v("COMPLETE", "scan is complete")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.v("COMPLETE", "NO scan is complete")
                }

                // Wait for output file to be ready
                myFile = File(outputDonePath)
                while (! myFile.exists()) {
                    Log.v("OUT", "Waiting for outputDone flag")
                    Thread.sleep(250)
                }

                File(outputPath).forEachLine {
                    if(it.contains("OK"))
                        infected = false
                }

                //Log.v("INFECTED", infected.toString())

            }
        }
        return Pair (infected, inputPath)
    }

    fun performFileSearch(requestCode: Int) {

        // create an intent with which will start an activity
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "*/*"
        val types = arrayOf("image/*", "audio/*")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, types)
        startActivityForResult(intent, requestCode)

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
