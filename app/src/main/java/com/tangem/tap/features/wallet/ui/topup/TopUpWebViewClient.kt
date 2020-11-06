package com.tangem.tap.features.wallet.ui.topup

import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store

class TopUpWebViewClient(
        private val progressBar: ProgressBar, private val redirectUrl: String?
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (redirectUrl != null && url.contains(redirectUrl)) {
            store.dispatch(WalletAction.TopUpAction.Finish(true))
            return true
        }
        view.loadUrl(url)
        return true
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        progressBar.hide()
    }

    init {
        progressBar.show()
    }
}