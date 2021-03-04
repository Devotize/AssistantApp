package com.sychev.assistantapp.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView

class ScreenshotComponent(
    private val context: Context,
    private val windowManager: WindowManager
) {
    val rootView = FrameLayout(context)
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

    fun hide() {
        if (rootView.parent != null) {
            windowManager.removeView(rootView)
            rootView.removeAllViews()
        }
    }

}















