package com.sychev.assistantapp.presentation.components

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import com.sychev.assistantapp.presentation.view.ResizableRectangleView

class ResizableBoundingBoxComponent(
    private val context: Context,
    private val windowManager: WindowManager,
) {
    private val rootView = ResizableRectangleView(context)
    private val layoutParams = WindowManager.LayoutParams(
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
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        ,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }
    var left = 0
    var top = 0
    var right = 0
    var bottom = 0


    fun show(xCoordinates: List<Float>, yCoordinates: List<Float>) {
        if (rootView.parent == null) {
            rootView.setCoordinates(xCoordinates = xCoordinates, yCoordinates = yCoordinates)
            setBounds(rootView.rectLeft, rootView.rectTop, rootView.rectRight, rootView.rectBottom)
            windowManager.addView(rootView, layoutParams)
        }
    }

    fun hide() {
        if (rootView.parent != null) {
            windowManager.removeView(rootView)
        }
    }

    private fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.bottom = bottom
        this.right = right
    }

}
















