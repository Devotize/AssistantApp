package com.sychev.assistantapp.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.sychev.assistantapp.R
import com.sychev.assistantapp.ui.TAG

class ScreenshotComponent(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val rootView = FrameLayout(context)
    private val imageView = ImageView(context)
    private val layoutParams = WindowManager.LayoutParams(
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
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        ,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }
    private var screenshot: Bitmap? = null

    fun setScreenshot(bitmap: Bitmap) {
        screenshot = bitmap
        imageView.setImageBitmap(screenshot)
    }
    fun getScreenshot() = screenshot

    fun show() {
        if (rootView.parent == null) {
            windowManager.addView(rootView, layoutParams)
            rootView.addView(imageView)
        }
    }

    fun addCircleForDetectedObject(boundingBox: Rect, onClick: () -> Unit){
        val x = boundingBox.left
        val y = boundingBox.top

        val params = FrameLayout.LayoutParams(
            60,
            60,
        )
        params.leftMargin = x
        params.topMargin = y


        val circleView = Button(context)
        circleView.background = ContextCompat.getDrawable(context, R.drawable.orange_circle_shape)
        circleView.setOnClickListener {
            onClick()
        }
        rootView.addView(circleView, params)
    }

    fun removeCirclesForDetectedObject() {
        hide()
        show()
    }

    fun hide() {
        if (rootView.parent != null) {
            windowManager.removeView(rootView)
            rootView.removeAllViews()
        }
    }

}















