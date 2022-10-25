package com.tangem.tap.common

import android.content.Intent

/**
* [REDACTED_AUTHOR]
 */
interface ActivityResultCallbackHolder {
    fun addOnActivityResultCallback(callback: OnActivityResultCallback)
    fun removeOnActivityResultCallback(callback: OnActivityResultCallback)
}

typealias OnActivityResultCallback = (Int, Int, Intent?) -> Unit
