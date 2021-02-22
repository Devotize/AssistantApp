package com.sychev.assistantapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.Image.Plane
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.sychev.assistantapp.R
import com.sychev.assistantapp.ui.components.DrawLineView
import com.sychev.assistantapp.ui.components.ResizableRectangleView
import com.sychev.assistantapp.utils.MyObjectDetector


class Assistant(
        private val context: Context,
        private val mediaProjection: MediaProjection
) {

    private var isActive = false
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView = layoutInflater.inflate(R.layout.assistant_layout, null)
    private val objectDetector = MyObjectDetector().instance
    private var screenshot: Bitmap? = null
    private var myRectangleView: ResizableRectangleView? = null

    private val displayMetrics = DisplayMetrics()

    private val display: Display = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        context.display!!
    else
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay).also {
            it?.getRealMetrics(displayMetrics)
    }
    private val heightPx = displayMetrics.heightPixels
    private val widthPx = displayMetrics.widthPixels
    @SuppressLint("WrongConstant")
    private val imageReader = ImageReader.newInstance(widthPx, heightPx, PixelFormat.RGBA_8888, 1)
            .also {
                val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                mediaProjection.createVirtualDisplay("screen_display", widthPx, heightPx, displayMetrics.densityDpi, flags, it.surface, null, null)
            }

    private val iconButton = rootView.findViewById<ImageButton>(R.id.assistant_icon)
    private val message = rootView.findViewById<TextView>(R.id.choose_field_text_view)
    private val mainFrame = rootView.findViewById<FrameLayout>(R.id.main_frame)
    private val backgroundImageView = rootView.findViewById<ImageView>(R.id.background_image_view)
    private val progressBar = rootView.findViewById<ProgressBar>(R.id.progress_bar)
    private val cropButton = rootView.findViewById<Button>(R.id.crop_button).apply {
        setOnClickListener {
            Log.d(TAG, "onClick: $screenshot")
            screenshot?.let{screenshot ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    return@let
                }else {
//                    val fileName = "bitmap.png"
//                    val stream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
//                    screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
//                    stream.close()
//
//                    val intent = Intent(context, CropActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    intent.putExtra("screenshot", fileName)
//                    context.startActivity(intent)
//                    close()
                    myRectangleView?.let{
                        val croppedBtm = Bitmap.createBitmap(screenshot.width, screenshot.height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(croppedBtm)
                        val paint = Paint()
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        val srcRect = Rect(it.rectLeft, it.rectTop, it.rectRight, it.rectBottom)
                        val destRect = Rect(0, 0, it.rectLeft + it.rectRight, it.rectTop + it.rectBottom)
                        canvas.drawBitmap(screenshot, srcRect, destRect, Paint())
                        val view = ImageView(context)
                        val params = FrameLayout.LayoutParams(
                            it.left + it.right,
                            it.top + it.bottom,
                            Gravity.CENTER
                        )
                        view.setImageBitmap(croppedBtm)
                        mainFrame.addView(view, params)
                        removeRectangle()
                    }

                }
            }
        }
    }

    private val windowParams = WindowManager.LayoutParams(
            widthPx / 3,
            heightPx / 5,
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

    private fun initWindow() {
        refreshAssistantView()

        iconButton.setOnClickListener {
            onIconClicked()
        }
    }

    private fun onIconClicked() {
        changeIsActive()
        takeScreenshot()
        initDrawLine()
        refreshAssistantView()
        updateWindowManager()
    }

    private fun initDrawLine() {
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        DrawLineView(context).also {view ->
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
//                    Log.d(TAG, "initDrawLine: drawing rectangle listX = ${view.touchedCoordinatesX}, \n listY = ${view.touchedCoordinatesY}")
                    showObjectFrame(view.touchedCoordinatesX, view.touchedCoordinatesY)
                }
                false
            }
            mainFrame.addView(view, layoutParams)
        }
    }

    private fun refreshAssistantView() {
        if (isActive) {
            setWindowParams(widthPx, heightPx)
            iconButton.background = ContextCompat.getDrawable(context, R.drawable.icon_1)
            message.visibility = View.VISIBLE
            cropButton.visibility = View.VISIBLE
            backgroundImageView.setImageBitmap(screenshot)
            screenshot?.let { detectImage(it) }
        } else {
            setWindowParams(widthPx / 3, heightPx / 5)
            iconButton.background = ContextCompat.getDrawable(context, R.drawable.icon_2)
            message.visibility = View.GONE
            backgroundImageView.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
            cropButton.visibility = View.GONE
            clearMainFrame()
        }
    }

    private fun clearMainFrame() {
        mainFrame.removeAllViews()
        mainFrame.background = ColorDrawable(Color.TRANSPARENT)
    }

    private fun takeScreenshot() {
        mainFrame.visibility = View.INVISIBLE
        updateWindowManager()
        val image = imageReader.acquireLatestImage()
        val planes: Array<Plane> = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bmp = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
        bmp.copyPixelsFromBuffer(buffer)
        val newBitmap = Bitmap.createBitmap(bmp, 0,0, image.width, image.height)
        image.close()
        mainFrame.visibility = View.VISIBLE
        updateWindowManager()
//        screenshot = bmp
        screenshot = newBitmap
    }

    private fun detectImage(bitmap: Bitmap) {
        mainFrame.background = ColorDrawable(ContextCompat.getColor(context, R.color.half_transparent))
        showProgressBar()
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

                hideProgressBar()
            }
            .addOnFailureListener {
                hideProgressBar()
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
    
    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        updateWindowManager()
    }
    
    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
        updateWindowManager()
    }

    init {
        initWindow()
    }

    fun open() {
        windowManager.addView(rootView, windowParams)
    }

    fun close() {
        if (rootView.parent != null) {
            windowManager.removeView(rootView)
        }
    }

    private fun updateWindowManager() {
        close()
        open()
    }

    private fun addCircleForDetectedObject(detectedObject: DetectedObject) {
        val x= detectedObject.boundingBox.exactCenterX().toInt()
        val y= detectedObject.boundingBox.exactCenterY().toInt()
        val params = FrameLayout.LayoutParams(
            80,
            80,
        )
        params.leftMargin = x
        params.topMargin = y
        for (label in detectedObject.labels) {
            Log.d(TAG, "addCircleForDetectedObject: trakingId = ${detectedObject.trackingId} labelText = ${label.text}")
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

        mainFrame.background = ColorDrawable(Color.TRANSPARENT)
        mainFrame.addView(view, params)
        mainFrame.addView(tv, tvParams)
    }

    private fun showObjectFrame(xList: List<Float>, yList: List<Float>) {
        clearMainFrame()
        myRectangleView = ResizableRectangleView(context, xList, yList).also {
            mainFrame.addView(it)
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
        mainFrame.addView(tv, params)
    }

    private fun removeRectangle() {
        myRectangleView?.let{
            mainFrame.removeView(it)
        }
    }

}

























