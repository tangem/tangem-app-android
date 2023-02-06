package com.tangem.tap.common.extensions

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.AnyRes
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

/**
 * Get uri to any resource type via given Resource Instance
 * @param resId - resource id
 * @throws Resources.NotFoundException if the given ID does not exist.
 * @return - Uri to resource by the given ID
 */
@Throws(Resources.NotFoundException::class)
fun Context.resourceUri(@AnyRes resId: Int): Uri {
    return Uri.parse(
        ContentResolver.SCHEME_ANDROID_RESOURCE +
            "://" + resources.getResourcePackageName(resId) +
            '/' + resources.getResourceTypeName(resId) +
            '/' + resources.getResourceEntryName(resId),
    )
}
