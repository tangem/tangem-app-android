package com.tangem.tap.common

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

private val uiHandler = Handler(Looper.getMainLooper())
private val backgroundHandler = Handler(HandlerThread("AppMainHandlerThread").apply { start() }.looper)

fun postUi(ms: Long = 0, func: Runnable) {
    if (ms == 0L) uiHandler.post { func.run() } else uiHandler.postDelayed(func, ms)
}

fun postBackground(ms: Long = 0, func: Runnable) {
    if (ms == 0L) backgroundHandler.post { func.run() } else backgroundHandler.postDelayed(func, ms)
}

fun post(ms: Long = 0, func: Runnable) {
    if (ms == 0L) {
        func.run()
    } else {
        val currentLooper = Looper.myLooper() ?: return

        Handler(currentLooper).postDelayed(func, ms)
    }
}