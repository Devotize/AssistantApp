package com.sychev.assistantapp.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.sychev.assistantapp.R
import com.sychev.assistantapp.presentation.activity.main_activity.TAG
import com.sychev.assistantapp.presentation.adapter.CardSwipeAdapter
import com.sychev.assistantapp.presentation.assistant.Assistant
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class ScreenshotComponent(
    private val context: Context,
    private val windowManager: WindowManager,
    private val assistant: Assistant
) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView = layoutInflater.inflate(R.layout.screenshot_layout, null)
    private val topLayout = rootView.findViewById<FrameLayout>(R.id.top_overlay_screenshot_layout)
    private val cardAdapter = CardSwipeAdapter(ArrayList<Bitmap>(), windowManager)
    private val cardManager = CardStackLayoutManager(context, object : CardStackListener{
        override fun onCardDragging(direction: Direction?, ratio: Float) {
            //ratio 0.6f = swipe
            if (ratio >= 0.6f) {
                when (direction){
                    Direction.Right -> {
                        topLayout.background = ContextCompat.getDrawable(context, R.drawable.green_right_side_gradient_drawable)
                    }
                    Direction.Left -> {
                        topLayout.background = ContextCompat.getDrawable(context, R.drawable.red_right_side_gradient_drawable)
                    }
                    Direction.Top -> {

                    }
                    Direction.Bottom -> {

                    }
                    else -> {
                        throw java.lang.Exception("There is no such  direction")
                    }
                }
            } else {
                topLayout.background = ColorDrawable(ContextCompat.getColor(context, R.color.transparent))
            }
        }

        override fun onCardSwiped(direction: Direction?) {
            when (direction) {
                Direction.Right -> {
                    assistant.fakePostRequest()
                }
                Direction.Left -> {
                    Log.d(TAG, "onCardSwiped: Left")
                }
                else -> {
                    throw Exception("something went wrong with directions")
                }
            }
            if (cardAdapter.screenshots.isEmpty()) {
                hide()
            } else {
                cardAdapter.screenshots.removeLast()
                if (cardAdapter.screenshots.isEmpty()) {
                    hide()
                }
            }
        }

        override fun onCardRewound() {

        }

        override fun onCardCanceled() {

        }

        override fun onCardAppeared(view: View?, position: Int) {
            topLayout.background = ColorDrawable(ContextCompat.getColor(context, R.color.transparent))
        }

        override fun onCardDisappeared(view: View?, position: Int) {

        }

    })
    private val swipeCardView = rootView.findViewById<CardStackView>(R.id.card_stack_view).apply {
        layoutManager = cardManager
    }
    private val layoutParams = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
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
        screenshot?.let{
            cardAdapter.screenshots.add(it)
            cardAdapter.notifyDataSetChanged()
        }
    }
    fun getScreenshot() = screenshot

    fun show() {
        if (rootView.parent == null) {
            windowManager.addView(rootView, layoutParams)
            initCardAdapter()
        }
    }

    fun removeCirclesForDetectedObject() {
        hide()
        show()
    }

    fun hide() {
        if (rootView.parent != null) {
            windowManager.removeView(rootView)
        }
    }

    private fun initCardAdapter() {
        swipeCardView.adapter = cardAdapter
    }

}















