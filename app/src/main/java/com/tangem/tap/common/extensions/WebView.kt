package com.tangem.tap.common.extensions

import android.webkit.WebView

fun WebView.stop() {
    stopLoading()
    pauseTimers()
}