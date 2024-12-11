package com.tangem.core.ui.webview

import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Applies set of settings to prevent base security issues
 *
[REDACTED_AUTHOR]
 */
fun WebView.applySafeSettings() {
    settings.apply {
        // disable to use scripts
        javaScriptEnabled = false

        // disable access to files
        allowFileAccess = false

        // disable access to content by system content provider
        allowContentAccess = false

        // disable to use cached data (scripts, files, etc)
        cacheMode = WebSettings.LOAD_NO_CACHE

        // disable to use local storage
        domStorageEnabled = false
    }

    setDownloadListener(null)
}