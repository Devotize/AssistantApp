package com.sychev.assistantapp.ui

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import com.google.mlkit.vision.objects.DetectedObject
import java.util.*


@SuppressLint("ViewConstructor")
class DetectorRectangleView(context: Context, detectedObject: DetectedObject?) : View(context) {
    var points = arrayOfNulls<Point>(4)


    var groupId = -1
    private val colorballs = ArrayList<ColorBall>()

    // array that holds the balls
    private var balID = 0

    // variable to know what ball is being dragged
    private val paint: Paint = Paint()
    private val myLeft = detectedObject?.boundingBox?.left ?: 400
    private val myRight = detectedObject?.boundingBox?.right ?: 400
    private val myTop = detectedObject?.boundingBox?.top ?: 400
    private val myBottom= detectedObject?.boundingBox?.bottom ?: 400

    init {
        isFocusable = true

        points[0] = Point()
        points[0]!!.x = myLeft
        points[0]!!.y = myTop
        points[1] = Point()
        points[1]!!.x = myLeft
        points[1]!!.y = myBottom
        points[2] = Point()
        points[2]!!.x = myRight
        points[2]!!.y = myBottom
        points[3] = Point()
        points[3]!!.x = myRight
        points[3]!!.y = myTop
        balID = 2
        groupId = 1
        // declare each ball with the ColorBall class
        for (pt in points) {
            colorballs.add(ColorBall(context, R.drawable.ic_menu_crop, pt))
        }
    }

    // the method that draws the balls
    override fun onDraw(canvas: Canvas) {
        if (points[3] == null) //point4 null when user did not touch and move on screen.
            return
        var left: Int
        var top: Int
        var right: Int
        var bottom: Int
        left = points[0]!!.x
        top = points[0]!!.y
        right = points[0]!!.x
        bottom = points[0]!!.y
        for (i in 1 until points.size) {
            left = if (left > points[i]!!.x) points[i]!!.x else left
            top = if (top > points[i]!!.y) points[i]!!.y else top
            right = if (right < points[i]!!.x) points[i]!!.x else right
            bottom = if (bottom < points[i]!!.y) points[i]!!.y else bottom
        }
        paint.isAntiAlias = true
        paint.isDither = true
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = 5f

        //draw stroke
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#f47100")
        paint.strokeWidth = 2f
        canvas.drawRect(
            (
                    left + colorballs[0].widthOfBall / 2).toFloat(), (
                    top + colorballs[0].widthOfBall / 2).toFloat(), (
                    right + colorballs[2].widthOfBall / 2).toFloat(), (
                    bottom + colorballs[2].widthOfBall / 2).toFloat(), paint
        )
        //fill the rectangle
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#66000000")
        paint.strokeWidth = 0f
        canvas.drawRect(
            (
                    left + colorballs[0].widthOfBall / 2).toFloat(), (
                    top + colorballs[0].widthOfBall / 2).toFloat(), (
                    right + colorballs[2].widthOfBall / 2).toFloat(), (
                    bottom + colorballs[2].widthOfBall / 2).toFloat(), paint
        )

        //draw the corners
        // draw the balls on the canvas
        paint.color = Color.BLUE
        paint.textSize = 18f
        paint.strokeWidth = 0f
        for (i in colorballs.indices) {
            val ball = colorballs[i]
            canvas.drawBitmap(
                ball.bitmap, ball.x.toFloat(), ball.y.toFloat(),
                paint
            )
            canvas.drawText("" + (i + 1), ball.x.toFloat(), ball.y.toFloat(), paint!!)
        }
    }

    // events when touching the screen
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eventaction = event.action
        val X = event.x.toInt()
        val Y = event.y.toInt()
        when (eventaction) {
            MotionEvent.ACTION_DOWN -> {
                //resize rectangle
                balID = -1
                groupId = -1
                var i = colorballs.size - 1
                while (i >= 0) {
                    val ball = colorballs[i]
                    val centerX = ball.x + ball.widthOfBall
                    val centerY = ball.y + ball.heightOfBall
                    paint.color = Color.CYAN
                    // calculate the radius from the touch to the center of the
                    // ball
                    val radCircle = Math
                        .sqrt(
                            ((centerX - X) * (centerX - X) + (centerY - Y)
                                    * (centerY - Y)).toDouble()
                        )
                    if (radCircle < ball.widthOfBall) {
                        balID = ball.iD
                        groupId = if (balID == 1 || balID == 3) {
                            2
                        } else {
                            1
                        }
                        invalidate()
                        break
                    }
                    invalidate()
                    i--
                }
            }
            MotionEvent.ACTION_MOVE -> if (balID > -1) {
                // move the balls the same as the finger
                colorballs[balID].x = X
                colorballs[balID].y = Y
                paint.color = Color.CYAN
                if (groupId == 1) {
                    colorballs[1].x = colorballs[0].x
                    colorballs[1].y = colorballs[2].y
                    colorballs[3].x = colorballs[2].x
                    colorballs[3].y = colorballs[0].y
                } else {
                    colorballs[0].x = colorballs[1].x
                    colorballs[0].y = colorballs[3].y
                    colorballs[2].x = colorballs[3].x
                    colorballs[2].y = colorballs[1].y
                }
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
            }
        }
        invalidate()
        return true
    }

    class ColorBall(context: Context, @DrawableRes resourceId: Int, point: Point?) {
        var bitmap: Bitmap
        var mContext: Context
        var point: Point?
        var iD: Int
        val widthOfBall: Int
            get() = bitmap.width
        val heightOfBall: Int
            get() = bitmap.height
        var x: Int
            get() = point!!.x
            set(x) {
                point!!.x = x
            }
        var y: Int
            get() = point!!.y
            set(y) {
                point!!.y = y
            }

        companion object {
            var count = 0
        }

        init {
            iD = count++
            bitmap = BitmapFactory.decodeResource(
                context.resources,
                resourceId
            )
            mContext = context
            this.point = point
        }
    }
}