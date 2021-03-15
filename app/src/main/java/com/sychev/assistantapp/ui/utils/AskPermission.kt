package com.sychev.assistantapp.ui.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


fun askPermission(
    context: Activity,
    permission: String,
    permCode: Int,
    handler: () -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            permission
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(permission),
            permCode
        )
    } else {
        handler()
    }
}

fun askPermissions(
    context: Activity,
    arrayOf: Array<String>,
    permCode: Int,
    handler: () -> Unit
) {
    val list = mutableListOf<String>()
    arrayOf.filter {
        ContextCompat.checkSelfPermission(
            context,
            it
        ) != PackageManager.PERMISSION_GRANTED
    }.forEach { list.add(it) }

    if (list.isNotEmpty()) {
        ActivityCompat.requestPermissions(
            context,
            list.toTypedArray(),
            permCode
        )
    } else {
        handler()
    }
}
