package com.tangem.tap.common.extensions

import android.webkit.WebView

/**
* [REDACTED_AUTHOR]
 */
fun WebView.configureSettings() {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
    }
}

fun WebView.stop() {
    stopLoading()
    pauseTimers()
    destroy()
}
