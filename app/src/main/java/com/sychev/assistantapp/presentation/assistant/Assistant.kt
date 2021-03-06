package com.sychev.assistantapp.presentation.assistant

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.Image.Plane
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.sychev.assistantapp.R
import com.sychev.assistantapp.domain.model.DetectedClothes
import com.sychev.assistantapp.ml.ClothesTestModel
import com.sychev.assistantapp.network.model.DetectedClothesDto
import com.sychev.assistantapp.presentation.activity.camera_activity.CameraPhotoActivity
import com.sychev.assistantapp.presentation.activity.main_activity.TAG
import com.sychev.assistantapp.presentation.components.*
import com.sychev.assistantapp.presentation.utils.FakeDetectedClothesData
import com.sychev.assistantapp.presentation.utils.State
import com.sychev.assistantapp.repository.ClothesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import javax.inject.Inject
import kotlin.math.absoluteValue

class Assistant
    constructor(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val repository: ClothesRepository
) {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView = layoutInflater.inflate(R.layout.assistant_layout, null)
    private var detectedClothes: DetectedClothes? = null

    private var heightPx: Int = 0
    private var widthPx: Int = 0

    // Coordinates before the start of the movement
    private var prevMoveX = 0
    private var prevMoveY = 0

    // Coordinates at the start of the movement
    private var startMoveX = 0.0
    private var startMoveY = 0.0

    private val isMoved: Boolean
        get() = prevMoveX != windowParams.x || prevMoveY != windowParams.y

    private val windowManager =
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).also { wm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowsMetrics = wm.currentWindowMetrics
                val windowInsets = windowsMetrics.windowInsets
                val insets = windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
                )
                val insetsWidth = insets.right + insets.left
                val insetsHeight = insets.top + insets.bottom
                val bounds = windowsMetrics.bounds
                widthPx = bounds.width() - insetsWidth
                heightPx = bounds.height() - insetsHeight
            } else {
                val size = Point()
                val display = wm.defaultDisplay
                display.getSize(size)
                widthPx = size.x
                heightPx = size.y
            }
        }

    private val frameDrawComponent = FrameDrawComponent(context, windowManager, this)
    private val screenshotComponent = ScreenshotComponent(context, windowManager, this)
    private val progressBar = ProgressBarComponent(context, windowManager)
    val webViewComponent = WebViewComponent(context, windowManager)
    private val detectedClothesComponent = DetectedClothesComponent(context, windowManager, this)


    private val viewBack = OverlayViewBack(context, windowManager).also { overlayBack ->
        overlayBack.setonClickListenerCamera {
            CoroutineScope(Dispatchers.Main).launch {
                screenshotComponent.setScreenshot(takeScreenshot())
                screenshotComponent.show()
//                recycleAssistantView()
//                screenshotComponent.getScreenshot()?.let { screenshot -> detectFaces(screenshot) }
//                screenshotComponent.getScreenshot()?.let {
//                    detectedClothes = detectClothes(it)
//                }
            }
        }
        overlayBack.setonClickListenerExit {
            hideComponents()
        }
        overlayBack.setOnClickListenerCrop {
            screenshotComponent.removeCirclesForDetectedObject()
            frameDrawComponent.show()
        }
        overlayBack.setOnClickListenerDone {
            frameDrawComponent.hide()
            frameDrawComponent.boundingBox.hide()
            screenshotComponent.getScreenshot()?.let { btm ->
//                screenshotComponent.setScreenshot(
//                    cropBitmap(
//                        btm,
//                        frameDrawComponent.boundingBox.left,
//                        frameDrawComponent.boundingBox.top,
//                        frameDrawComponent.boundingBox.right,
//                        frameDrawComponent.boundingBox.bottom
//                    )
//                )
            }
        }
        overlayBack.setonClickListenerScreen {
            val intent = Intent(context, CameraPhotoActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(context, intent, null)
        }
        overlayBack.setonClickListenerSettings {
            fakePostRequest()
        }
    }

    @SuppressLint("WrongConstant")
    private val imageReader =
        ImageReader.newInstance(widthPx, heightPx, PixelFormat.RGBA_8888, 1)
            .also {
                createVirtualDisplay(it)
            }

    private val iconButton =
        rootView.findViewById<ImageButton>(R.id.assistant_icon)

    private val windowParams = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    private fun createVirtualDisplay(ir: ImageReader) {
        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
        mediaProjection.createVirtualDisplay(
            "screen_display",
            widthPx,
            heightPx,
            context.resources.displayMetrics.densityDpi,
            flags,
            ir.surface,
            null,
            null
        )
    }

    init {
        setOnTouchListener()
    }

    private suspend fun takeScreenshot(): Bitmap {
        close()
        delay(350)
        Log.d(TAG, "takeScreenshot: assistantParent: ${rootView.parent}")
        val image = imageReader.acquireNextImage()
        val planes: Array<Plane> = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bmp = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(buffer)
        val newBitmap = Bitmap.createBitmap(bmp, 0, 0, image.width, image.height)
        image.close()
        open()
        return newBitmap
    }

    private fun hideComponents() {
        frameDrawComponent.hide()
        frameDrawComponent.boundingBox.hide()
        screenshotComponent.hide()
    }

    fun open() {
        if (rootView.parent == null){
            viewBack.show()
            windowManager.addView(rootView, windowParams)
        }
    }

    fun close() {
        if (rootView.parent != null) {
            viewBack.hide()
            windowManager.removeView(rootView)
        }
    }

    fun recycleAssistantView() {
        if (rootView.parent != null) {
            viewBack.hide()
            viewBack.show()
            windowManager.removeView(rootView)
            windowManager.addView(rootView, windowParams)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener() {
        iconButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    prevMoveX = windowParams.x
                    prevMoveY = windowParams.y
                    windowParams.gravity = Gravity.CENTER
                    startMoveX = event.rawX.toDouble()
                    startMoveY = event.rawY.toDouble()

                }
                MotionEvent.ACTION_MOVE -> {
                    if (viewBack.state == State.CLOSE) {
                        val deltaX = event.rawX - startMoveX
                        val deltaY = event.rawY - startMoveY
                        moveAt(prevMoveX + deltaX.toInt(), prevMoveY + deltaY.toInt())
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        if (viewBack.state == State.CLOSE) {
                            viewBack.open()
                        } else if (viewBack.state == State.OPEN || viewBack.state == State.OPENCSREEN) {
                            viewBack.close()
                        }
                    } else {
                        viewBack.moveAt(windowParams.x, windowParams.y)
                    }
                }
            }
            return@setOnTouchListener true
        }
    }

    private fun moveAt(x: Int = 0, y: Int = 0) {
        windowParams.x = x
        windowParams.y = y
        windowManager.updateViewLayout(rootView, windowParams)
    }

    private fun detectClothes(bitmap: Bitmap): DetectedClothes {
        val model = ClothesTestModel.newInstance(context)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(416, 416, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        val tImage = TensorImage(DataType.FLOAT32)
        tImage.load(bitmap)
        val resizedTImage = imageProcessor.process(tImage)
        val inputBuffer =
            TensorBuffer.createFixedSize(intArrayOf(1, 416, 416, 3), DataType.FLOAT32)
        inputBuffer.loadBuffer(resizedTImage.buffer)
        val output = model.process(inputBuffer)
        val feature0 = output.outputFeature0AsTensorBuffer
        val feature1 = output.outputFeature1AsTensorBuffer
        Log.d(TAG, "detectClothes: ${feature0.floatArray.toList()}")
        Log.d(TAG, "detectClothes: feature1 = ${feature1.floatArray.toList()}")

        val rect = Rect()

        model.close()
        return DetectedClothes(feature0.toString(), rect)
    }

    private fun processOutput(arr: FloatArray): List<Float>{
        val list = ArrayList<Float>()
            for (i in arr.indices){
                list.add((arr[i] - 0)*1)
            }
        return list
    }

    private fun downloadModel() {
        progressBar.show()
        var interpreter: Interpreter? = null
        val conditions = CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build()

        FirebaseModelDownloader.getInstance()
            .getModel("ClothesTestDetector", DownloadType.LATEST_MODEL, conditions)
            .addOnSuccessListener {customModel ->
                progressBar.hide()
                Log.d(TAG, "downloadModel: success!!")
                val modelFile = customModel.file
                if (modelFile != null) {
                    interpreter = Interpreter(modelFile)
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "downloadModel: $it")
                progressBar.hide()
            }
    }

    fun fakePostRequest() {
        //just for testing purposes
        detectedClothes = DetectedClothes("test", Rect())

        detectedClothes?.let{dc ->
            CoroutineScope(Main).launch {
                dc.image?.let {
                    progressBar.show()
                    Log.d(TAG, "making a query from assistant")
//                        val detectedClothes =  repository.sendDetectedClothes(it)
                    delay(2000)
                    FakeDetectedClothesData.detectedObjects.forEach {
                    }
//                    webViewComponent.setUrls(urls)
                    detectedClothesComponent.setDetectedClothes(FakeDetectedClothesData.detectedObjects)
                    progressBar.hide()
                    detectedClothesComponent.show()
                }

            }
        }

    }

}


























