package com.tangem.tap.features.sprinklr.ui.webview

import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebContent

internal class SprinklrWebViewClient(
    private val onNewUrl: WebContent.(String) -> WebContent,
) : AccompanistWebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // If the url hasn't changed, this is probably an internal event like
        // a javascript reload. We should let it happen.
        if (view?.url == request?.url.toString()) {
            return false
        }

        request?.let {
            state.content = onNewUrl(state.content, it.url.toString())
        }
        return true
    }
}
