package com.sychev.assistantapp.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.sychev.assistantapp.R
import com.sychev.assistantapp.ui.Assistant
import com.sychev.assistantapp.ui.MainActivityViewModel
import com.sychev.assistantapp.ui.STOP_FOREGROUND_SERVICE
import com.sychev.assistantapp.ui.TAG

const val CHANNEL_ID = "my_channel_id"

@RequiresApi(Build.VERSION_CODES.Q)
class AssistantService: Service() {
    private var mResultCode: Int = 0
    private var mData: Intent? = null
    private var assistant: Assistant? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }



    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Assistant = $assistant")
        if (intent.action == STOP_FOREGROUND_SERVICE) {
            assistant?.close()
            assistant = null
            stopForeground(true)
            stopSelfResult(startId)
        } else {
            mResultCode = intent.getIntExtra("result_code", 0)
            mData = intent.getParcelableExtra("data")
            createNotificationChannel()

            startForeground(
                1,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mProjection = projectionManager.getMediaProjection(mResultCode, mData!!)
//
            assistant = Assistant(
                context = applicationContext,
                mProjection
            )
            assistant?.open()
            Log.d(TAG, "onStartCommand: Assistant = $assistant")
        }

        return START_STICKY

    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: called inside service")

    }

    private fun createNotification(): Notification {
        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_1)
            .build()
        return notification
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ChannelName"
            val descriptionText = "Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


}