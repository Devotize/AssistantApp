package com.sychev.assistantapp.ui.assistant

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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.sychev.assistantapp.R
import com.sychev.assistantapp.ml.ClothesTestModel
import com.sychev.assistantapp.ui.ActivityImage
import com.sychev.assistantapp.ui.TAG
import com.sychev.assistantapp.ui.components.FrameDrawComponent
import com.sychev.assistantapp.ui.components.OverlayViewBack
import com.sychev.assistantapp.ui.components.ProgressBarComponent
import com.sychev.assistantapp.ui.components.ScreenshotComponent
import com.sychev.assistantapp.ui.utils.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


class Assistant(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {

    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView = layoutInflater.inflate(R.layout.assistant_layout, null)
    private val faceDetector = FaceDetection.getClient()

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
    private val screenshotComponent = ScreenshotComponent(context, windowManager)
    private val progressBar = ProgressBarComponent(context, windowManager)


    private val viewBack = OverlayViewBack(context, windowManager).also { overlayBack ->
        overlayBack.setonClickListenerCamera {
            CoroutineScope(Dispatchers.Main).launch {
                screenshotComponent.setScreenshot(takeScreenshot())
                screenshotComponent.show()
                recycleAssistantView()
                screenshotComponent.getScreenshot()?.let { screenshot -> detectFaces(screenshot) }

            }
//            screenshotComponent.getScreenshot()?.let {detectClothes(it)}
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
                screenshotComponent.setScreenshot(
                    cropBitmap(
                        btm,
                        frameDrawComponent.boundingBox.left,
                        frameDrawComponent.boundingBox.top,
                        frameDrawComponent.boundingBox.right,
                        frameDrawComponent.boundingBox.bottom
                    )
                )
            }
        }
        overlayBack.setonClickListenerScreen {
            val intent = Intent(context, ActivityImage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            intent.putExtra(ActivityImage.ACTION_CREATE, ActivityImage.ACTION_CAMERA_CODE)
            startActivity(context, intent, null)
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
        viewBack.show()
        windowManager.addView(rootView, windowParams)
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

    private fun cropBitmap(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Bitmap {
        val croppedBtm = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(croppedBtm)
        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val srcRect = Rect(left, top, right, bottom)
        val destRect =
            Rect(left, top, right, bottom)
        canvas.drawBitmap(bitmap, srcRect, destRect, Paint())
        return croppedBtm
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

    private fun detectClothes(bitmap: Bitmap) {
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
        val feature0 = output.outputFeature0AsTensorBuffer.buffer
        val feature1 = output.outputFeature1AsTensorBuffer.buffer
        val arr = ByteArray(feature0.remaining())
//        Log.d(TAG, "detectClothes: ${feature0.getFloat(feature0.remaining())}")
        feature0.rewind()
        for (i in 1..feature0.capacity() / 4) {
//            Log.d(TAG, "detectClothes: ${feature0.float}")
//        feature0.get(arr)
//        Log.d(TAG, "detectClothes: $arr")
        }
        val value1 = feature0.float
        val value2 = feature0.float
        Log.d(TAG, "detectClothes: val1: $value1, val2; $value2")

        model.close()

    }

    private fun detectFaces(bitmap: Bitmap) {
        progressBar.show()
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val result = faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                for (face in faces) {
                    val boundingBox = face.boundingBox
                    screenshotComponent.addCircleForDetectedObject(
                        boundingBox = boundingBox,
                        onClick = {
                            Log.d(TAG, "detectFaces: clicked on $face")
                        })
                }
                progressBar.hide()
            }
            .addOnFailureListener {
                Log.d(TAG, "detectFaces: Failure: $it")
                progressBar.hide()
            }
    }

}


























