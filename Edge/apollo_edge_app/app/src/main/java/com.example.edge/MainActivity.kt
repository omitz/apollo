package com.example.edge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import org.KaldiActivity
import pp.facerecognizer.MainActivity
import pp.facerecognizer.R
import java.io.File
import java.io.FileOutputStream
import apollo_utils.PermissionsUtils
import upload.UploadMainActivity

import android.os.Environment
import apollo_utils.ApolloFileUtils;
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T


class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST = 1
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!PermissionsUtils.hasPermission(this, permissions)) {
            requestPermissions(permissions, PERMISSIONS_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (!(grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(this.permissions, PERMISSIONS_REQUEST)
            }
        }
    }

    fun startUpload(view: View) {
        val intent = Intent(this, upload.UploadMainActivity::class.java)
        intent.putExtra("isScanned", false)
        startActivity(intent)
    }

    fun startSpeech(view: View) {
        val intent = Intent(this, KaldiActivity::class.java)
        intent.putExtra("isScanned", false)
        startActivity(intent)
    }

    fun startSpeaker(view: View) {
        val intent = Intent(this, caci.speakerrecognizer.MainActivity::class.java)
        intent.putExtra("isScanned", false)
        startActivity(intent)
    }

    fun startPerson(view: View) {
        val intent = Intent(this, pp.facerecognizer.MainActivity::class.java)
        intent.putExtra("isScanned", false)
        startActivity(intent)
    }

    fun startText(view: View) {
        val intent = Intent(this, ocr.MainActivity::class.java)
        intent.putExtra("isScanned", false)
        startActivity(intent)
    }

    // Uncomment after CDR
//    fun startObject(view: View) {
//        //val intent = Intent(this, ObjectDetectionActivity::class.java)
//        //intent.putExtra("isScanned", false)
//        //startActivity(intent)
//    }

    fun startVirusScan(view: View) {
        val intent = Intent(this, virus.MainActivity::class.java)
        startActivity(intent)
    }
}

