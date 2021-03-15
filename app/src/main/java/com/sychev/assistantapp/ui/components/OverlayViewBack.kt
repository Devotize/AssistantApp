package com.sychev.assistantapp.ui.components

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import com.sychev.assistantapp.R
import com.sychev.assistantapp.ui.TAG
import com.sychev.assistantapp.ui.assistant.Assistant
import com.sychev.assistantapp.ui.utils.State
import com.sychev.assistantapp.ui.utils.awaitTransitionComplete

import kotlinx.coroutines.*

/**
 *  It is motionLayout view
 */

class OverlayViewBack(
    context: Context,
    private val windowManager: WindowManager,
) {
    private val relative: View = View.inflate(context, R.layout.overlay_view_back, null)
    private val motion: MotionLayout = relative.findViewById(R.id.motion_layout) as MotionLayout
    private val params = WindowManager.LayoutParams()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    var state = State.CLOSE
        private set

    init {
        initParams()
    }

    fun show() {
        if (relative.parent == null) {
            windowManager.addView(relative, params)
        }
    }

    fun hide() {
        if (relative.parent != null) {
            windowManager.removeView(relative)
        }
    }

    private fun initParams() {
        val TYPE_OVERLAY = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.CENTER
        params.type = TYPE_OVERLAY
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        params.format = PixelFormat.TRANSPARENT

    }

    fun open() {
        if (state == State.OPEN) return

        state = State.Animated

        scope.launch {
            motion.setTransition(R.id.open_center_step1)
            motion.transitionToEnd()
            motion.awaitTransitionComplete(R.id.center_step1)
            motion.setTransition(R.id.open_center_step2)
            motion.transitionToEnd()
            motion.awaitTransitionComplete(R.id.center_step2)
            state = State.OPEN
        }

        params.flags =
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        windowManager.updateViewLayout(relative, params)
    }

    fun close(handler: () -> Unit = {}) {
        if (state == State.CLOSE) return

        state = State.Animated

        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        windowManager.updateViewLayout(relative, params)

        scope.launch {
            motion.setTransition(R.id.close_center)
            motion.transitionToEnd()
            motion.awaitTransitionComplete(R.id.start_center)
            state = State.CLOSE
            handler()
        }
    }

    fun moveAt(x: Int = 0, y: Int = 0) {
        params.x = x
        params.y = y
        windowManager.updateViewLayout(relative, params)
    }


    fun setonClickListenerExit(handler: () -> Unit) {
        motion.findViewById<ImageButton>(R.id.exit).setOnClickListener {
            close(handler)
        }
    }

    fun setonClickListenerGallery(handler: () -> Unit) {
        motion.findViewById<ImageButton>(R.id.data).setOnClickListener {
            close(handler)
        }
    }

    fun setonClickListenerScreen(handler: () -> Unit) {
        motion.findViewById<ImageButton>(R.id.screen).setOnClickListener {
            close(handler)
        }
    }

    fun setOnClickListenerDone(handler: () -> Unit) {
        motion.findViewById<ImageButton>(R.id.done).setOnClickListener {
            handler()
        }
    }

    fun setOnClickListenerCrop(handler: () -> Unit) {
        motion.findViewById<ImageButton>(R.id.crop).setOnClickListener {
            handler()
        }
    }

    fun setonClickListenerCamera(handler: () -> Unit) {
        motion.findViewById<ImageButton>(R.id.stop).setOnClickListener {
            //my anim
            state = State.Animated
            scope.launch {
                handler()
//                motion.setTransition(R.id.close_center)
//                motion.transitionToEnd()
//                motion.awaitTransitionComplete(R.id.start_center)
                motion.setTransition(R.id.click_center_screen)
                motion.transitionToEnd()
                motion.awaitTransitionComplete(R.id.center_click_screen)
                state = State.OPENCSREEN
            }
        }
    }

    fun destroy() {
        scope.cancel()
        windowManager.removeView(relative)
    }

    fun update() {
        windowManager.updateViewLayout(relative, params)
    }

}