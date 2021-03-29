package com.sychev.assistantapp.presentation.components

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sychev.assistantapp.R
import com.sychev.assistantapp.domain.model.DetectedClothes
import com.sychev.assistantapp.network.model.DetectedClothesDto
import com.sychev.assistantapp.presentation.adapter.DetectedClothesAdapter
import com.sychev.assistantapp.presentation.assistant.Assistant

class DetectedClothesComponent(
    private val context: Context,
    private val windowManager: WindowManager,
    private val assistant: Assistant,
) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    private val rootView = layoutInflater.inflate(R.layout.detected_clothes_layout, null)
    private val rv = rootView.findViewById<RecyclerView>(R.id.detected_clothes_recycler_view)
    private val clothsButton = rootView.findViewById<ImageButton>(R.id.close_detected_clothes_button).apply {
        setOnClickListener { hide() }
    }
    private val windowParams = WindowManager.LayoutParams(
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

    private val detectedClothes = ArrayList<DetectedClothesDto>()

    fun setDetectedClothes(detectedClothes: ArrayList<DetectedClothesDto>){
        this.detectedClothes.addAll(detectedClothes)
    }

    private fun initRv() {
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = DetectedClothesAdapter(clothes = detectedClothes, object : DetectedClothesAdapter.OnItemClickListener{
            override fun onItemClick(item: DetectedClothesDto) {
                assistant.webViewComponent.setUrls(listOf("${item.url}"))
                assistant.webViewComponent.show()
                hide()
            }

        })

    }

    fun show() {
        if (rootView.parent == null){
            initRv()
            windowManager.addView(rootView, windowParams)
        }
    }

    fun hide() {
        if (rootView.parent != null){
            windowManager.removeView(rootView)
        }
    }

}
















