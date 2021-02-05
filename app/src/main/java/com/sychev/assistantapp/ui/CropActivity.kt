package com.sychev.assistantapp.ui

import android.content.Intent
import android.graphics.*
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sychev.assistantapp.R
import com.sychev.assistantapp.utils.loadPicture
import com.theartofdev.edmodo.cropper.CropImage

class CropActivity: AppCompatActivity() {

    private val viewModel: MainActivityViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.extras?.getString("screenshot")?.let{ fileName ->
            val uri = Uri.fromFile(getFileStreamPath(fileName))
            viewModel.launchImageCrop(uri, this)
            Log.d(TAG, "onCreate: uri = $uri")


//            val inputStream = openFileInput(fileName)
//            BitmapFactory.decodeStream(inputStream).let{
//                viewModel.setCroppedScreenshot(it)
//
//            }
        }

        setContent {
            val screenshot = viewModel.croppedScreenshot.value
            val detectedObjects = viewModel.detectedObjects.value
            val loading = viewModel.loading.value

            Column(
                    modifier = Modifier.fillMaxSize(),
            ) {
                screenshot?.let { screenshot ->
                    val img = loadPicture(bitmap = screenshot).value
                    img?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.CenterHorizontally),
                        ) {
                            Image(
                                modifier = Modifier
                                    .wrapContentSize(),
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null
                            )
                            detectedObjects?.let{detectedObjects ->
                                if (detectedObjects.isNotEmpty()) {
                                    Canvas(modifier = Modifier.matchParentSize(), ) {
                                        detectedObjects.forEach { detectedObject ->
                                            drawCircle(
                                                    color = Color.Red,
                                                    center = Offset(
                                                            detectedObject.boundingBox.exactCenterX(),
                                                            detectedObject.boundingBox.exactCenterY(),
                                                    ),
                                                    radius = 30f
                                            )
                                            val paint = Paint().asFrameworkPaint()
                                            drawIntoCanvas { canvas ->
                                                paint.apply {
                                                    textSize = 62f
                                                    color = ContextCompat.getColor(applicationContext, R.color.orange_700)
                                                }
                                                val text = if (detectedObject.labels.isNotEmpty())
                                                    detectedObject.labels[0].text
                                                else
                                                    "Object"
                                                canvas.nativeCanvas.drawText(text, detectedObject.boundingBox.exactCenterX() - 50, detectedObject.boundingBox.exactCenterY() + 100f, paint)
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                            text = "No Objects Found",
                                            modifier = Modifier
                                                    .align(Alignment.Center),
                                            color = Color.Red,
                                            style = MaterialTheme.typography.h5
                                    )
                                }
                            }

                        }
                    }
                }
                Spacer(modifier = Modifier.padding(20.dp))
                Button(onClick = {
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    startActivity(intent)
                },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(text = "To Main Activity")
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK){
                val uri = result.uri
                val bitmap = if (Build.VERSION.SDK_INT < 28)
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                else
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                viewModel.setCroppedScreenshot(bitmap)
                viewModel.detectObjects(bitmap)
                Log.d(TAG, "onActivityResult: uri = $uri")
            }else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                val error = result.error
                Log.d(TAG, "onActivityResult: $error")
            }
        }
    }

}