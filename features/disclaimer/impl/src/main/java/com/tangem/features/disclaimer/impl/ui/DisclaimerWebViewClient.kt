package com.tangem.features.disclaimer.impl.ui

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.google.accompanist.web.AccompanistWebViewClient
import timber.log.Timber

/**
 * [AccompanistWebViewClient] for Disclaimer [WebView]
 *
 * @property onLoadingFinished lambda be invoked when client will get a result of loading (content or error)
 *
[REDACTED_AUTHOR]
 */
internal class DisclaimerWebViewClient(
    private val onLoadingFinished: () -> Unit,
) : AccompanistWebViewClient() {

    private val loadedUrls: MutableMap<String, Boolean> = mutableMapOf()

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        Timber.d("onPageStarted: $url")

        if (url != null) {
            loadedUrls[url] = false
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        Timber.d("onPageFinished: $url")

        if (url != null && loadedUrls.containsKey(url)) {
            loadedUrls[url] = true
            onLoadingFinished()
        }
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)

        val url = request?.url?.toString()

        Timber.d("onReceivedError: $url")

        if (url != null && loadedUrls.containsKey(url)) {
            loadedUrls[url] = true
            onLoadingFinished()
        }
    }
}