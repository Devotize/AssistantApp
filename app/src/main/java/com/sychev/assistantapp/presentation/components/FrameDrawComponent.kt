package com.sychev.assistantapp.presentation.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import com.sychev.assistantapp.presentation.activity.main_activity.TAG
import com.sychev.assistantapp.presentation.assistant.Assistant
import com.sychev.assistantapp.presentation.view.FrameDrawView

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
    private var layoutParams = WindowManager.LayoutParams(
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
    )
    val boundingBox = ResizableBoundingBoxComponent(context, windowManager)

    fun setLayoutParams(params: WindowManager.LayoutParams) {
        layoutParams = params
    }

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

    fun cropBitmap(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ): Bitmap {
        val croppedBtm = Bitmap.createBitmap(
            width,
            height,
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


}













