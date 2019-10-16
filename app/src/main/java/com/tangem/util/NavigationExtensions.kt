package com.tangem.util

import android.os.Bundle
import android.util.Log
import androidx.annotation.IdRes
import androidx.navigation.NavController

fun NavController.navigateSafely(@IdRes destination: Int, data: Bundle? = null) {
    try {
        this.navigate(destination, data)
    } catch (e: IllegalArgumentException) {
        Log.w(this::class.java.simpleName, e.message)
    } catch (e: IllegalStateException) {
        Log.w(this::class.java.simpleName, e.message)
    }
}