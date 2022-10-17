package com.tangem.tap.common.extensions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Context.readFile(fileName: String): String =
    this.openFileInput(fileName).bufferedReader().readText()

fun Context.rewriteFile(content: String, fileName: String) {
    this.openFileOutput(fileName, Context.MODE_PRIVATE).use {
        it.write(content.toByteArray(), 0, content.length)
    }
}

fun Context.readAssetAsString(fileName: String): String {
    return this.assets.open("$fileName.json").bufferedReader().readText()
}

fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
