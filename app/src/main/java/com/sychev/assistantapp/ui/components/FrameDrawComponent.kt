package com.sychev.assistantapp.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import com.sychev.assistantapp.ui.assistant.Assistant
import com.sychev.assistantapp.ui.view.FrameDrawView

class FrameDrawComponent(
    private val context: Context,
    private val windowManager: WindowManager,
    private val assistant: Assistant? = null
) {
    @SuppressLint("ClickableViewAccessibility")
    private val rootView = FrameDrawView(context = context).apply {
        setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP){
                boundingBox.show(this.touchedCoordinatesX, this.touchedCoordinatesY)
                hide()
                assistant?.recycleAssistantView()
            }
            false
        }
    }
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
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }
    val boundingBox = ResizableBoundingBox(context, windowManager)

    fun show() {
        if (rootView.parent == null){
            windowManager.addView(rootView, layoutParams)
        }
     }

    fun hide() {
        if (rootView.parent != null) {
            windowManager.removeView(rootView)
        }
    }


}













