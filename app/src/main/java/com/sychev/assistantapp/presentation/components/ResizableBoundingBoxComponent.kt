package com.sychev.assistantapp.presentation.components

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.sychev.assistantapp.presentation.view.ResizableRectangleView

class ResizableBoundingBoxComponent(
    private val context: Context,
    private val windowManager: WindowManager,
) {
    private val rootView = ResizableRectangleView(context)
    private var layoutParams = WindowManager.LayoutParams(
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
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        ,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    val left
        get() = rootView.rectLeft
    val top
        get() = rootView.rectTop
    val right
        get() = rootView.rectRight
    val bottom
        get() = rootView.rectBottom

    var isShowing = false

    fun setLayoutParams(params: WindowManager.LayoutParams) {
        layoutParams = params
    }

    fun show(xCoordinates: List<Float>, yCoordinates: List<Float>) {
        if (rootView.parent == null) {
            isShowing = true
            rootView.setCoordinates(xCoordinates = xCoordinates, yCoordinates = yCoordinates)
            windowManager.addView(rootView, layoutParams)
        }
    }

    fun hide() {
        if (rootView.parent != null) {
            isShowing = false
            windowManager.removeView(rootView)
        }
    }

}
















