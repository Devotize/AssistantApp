package com.sychev.assistantapp.utils

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.sychev.assistantapp.R
import com.sychev.assistantapp.presentation.activity.main_activity.STOP_FOREGROUND_SERVICE
import com.sychev.assistantapp.presentation.activity.main_activity.TAG
import com.sychev.assistantapp.presentation.assistant.Assistant
import com.sychev.assistantapp.repository.ClothesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


const val CHANNEL_ID = "assistant_notification_id"
const val CHANNEL_NAME = "assistant_channel_name"
const val INTENT_COMMAND = "intent_command|"
const val INTENT_COMMAND_EXIT = "intent_command_exit"
const val CODE_EXIT_INTENT = 3213


@RequiresApi(Build.VERSION_CODES.Q)
@AndroidEntryPoint
class AssistantService : Service() {
    private var mResultCode: Int = 0
    private var mData: Intent? = null
    private var assistant: Assistant? = null
    @Inject
    lateinit var repository: ClothesRepository

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun stopService() {
        assistant?.close()
        assistant = null
        stopForeground(true)
        stopSelf()
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Assistant before = $assistant")
        val command = intent.getStringExtra(INTENT_COMMAND) ?: ""
        if (intent.action == STOP_FOREGROUND_SERVICE) stopService(). also { return START_NOT_STICKY }
        if (command == INTENT_COMMAND_EXIT) stopService().also { return START_NOT_STICKY }

        mResultCode = intent.getIntExtra("result_code", 0)
        mData = intent.getParcelableExtra("data")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(
                1,
                createNotification()
            )
        }


        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mProjection = projectionManager.getMediaProjection(mResultCode, mData!!)

        assistant = Assistant(
            context = applicationContext,
            mProjection,
            repository
        )
        assistant?.open()
        Log.d(TAG, "onStartCommand: Assistant after = $assistant")


        return START_STICKY

    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: called inside service")

    }

//    private fun createNotification(): Notification {
//        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
//            .setSmallIcon(R.drawable.icon_1)
//            .build()
//        return notification
//    }
//
//    private fun createNotificationChannel() {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "ChannelName"
//            val descriptionText = "Description"
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//
//            val notificationManager: NotificationManager =
//                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }

    private fun createNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val exitIntent = Intent(this, AssistantService::class.java).apply {
            putExtra(INTENT_COMMAND, INTENT_COMMAND_EXIT)
        }
        val exitPendingIntent = PendingIntent.getService(
            this, CODE_EXIT_INTENT, exitIntent, 0
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableLights(false)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                manager.createNotificationChannel(this)
            }
        }

        NotificationCompat.Builder(
            this,
            CHANNEL_ID
        ).apply {
            setTicker(null)
            setContentTitle("Assistant")
            setContentText("Notification for assistant")
            setAutoCancel(false)
            setOngoing(true)
            setWhen(System.currentTimeMillis())
            setSmallIcon(R.drawable.icon_1)
            priority = NotificationCompat.PRIORITY_DEFAULT
            addAction(
                NotificationCompat.Action(
                    0,
                    "EXIT",
                    exitPendingIntent
                )
            )
            return this.build()
            startForeground(
                0,
                this.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }

    }

}















