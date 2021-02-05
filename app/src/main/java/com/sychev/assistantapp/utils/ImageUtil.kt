package com.sychev.assistantapp.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.AmbientContext
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

@Composable
fun loadPicture(@DrawableRes res: Int): MutableState<Bitmap?> {
    val picture: MutableState<Bitmap?> = mutableStateOf(null)
    Glide.with(AmbientContext.current)
            .asBitmap()
            .load(res)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    picture.value = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })

    return picture
}

@Composable
fun loadPicture(bitmap: Bitmap): MutableState<Bitmap?> {
    val picture: MutableState<Bitmap?> = mutableStateOf(null)
    Glide.with(AmbientContext.current)
            .asBitmap()
            .load(bitmap)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    picture.value = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })

    return picture
}