package com.tangem.features.disclaimer.impl.ui

import android.graphics.Bitmap
import android.webkit.WebView
import com.google.accompanist.web.AccompanistWebViewClient

internal enum class ProgressState {
    Loading,
    Done,
    Error,
}

/**
 * Workaround to display web view with ToS only in dark theme
 */
private fun WebView.injectCSS() {
    val code = "javascript:(function() {" +
        "var node = document.createElement('style');" +
        "node.type = 'text/css';" +
        " node.innerHTML = 'body, label,th,p,a, td, tr,li,ul,span,table,h1,h2,h3,h4,h5,h6,h7,div,small {" +
        "     color: #C9C9C9;" +
        "background-color: #1E1E1E;" +
        " } ';" +
        " document.head.appendChild(node);})();"

    evaluateJavascript(code, null)
}

internal class DisclaimerWebViewClient : AccompanistWebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        view?.injectCSS()
        super.onPageStarted(view, url, favicon)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        view?.injectCSS()
        super.onPageCommitVisible(view, url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.injectCSS()
        super.onPageFinished(view, url)
    }
}