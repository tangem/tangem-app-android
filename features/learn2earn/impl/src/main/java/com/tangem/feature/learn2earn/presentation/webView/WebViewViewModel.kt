package com.tangem.feature.learn2earn.presentation.webView

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.tangem.feature.learn2earn.domain.HeadersConverter
import com.tangem.feature.learn2earn.domain.api.WebViewRedirectHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
[REDACTED_AUTHOR]
 */
@HiltViewModel
internal class WebViewViewModel @Inject constructor(
    handler: WebViewRedirectHandler,
) : ViewModel(), WebViewRedirectHandler by handler {

    fun extractData(intent: Intent): WebViewData {
        val uriString = requireNotNull(intent.getStringExtra(Learn2earnWebViewActivity.EXTRA_WEB_URI)) {
            "The Intent property EXTRA_WEB_URI is null"
        }
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