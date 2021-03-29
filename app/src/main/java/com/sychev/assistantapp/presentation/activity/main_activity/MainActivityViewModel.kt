package com.sychev.assistantapp.presentation.activity.main_activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.sychev.assistantapp.presentation.assistant.Assistant
import com.sychev.assistantapp.utils.MyObjectDetector
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

class MainActivityViewModel: ViewModel() {

    val isAssistantActive = mutableStateOf(false)
    var assistant: Assistant? = null
    val croppedScreenshot: MutableState<Bitmap?> = mutableStateOf(null)
    val detectedObjects: MutableState<List<DetectedObject>?> = mutableStateOf(null)
    val loading = mutableStateOf(false)

    fun mediaProjectorPermission(activity: Activity) {
        val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        activity.startActivityForResult(projectionManager.createScreenCaptureIntent(), PROJECTION_MANAGER_PERMISSION_CODE)
    }

    fun checkPermissionSystemWindow(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                activity.startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_CODE)
            }
        }
    }

    fun changeIsAssistantActive() {
        isAssistantActive.value = !isAssistantActive.value
    }

    fun stopAssistant() {
        assistant?.close()
        assistant = null
    }
    
    fun setCroppedScreenshot(bitmap: Bitmap) {
        Log.d(TAG, "setCroppedScreenshot: bitmap = $bitmap")
        croppedScreenshot.value = bitmap
    }

    fun launchImageCrop(uri: Uri, activity: Activity) {
        CropImage.activity(uri)
            .setGuidelines(CropImageView.Guidelines.ON)
            .setAspectRatio(1920,1080)
            .start(activity)
    }

    fun detectObjects(bitmap: Bitmap) {
        loading.value = true
        MyObjectDetector().instance.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener {
                Log.d(TAG, "detectObjects: succes! objects: $it")
                loading.value = false
                detectedObjects.value = it
            }
            .addOnFailureListener {
                loading.value = false
                detectedObjects.value = null
        }
    }

}