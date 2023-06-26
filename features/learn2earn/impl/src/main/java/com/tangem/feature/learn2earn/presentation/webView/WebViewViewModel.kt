package com.tangem.feature.learn2earn.presentation.webView

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.tangem.feature.learn2earn.domain.HeadersConverter
import com.tangem.feature.learn2earn.domain.api.RedirectConsequences
import com.tangem.feature.learn2earn.domain.api.WebViewRedirectHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
[REDACTED_AUTHOR]
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    private val handler: WebViewRedirectHandler,
) : ViewModel(), WebViewRedirectHandler {

    override fun handleRedirect(uri: Uri): RedirectConsequences {
        return handler.handleRedirect(uri)
    }

    fun extractData(intent: Intent): WebViewData {
        val uriString = intent.getStringExtra(Learn2earnWebViewActivity.EXTRA_WEB_URI)!!
        val rawHeaders = intent.getStringArrayListExtra(Learn2earnWebViewActivity.EXTRA_WEB_HEADERS) ?: ArrayList()

        return WebViewData(
            uri = Uri.parse(uriString),
            headers = HeadersConverter().convertBack(rawHeaders),
        )
    }
}

data class WebViewData(
    val uri: Uri,
    val headers: Map<String, String>,
)