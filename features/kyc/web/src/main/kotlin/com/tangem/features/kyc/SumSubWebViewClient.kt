package com.tangem.features.kyc

import android.webkit.WebView
import android.webkit.WebViewClient

internal class SumSubWebViewClient(private val accessToken: String) : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        view?.evaluateJavascript("initSumsub('$accessToken');", null)
    }
}