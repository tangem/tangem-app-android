package com.tangem.feature.learn2earn.presentation.webView

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tangem.feature.learn2earn.domain.api.RedirectConsequences
import com.tangem.feature.learn2earn.domain.api.WebViewRedirectHandler
import timber.log.Timber

internal class Learn2earnWebViewClient(
    private val helper: WebViewRedirectHandler,
    private val headers: Map<String, String>,
    private val finishSessionHandler: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        Timber.d("url: ${request.url}")
        when (helper.handleRedirect(request.url)) {
            RedirectConsequences.PROCEED -> {
                val mergedHeaders = headers.toMutableMap()
                request.requestHeaders?.let { mergedHeaders.putAll(it) }
                view.loadUrl(request.url.toString(), mergedHeaders)
            }
            RedirectConsequences.FINISH_SESSION -> {
                finishSessionHandler.invoke()
            }
            else -> Unit
        }
        return true
    }
}