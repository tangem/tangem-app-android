package com.tangem.features.marketing.impl.promopage.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.tangem.common.uri.PromoPageUrlValidator
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.webview.applySafeSettings
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private val PAGE_LOAD_TIMEOUT = 15.seconds

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PromoPageScreen(
    url: String,
    onCloseClick: () -> Unit,
    onLoadError: (reason: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(PAGE_LOAD_TIMEOUT)
        if (isLoading) onLoadError("timeout")
    }

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            TangemTopNavigation(endButton = { TangemButton.Close(onClick = onCloseClick) })
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        // Transparent until the page shows up: an empty WebView paints its white past its bounds
                        setBackgroundColor(Color.TRANSPARENT)
                        applySafeSettings()
                        // Promo pages pick the content to show with a script and stay blank without it
                        settings.javaScriptEnabled = true
                        webViewClient = PromoPageWebViewClient(
                            onPageVisible = { isLoading = false },
                            onLoadError = onLoadError,
                        )
                        if (PromoPageUrlValidator.isAllowed(url.toUri())) loadUrl(url)
                    }
                },
                onRelease = WebView::destroy,
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading) TangemLoader(size = TangemLoaderSize.X32)
        }
    }
}

private class PromoPageWebViewClient(
    private val onPageVisible: () -> Unit,
    private val onLoadError: (reason: String) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        request.isForMainFrame && !PromoPageUrlValidator.isAllowed(request.url)

    override fun onPageCommitVisible(view: WebView, url: String) {
        // POST navigations bypass shouldOverrideUrlLoading
        if (!PromoPageUrlValidator.isAllowed(url.toUri())) {
            onLoadError("page is not allowed")
            return
        }
        view.setBackgroundColor(Color.WHITE)
        onPageVisible()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) onLoadError("error ${error.errorCode}")
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) onLoadError("HTTP ${errorResponse.statusCode}")
    }

    // Unhandled, a crashed or killed renderer takes the app down with it
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onLoadError("renderer gone")
        return true
    }
}