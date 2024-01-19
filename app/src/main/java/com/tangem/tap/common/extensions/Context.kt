package com.tangem.tap.common.extensions

import android.content.*
import android.content.pm.*
import android.content.res.*
import android.net.*
import androidx.annotation.*
import androidx.core.content.*

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