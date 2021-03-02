package com.sychev.assistantapp.ui.assistant

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.Image.Plane
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.sychev.assistantapp.R
import com.sychev.assistantapp.ml.ClothesTestModel
import com.sychev.assistantapp.ui.TAG
import com.sychev.assistantapp.ui.components.FrameDrawComponent
import com.sychev.assistantapp.ui.components.ResizableBoundingBox
import com.sychev.assistantapp.ui.view.FrameDrawView
import com.sychev.assistantapp.ui.view.ResizableRectangleView
import com.sychev.assistantapp.utils.MyObjectDetector
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.lang.Exception


class Assistant(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {

    private var isActive = false
    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val assistantLayoutView = layoutInflater.inflate(R.layout.assistant_layout, null)
    private val screenshotImageView = ImageView(context).also {
        it.scaleType = ImageView.ScaleType.CENTER_CROP
    }
    private var screenshot: Bitmap? = null

    private var heightPx: Int = 0
    private var widthPx: Int = 0

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


    @SuppressLint("WrongConstant")
    private val imageReader = ImageReader.newInstance(widthPx, heightPx, PixelFormat.RGBA_8888, 1)
        .also {
            createVirtualDisplay(it)
        }

    private val iconButton =
        assistantLayoutView.findViewById<ImageButton>(R.id.assistant_icon).apply {
            setOnClickListener {
                onIconClicked()
            }
        }
    private val doneButton = assistantLayoutView.findViewById<ImageButton>(R.id.done_button)
    private val cancelButton =
        assistantLayoutView.findViewById<ImageButton>(R.id.cancel_button).apply {
            setOnClickListener {
                hideExtraButtons()
            }
        }
    private val cropButton = assistantLayoutView.findViewById<ImageButton>(R.id.crop_button).apply {
        setOnClickListener {
            screenshot?.let { shot ->
                val boundingBox = frameDrawComponent.boundingBox
                screenshot = cropBitmap(
                    shot,
                    boundingBox.left,
                    boundingBox.top,
                    boundingBox.right,
                    boundingBox.bottom
                )
                Log.d(TAG, "bounding box is shown croppedBitmap = $screenshot")
                screenshot?.let {
                    screenshotImageView.setImageBitmap(it)
                    screenshotImageView.setBackgroundColor(Color.BLACK)
                }
                boundingBox.hide()
                it.visibility = View.GONE
                recycleAssistantView()
            }
        }
    }
    private val drawButton = assistantLayoutView.findViewById<ImageButton>(R.id.draw_button).apply {
        setOnClickListener {
            frameDrawComponent.show()
            cropButton.visibility = View.VISIBLE
            recycleAssistantView()
        }
    }


    private val screenshotButton =
        assistantLayoutView.findViewById<ImageButton>(R.id.screenshot_button).apply {
            setOnClickListener {
                showExtraButtons()
                takeScreenshot()
                screenshot?.let {
                    addScreenshotViewToWindowManager(it)
                }
                recycleAssistantView()

                Log.d(TAG, "onClick: $screenshot")
            }
        }

    private val extraParams = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        0,
        0,
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


    private val windowParams = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        0,
        0,
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
        gravity = Gravity.TOP or Gravity.END
    }

    private fun showExtraButtons() {
        drawButton.visibility = View.VISIBLE
        doneButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
    }

    private fun hideExtraButtons() {
        drawButton.visibility = View.GONE
        doneButton.visibility = View.GONE
        cancelButton.visibility = View.GONE
        cropButton.visibility = View.GONE
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

    private fun initWindow() {
        refreshAssistantView()
    }

    private fun onIconClicked() {
        changeIsActive()
        refreshAssistantView()
    }

    private fun refreshAssistantView() {
        if (isActive) {
            iconButton.background = ContextCompat.getDrawable(context, R.drawable.icon_1)
            screenshotButton.visibility = View.VISIBLE
        } else {
            iconButton.background = ContextCompat.getDrawable(context, R.drawable.icon_2)
            screenshotButton.visibility = View.GONE
            hideExtraButtons()
        }
    }

    private fun addScreenshotViewToWindowManager(bitmap: Bitmap) {
        screenshotImageView.setImageBitmap(bitmap)
        windowManager.addView(screenshotImageView, extraParams)
    }

    private fun removeScreenshotViewFromWindowManager() {
        try {
            windowManager.removeView(screenshotImageView)
        } catch (e: Exception) {
            //ignore
        }
    }

    private fun takeScreenshot() {
        assistantLayoutView.visibility = View.GONE
        Log.d(TAG, "takeScreenshot: assistantParent: ${assistantLayoutView.parent}")
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
        assistantLayoutView.visibility = View.VISIBLE
        screenshot = newBitmap.also {
            detectClothes(it)
        }
        Log.d(TAG, "takeScreenshot: screenshot: $screenshot")
    }

    private fun changeIsActive() {
        isActive = !isActive
    }

    init {
        initWindow()
    }

    fun open() {
        windowManager.addView(assistantLayoutView, windowParams)
    }

    fun close() {
        if (assistantLayoutView.parent != null) {
            windowManager.removeView(assistantLayoutView)
        }
    }

    fun recycleAssistantView() {
        if (assistantLayoutView.parent != null) {
            windowManager.removeView(assistantLayoutView)
            windowManager.addView(assistantLayoutView, windowParams)
        }
    }

    private fun addCircleForDetectedObject(detectedObject: DetectedObject) {
        val x = detectedObject.boundingBox.exactCenterX().toInt()
        val y = detectedObject.boundingBox.exactCenterY().toInt()
        val params = FrameLayout.LayoutParams(
            80,
            80,
        )
        params.leftMargin = x
        params.topMargin = y
        for (label in detectedObject.labels) {
            Log.d(
                TAG,
                "addCircleForDetectedObject: trakingId = ${detectedObject.trackingId} labelText = ${label.text}"
            )
        }

        var tvVisible = false
        val tv = TextView(context).apply {
            text = if (detectedObject.labels.isNotEmpty())
                detectedObject.labels[0].text
            else
                "Object"
            textSize = 26f
            setTextColor(ContextCompat.getColor(context, R.color.orange_700))
            visibility = View.INVISIBLE
        }
        val tvParams = FrameLayout.LayoutParams(
            260,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = x
            topMargin = y + 90
        }

        val view = Button(context)
        view.background = ContextCompat.getDrawable(context, R.drawable.orange_circle_shape)
        view.setOnClickListener {
            tvVisible = !tvVisible
            tv.visibility = if (tvVisible) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun cropBitmap(bitmap: Bitmap, left: Int, top: Int, right: Int, bottom: Int): Bitmap {
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

    private fun detectClothes(bitmap: Bitmap) {
        val model = ClothesTestModel.newInstance(context)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(416, 416, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        val tImage = TensorImage(DataType.FLOAT32)
        tImage.load(bitmap)
        val resizedTImage = imageProcessor.process(tImage)
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 416, 416, 3), DataType.FLOAT32)
        inputBuffer.loadBuffer(resizedTImage.buffer)
        Log.d(TAG, "detectClothes: inputBuffer: $inputBuffer")
        val output = model.process(inputBuffer)
        Log.d(TAG, "detectClothes: output: $output")
        val feature0 = output.outputFeature0AsTensorBuffer
        val feature1 = output.outputFeature1AsTensorBuffer
        Log.d(TAG, "detectClothes: feature0: ${feature0.floatArray}")
        Log.d(TAG, "detectClothes: feature1: ${feature1.floatArray}")


        model.close()


    }

}

























