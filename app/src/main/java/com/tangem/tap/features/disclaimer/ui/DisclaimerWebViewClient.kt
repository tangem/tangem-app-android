package com.tangem.tap.features.disclaimer.ui

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.tangem.common.extensions.ifNotNull
import com.tangem.tap.features.wallet.redux.ProgressState

class DisclaimerWebViewClient : WebViewClient() {

    var onProgressStateChanged: ((ProgressState) -> Unit)? = null

    private var loadingUrl: String? = null
    private var loadedUrl: String? = null

    private var progressState: ProgressState = ProgressState.Loading
        set(value) {
            field = value
            onProgressStateChanged?.invoke(value)
        }

    fun reset() {
        loadingUrl = null
        loadedUrl = null
        progressState = ProgressState.Loading
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        if (loadingUrl != url) progressState = ProgressState.Loading
        loadingUrl = url
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        if (loadedUrl != url) progressState = ProgressState.Done
        loadedUrl = url
    }

    override fun onReceivedError(
        view: WebView?,
        resourceRequest: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, resourceRequest, error)
        error?.let { progressState = ProgressState.Error }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        resourceRequest: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, resourceRequest, errorResponse)

        ifNotNull(resourceRequest, errorResponse) { request, response ->
            if (request.url?.toString() != loadingUrl || response.statusCode < RESPONSE_USER_ERROR_STATUS_CODE) return
            if (progressState != ProgressState.Done) return

            progressState = ProgressState.Error
        }
    }

    companion object {
        private const val RESPONSE_USER_ERROR_STATUS_CODE = 400
    }
}
