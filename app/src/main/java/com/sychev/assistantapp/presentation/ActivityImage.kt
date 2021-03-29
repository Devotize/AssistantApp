package com.sychev.assistantapp.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sychev.assistantapp.R
import com.sychev.assistantapp.ui.utils.askPermission
import com.sychev.assistantapp.ui.utils.askPermissions

class ActivityImage : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var buttonBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        imageView = findViewById(R.id.displayImageView)
        buttonBack = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener {
            finish()
        }

        when (intent.getIntExtra(ACTION_CREATE, DEFAULT)) {
            ACTION_CAMERA_CODE -> {
                cameraCapture()
            }
            ACTION_GALLERY_CODE -> {
                galleryCapture()
            }
            else -> {
                finish()
            }
        }
    }

    private fun galleryCapture() {
        askPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, GALLERY_PERM_CODE) {
            galleryPictureIntent()
        }
    }

    private fun cameraCapture() {
        if (checkCameraHardware(this)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                askPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    CAMERA_PERM_CODE
                ) {
                    cameraPictureIntent()
                }
            } else {
                askPermission(
                    this,
                    Manifest.permission.CAMERA,
                    CAMERA_PERM_CODE
                ) {
                    cameraPictureIntent()
                }
            }
        } else {
            Toast.makeText(this, "No camera hardware available !", Toast.LENGTH_SHORT).show()
            finish()
        }
    }


    private fun galleryPictureIntent() {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(gallery, ACTION_GALLERY_CODE)
        //startGalleryForResult.launch(gallery)
    }

    private var photoUri: Uri? = null
    private lateinit var values: ContentValues

    private fun cameraPictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        values = ContentValues(3)

        val displayName = "CapturePicture." + System.currentTimeMillis() + ".jpg"

        values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        //values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/playground");
        //values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/playground");
        photoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

        startActivityForResult(takePictureIntent, ACTION_CAMERA_CODE)
        //startCameraForResult.launch(takePictureIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERM_CODE ->
                checkGrantResults(grantResults) {
                    cameraPictureIntent()
                }
            GALLERY_PERM_CODE ->
                checkGrantResults(grantResults) {
                    galleryPictureIntent()
                }
        }
    }

    /** Check if this device has a camera */
    private fun checkCameraHardware(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun checkGrantResults(grantResults: IntArray, handler: () -> Unit) {
        if (grantResults.isNotEmpty() && grantResults.none { it != PackageManager.PERMISSION_GRANTED }) {
            handler()
        } else {
            Toast.makeText(
                this,
                "Permission is Required to use !",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    /*private val startCameraForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                imageView.setImageURI(photoUri)
            } else {
                finish()
            }
        }

    private val startGalleryForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                imageView.setImageURI(result.data?.data)
            } else {
                finish()
            }
        }*/

    companion object {
        const val ACTION_CREATE = "ACTION_CREATE"

        private const val DEFAULT = -1
        const val ACTION_CAMERA_CODE = 0
        const val ACTION_GALLERY_CODE = 1

        private const val CAMERA_PERM_CODE = 2
        private const val GALLERY_PERM_CODE = 3
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTION_CAMERA_CODE) {
                imageView.setImageURI(photoUri)
            }
            if (requestCode == ACTION_GALLERY_CODE) {
                imageView.setImageURI(data?.data)
            }
        } else {
            finish()
        }
    }
}