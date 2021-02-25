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
import com.sychev.assistantapp.ml.TfliteTestModel
import com.sychev.assistantapp.ui.TAG
import com.sychev.assistantapp.ui.components.DrawLineView
import com.sychev.assistantapp.ui.components.ResizableRectangleView
import com.sychev.assistantapp.utils.MyObjectDetector
import org.tensorflow.lite.support.image.TensorImage
import java.lang.Exception


class Assistant(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {

    private var isActive = false
    private val layoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val assistantLayoutView = layoutInflater.inflate(R.layout.assistant_layout, null)
    private val screenshotImageView = ImageView(context)
    private val drawLineView = DrawLineView(context)

    private val objectDetector = MyObjectDetector().instance
    private var screenshot: Bitmap? = null
    private var resizableRectangleView: ResizableRectangleView? = null

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
    private val cancelButton = assistantLayoutView.findViewById<ImageButton>(R.id.cancel_button).apply{
        setOnClickListener {
            removeEverythingButAssistant()
        }
    }
    private val cropButton = assistantLayoutView.findViewById<ImageButton>(R.id.crop_button).apply {
        setOnClickListener {
            addDrawLine()
        }
    }


    private val screenshotButton =
        assistantLayoutView.findViewById<ImageButton>(R.id.screenshot_button).apply {
            setOnClickListener {
                    showExtraButtons()
                    removeEverythingButAssistant()
                    takeScreenshot()
                    screenshot?.let{
                        addScreenshotViewToWindowManager(it)
                    }
                    recycleAssistantView()

//                    screenshot?.let { screenshot ->
//                        Log.d(TAG, "Screenshot clicked: screenshot is not null")
////                    val fileName = "bitmap.png"
////                    val stream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
////                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
////                    stream.close()
////
////                    val intent = Intent(context, CropActivity::class.java)
////                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
////                    intent.putExtra("screenshot", fileName)
////                    context.startActivity(intent)
////                    close()
//
//                        resizableRectangleView?.let{
//                            val croppedBtm = it.cropBitmap(screenshot)
//                            val view = ImageView(context).apply {
//                                setImageBitmap(croppedBtm)
//                                scaleType = ImageView.ScaleType.FIT_XY
//                            }
//                            val params = FrameLayout.LayoutParams(
//                                550,
//                                350,
//                                Gravity.CENTER
//                            )
//                            removeScreenshotViewFromWindowManager()
//                            windowManager.addView(view, params)
//                            removeRectangle()
//                            croppedBtm.detectObjects(context)
//                        }
//                    }


                Log.d(TAG, "onClick: $screenshot")
            }
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
        cropButton.visibility = View.VISIBLE
        doneButton.visibility = View.VISIBLE
        cancelButton.visibility = View.VISIBLE
    }

    private fun hideExtraButtons() {
        cropButton.visibility = View.GONE
        doneButton.visibility = View.GONE
        cancelButton.visibility = View.GONE
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

    @SuppressLint("ClickableViewAccessibility")
    private fun addDrawLine() {
            drawLineView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
//                    Log.d(TAG, "initDrawLine: drawing rectangle listX = ${view.touchedCoordinatesX}, \n listY = ${view.touchedCoordinatesY}")
                    addCropFrameToWindowManager(drawLineView.touchedCoordinatesX, drawLineView.touchedCoordinatesY)
                    removeDrawLine()
                }
                false
            }
            windowManager.addView(drawLineView, windowParams)

    }

    private fun removeDrawLine() {
        try{
            windowManager.removeView(drawLineView)
        } catch (e: Exception) {
            //ignore
        }
    }

    private fun refreshAssistantView() {
        if (isActive) {
            iconButton.background = ContextCompat.getDrawable(context, R.drawable.icon_1)
            screenshotButton.visibility = View.VISIBLE
        } else {
            iconButton.background = ContextCompat.getDrawable(context, R.drawable.icon_2)
            screenshotButton.visibility = View.GONE
            hideExtraButtons()
            removeEverythingButAssistant()
        }
    }

    private fun removeEverythingButAssistant() {
        removeScreenshotViewFromWindowManager()
        removeRectangle()
    }

    private fun addScreenshotViewToWindowManager(bitmap: Bitmap) {
        screenshotImageView.setImageBitmap(bitmap)
        val params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
        )
        windowManager.addView(screenshotImageView, windowParams)
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
//        screenshot = bmp
        screenshot = newBitmap.also {
            it.detectObjects(context)
        }
        Log.d(TAG, "takeScreenshot: screenshot: $screenshot")
    }

    private fun detectImage(bitmap: Bitmap) {
//        mainFrame.background =
//            ColorDrawable(ContextCompat.getColor(context, R.color.half_transparent))
//        showProgressBar()
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        objectDetector.process(inputImage)
            .addOnSuccessListener {
                Log.d(TAG, "detectImage: onSuccess")
                if (it.isNotEmpty()) {
                    it.forEach {
                        addCircleForDetectedObject(it)
                    }
                } else {
//                    addNoObjectsFoundTv()
                }
//                Log.d(TAG, "detectImage: centerY = ${it[0].boundingBox.centerY()}")

//                hideProgressBar()
            }
            .addOnFailureListener {
//                hideProgressBar()
                Log.d(TAG, "detectImage: onFailure $it")
            }
    }

    private fun setWindowParams(width: Int, height: Int) {
        windowParams.width = width
        windowParams.height = height
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
        removeEverythingButAssistant()
    }

    private fun recycleAssistantView() {
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
//            showObjectFrame(detectedObject)
        }

//        mainFrame.background = ColorDrawable(Color.TRANSPARENT)
//        mainFrame.addView(view, params)
//        mainFrame.addView(tv, tvParams)
    }

    private fun addCropFrameToWindowManager(xList: List<Float>, yList: List<Float>) {
        resizableRectangleView = ResizableRectangleView(context, xList, yList).also {
            val layoutParams = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
            )
            windowManager.addView(it, windowParams)
            recycleAssistantView()
        }
    }

    private fun addNoObjectsFoundTv() {
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        val tv = TextView(context).apply {
            textSize = 30f
            text = "No Objects Found"
            setTextColor(ContextCompat.getColor(context, R.color.orange_700))
        }
//        mainFrame.addView(tv, params)
    }

    private fun removeRectangle() {
        resizableRectangleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                //ignote
            }
        }
    }

    private fun ResizableRectangleView.cropBitmap(bitmap: Bitmap): Bitmap {
        val croppedBtm = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(croppedBtm)
        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val srcRect = Rect(this.rectLeft, this.rectTop, this.rectRight, this.rectBottom)
        val destRect =
            Rect(0, 0, this.rectLeft + this.rectRight, this.rectTop + this.rectBottom)
        canvas.drawBitmap(bitmap, srcRect, destRect, Paint())
        return croppedBtm
    }

    private fun Bitmap.detectObjects(context: Context) {
        val testModel = TfliteTestModel.newInstance(context)
        val tfImage = TensorImage.fromBitmap(this)
        val outputs = testModel.process(tfImage)
        val locations = outputs.locationsAsTensorBuffer
        val classes = outputs.classesAsTensorBuffer
        val scores = outputs.scoresAsTensorBuffer
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer

        Log.d(TAG, "detectObjects: locations = ${locations.dataType}")
        Log.d(TAG, "detectObjects: classes = ${classes.buffer}")
        Log.d(TAG, "detectObjects: scores = ${scores.buffer}")
        Log.d(TAG, "detectObjects: numberOfDetectedObjects = ${numberOfDetections.buffer}")

        testModel.close()
    }

}

























