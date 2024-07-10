package com.tangem.features.disclaimer.impl.ui

import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.runtime.MutableState

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

internal class DisclaimerWebViewClient(private val progressState: MutableState<ProgressState>) : WebViewClient() {

    private var loadingUrl: String? = null
    private var loadedUrl: String? = null

    fun reset() {
        loadingUrl = null
        loadedUrl = null
        progressState.value = ProgressState.Loading
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        view?.injectCSS()
        super.onPageStarted(view, url, favicon)

        if (loadingUrl != url && progressState.value != ProgressState.Error) progressState.value = ProgressState.Loading
        loadingUrl = url
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.injectCSS()
        super.onPageFinished(view, url)

        if (loadedUrl != url && progressState.value != ProgressState.Error) progressState.value = ProgressState.Done
        loadedUrl = url
    }

    override fun onReceivedError(view: WebView?, resourceRequest: WebResourceRequest?, error: WebResourceError?) {
        view?.injectCSS()
        super.onReceivedError(view, resourceRequest, error)
        error?.let { progressState.value = ProgressState.Error }
    }

    override fun onReceivedHttpError(
        view: WebView?,
        resourceRequest: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        view?.injectCSS()
        super.onReceivedHttpError(view, resourceRequest, errorResponse)

        if (resourceRequest != null && errorResponse != null) {
            val isDifferentUrl = resourceRequest.url?.toString() != loadingUrl
            val isSuccessCode = errorResponse.statusCode < RESPONSE_USER_ERROR_STATUS_CODE
            val isNotDone = progressState.value != ProgressState.Done
            if (isDifferentUrl || isSuccessCode || isNotDone) return
            progressState.value = ProgressState.Error
        }
    }

    companion object {
        private const val RESPONSE_USER_ERROR_STATUS_CODE = 400
    }
}