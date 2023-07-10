package com.tangem.feature.learn2earn.presentation.webView

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tangem.feature.learn2earn.domain.api.WebViewAction
import com.tangem.feature.learn2earn.domain.api.WebViewRedirectHandler
import timber.log.Timber

internal class Learn2earnWebViewClient(
    private val redirectHandler: WebViewRedirectHandler,
    private val headers: Map<String, String>,
    private val finishSessionHandler: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        Timber.d("url: ${request.url}")
        when (redirectHandler.handleRedirect(request.url)) {
            WebViewAction.NOTHING -> Unit
            WebViewAction.PROCEED -> {
                val mergedHeaders = headers.toMutableMap()
                request.requestHeaders?.let { mergedHeaders.putAll(it) }
                view.loadUrl(request.url.toString(), mergedHeaders)
            }
            WebViewAction.FINISH_SESSION -> {
                finishSessionHandler.invoke()
            }
        }
        return true
    }
}