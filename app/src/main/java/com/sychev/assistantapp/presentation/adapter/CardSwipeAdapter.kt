package com.sychev.assistantapp.presentation.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.sychev.assistantapp.R
import com.sychev.assistantapp.presentation.activity.main_activity.TAG
import com.sychev.assistantapp.presentation.components.FrameDrawComponent

class CardSwipeAdapter(
    val screenshots: ArrayList<Bitmap>,
    val windowManager: WindowManager,
): RecyclerView.Adapter<CardSwipeAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Log.d(TAG, "onCreateViewHolder: called")
        val layoutInflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val screenshotLayout = layoutInflater.inflate(R.layout.screenshot_item, parent, false)
        return MyViewHolder(screenshotLayout, windowManager)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(screenshots[position])
    }

    override fun getItemCount(): Int {
        return screenshots.size
    }

    class MyViewHolder(
        itemView: View,
        val windowManager: WindowManager,
    ): RecyclerView.ViewHolder(itemView){
        private val context = itemView.context
        private val screenshotImageView = itemView.findViewById<ImageView>(R.id.screenshot_image_view)
        private val faceButton = itemView.findViewById<ImageButton>(R.id.face_button)
        private val cropButton = itemView.findViewById<ImageButton>(R.id.crop_button)
        private val screenshotFrame = itemView.findViewById<FrameLayout>(R.id.screenshot_frame)
        private val progressBar = itemView.findViewById<ProgressBar>(R.id.progress_bar_screenshot_item)
        private val screenshotContainer = itemView.findViewById<FrameLayout>(R.id.screenshot_container)
        private var inCropMode = false
        private val frameDrawComponent = FrameDrawComponent(context = context, windowManager = windowManager)

        fun bind(
            screenshot: Bitmap,
        ) {
            Glide.with(itemView)
                .asBitmap()
                .load(screenshot)
                .fitCenter()
                .into(screenshotImageView)

            faceButton.setOnClickListener {
                screenshotFrame.removeAllViews()
                detectFaces(bitmap = screenshot)
            }

            cropButton.setOnClickListener { it as ImageButton
                inCropMode = !inCropMode
                if (inCropMode) {
                    it.setImageResource(R.drawable.ic_baseline_done_24)
                    val params = WindowManager.LayoutParams(
                        screenshotFrame.width,
                        screenshotFrame.height,
                        0,
                        -93,
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
                    frameDrawComponent.setLayoutParams(params = params)
                    frameDrawComponent.boundingBox.setLayoutParams(params)
                    frameDrawComponent.show()
                } else {
                    if (frameDrawComponent.boundingBox.isShowing){
                        Log.d(TAG, "bind: container margin: start ${screenshotContainer.marginStart}, end ${screenshotContainer.marginEnd}")
                        val horizontalOffset = screenshotContainer.marginStart
                        val verticalOffset = screenshotContainer.marginTop
                        val croppedBtm = frameDrawComponent.cropBitmap(
                            screenshot,
                            screenshotImageView.width,
                            screenshotImageView.height,
                            frameDrawComponent.boundingBox.left + horizontalOffset + 20,
                            frameDrawComponent.boundingBox.top + 70 + verticalOffset,
                            frameDrawComponent.boundingBox.right + horizontalOffset + 20,
                            frameDrawComponent.boundingBox.bottom + 70 + verticalOffset,
                        )
                        Glide.with(itemView)
                            .asBitmap()
                            .load(croppedBtm)
                            .into(screenshotImageView)
                    }
                    frameDrawComponent.hide()
                    frameDrawComponent.boundingBox.hide()
                    it.setImageResource(R.drawable.ic_crop)
                }

            }

        }

        private fun detectFaces(bitmap: Bitmap) {
            progressBar.visibility = View.VISIBLE
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = FaceDetection.getClient().process(inputImage)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        val horizontalOffset = (context.resources.displayMetrics.widthPixels - screenshotFrame.width)
                        val verticalOffset = (context.resources.displayMetrics.heightPixels - screenshotFrame.height)
                        Log.d(TAG, "detectFaces: horizontalOffset; $horizontalOffset, verticalOffset: $verticalOffset")
                        Log.d(TAG, "detectFaces: left: ${face.boundingBox.left}, right: ${face.boundingBox.right}")
                        val boundingBox = Rect(
                            face.boundingBox.left - horizontalOffset,
                            face.boundingBox.top - verticalOffset / 2,
                            face.boundingBox.right - horizontalOffset,
                            face.boundingBox.bottom - verticalOffset / 2,
                        )
                        addCircleForDetectedObject(
                            boundingBox = boundingBox,
                            onClick = {
                                Log.d(TAG, "detectFaces: clicked on $face")
                            })
                    }
                }
                .addOnFailureListener {
                    Log.d(TAG, "detectFaces: Failure: $it")
                }
                .addOnCompleteListener {
                    progressBar.visibility = View.GONE
                }
        }
        fun addCircleForDetectedObject(boundingBox: Rect, onClick: () -> Unit){
            val x = boundingBox.exactCenterX().toInt()
            val y = boundingBox.exactCenterY().toInt()

            val params = FrameLayout.LayoutParams(
                35,
                35,
            )
            params.leftMargin = x
            params.topMargin = y


            val circleView = Button(context)
            circleView.background = ContextCompat.getDrawable(context, R.drawable.orange_circle_shape)
            circleView.setOnClickListener {
                onClick()
            }
            screenshotFrame.addView(circleView, params)
        }
    }

}
