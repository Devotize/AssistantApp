package com.sychev.assistantapp.presentation.activity.camera_activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.sychev.assistantapp.R
import java.io.File
import java.io.IOException

class CameraPhotoActivity : AppCompatActivity() {
    private val CAMERA_PERM_CODE = 101
    private val CAMERA_CAPTURE = 100

    /*private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val file = File(currentPhotoPath)
                imageView.scaleType = ImageView.ScaleType.FIT_XY
                imageView.setImageURI(Uri.fromFile(file))
            }
        }*/

    lateinit var imageView: ImageView
    lateinit var buttonBack: ImageButton
    lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_camera)
        imageView = findViewById(R.id.displayImageView)
        buttonBack = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish()
        }

        if (checkCameraHardware(this)) {
            askCameraPermissions()
        } else {
            Toast.makeText(this, "No camera hardware available !", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun askCameraPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERM_CODE
            )
        } else {
            dispatchTakePictureIntent()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERM_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    dispatchTakePictureIntent()
                } else {
                    Toast.makeText(
                        this,
                        "Camera Permission is Required to use camera !",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }
    }


    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = "CameraPhoto"//SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "PhotoFile == null !", Toast.LENGTH_LONG).show()
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.sychev.android.fileProvider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    //startForResult.launch(takePictureIntent)
                    startActivityForResult(takePictureIntent, CAMERA_CAPTURE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == CAMERA_CAPTURE) {
            val file = File(currentPhotoPath)
            imageView.scaleType = ImageView.ScaleType.FIT_XY
            imageView.setImageURI(Uri.fromFile(file))
        }
    }
}