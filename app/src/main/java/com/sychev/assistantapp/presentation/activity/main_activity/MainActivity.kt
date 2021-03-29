package com.sychev.assistantapp.presentation.activity.main_activity

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import com.sychev.assistantapp.repository.ClothesRepository
import com.sychev.assistantapp.utils.AssistantService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val TAG = "AppDebug"
const val ACTION_MANAGE_OVERLAY_PERMISSION_CODE = 123
const val PROJECTION_MANAGER_PERMISSION_CODE = 243
const val STOP_FOREGROUND_SERVICE = "STOP_FOREGROUND_SERVICE_ACTION"

class MainActivity : AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isActive = viewModel.isAssistantActive.value

            Column(
                modifier = Modifier.fillMaxSize()
                    .clickable {
                               Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
                    },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(onClick = {
                    viewModel.changeIsAssistantActive()
                    if (isActive){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val stopIntent = Intent(this@MainActivity, AssistantService::class.java)
                            stopIntent.action = STOP_FOREGROUND_SERVICE
                            startService(stopIntent)
                        } else {
                        viewModel.stopAssistant()
                        }
                    }else{
                        viewModel.mediaProjectorPermission(activity = this@MainActivity)
                    }

                }) {
                    val text = if (isActive) "Stop Assistant" else "StartAssistant"
                    Text(text = text)
                }
            }

        }
        viewModel.checkPermissionSystemWindow(activity = this)

        Log.d(TAG, "onCreate: observe called")


    }

    private fun Context.startAssistantService(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: called")
        if (resultCode == RESULT_OK && requestCode == PROJECTION_MANAGER_PERMISSION_CODE && data != null){
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Intent(applicationContext, AssistantService::class.java,).also {
                    Log.d(TAG, "onActivityResult: trying to start service")
                    it.putExtra("data", data)
                    it.putExtra("result_code", resultCode)
                    startAssistantService(it)
                }
//            } else {
//                val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//                val mProjection = projectionManager.getMediaProjection(resultCode, data)
//                viewModel.startAssistant(this, mProjection)
//            }

        } else {
            viewModel.mediaProjectorPermission(this)
        }

    }

}