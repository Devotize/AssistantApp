package com.sychev.assistantapp.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sychev.assistantapp.R
import com.sychev.assistantapp.utils.loadPicture

@Composable
fun AssistantIcon() {
    Surface {
        val image = loadPicture(res = R.drawable.icon_1).value
        image?.let {
            IconButton(
                    modifier = Modifier
                            .preferredWidth(100.dp)
                            .preferredHeight(100.dp),
                    onClick = {

            }) {
                Image(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds
                )
            }
        }

    }
}