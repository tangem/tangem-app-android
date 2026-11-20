package com.tangem.features.marketing.impl.promopage.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.http.SslError
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
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
import androidx.core.view.isInvisible
import com.tangem.common.uri.PromoPageUrlValidator
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.webview.applySafeSettings
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private val PAGE_LOAD_TIMEOUT = 15.seconds
private const val MIME_TYPE_HTML = "text/html"
private const val CHARSET_UTF8 = "UTF-8"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PromoPageScreen(
    url: String,
    onCloseClick: () -> Unit,
    onLoadError: (reason: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }
    var isFailed by remember { mutableStateOf(false) }
    val failLoading: (reason: String) -> Unit = { reason ->
        isFailed = true
        onLoadError(reason)
    }

    LaunchedEffect(Unit) {
        delay(PAGE_LOAD_TIMEOUT)
        if (isLoading) failLoading("timeout")
    }

    TangemTopBarScaffold(
        modifier = modifier,
        topBar = {
            // A blur draws the WebView once more into its own layer, and a WebView drawn twice a frame flickers
            ProvideHaze {
                TangemTopNavigation(
                    blurBackground = false,
                    endButton = { TangemButton.Close(onClick = onCloseClick) },
                )
            }
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
                            mainFrameHost = url.toUri().host,
                            onPageVisible = { isLoading = false },
                            onLoadError = failLoading,
                        )
                        if (PromoPageUrlValidator.isAllowed(url.toUri())) loadUrl(url)
                    }
                },
                // The WebView doesn't fade with the leaving screen and would show its error page
                update = { webView -> webView.isInvisible = isFailed },
                onRelease = WebView::destroy,
                modifier = Modifier.fillMaxSize(),
            )

            if (isLoading || isFailed) {
                Box(modifier = Modifier.fillMaxSize().background(TangemTheme.colors3.bg.primary))
                TangemLoader(size = TangemLoaderSize.X32)
            }
        }
    }
}

private class PromoPageWebViewClient(
    private var mainFrameHost: String?,
    private val onPageVisible: () -> Unit,
    private val onLoadError: (reason: String) -> Unit,
) : WebViewClient() {

    private var isPageShown = false

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (!request.isForMainFrame) return false
        val isAllowed = PromoPageUrlValidator.isAllowed(request.url)
        if (isAllowed) mainFrameHost = request.url.host else failFirstLoad("page is not allowed")
        return !isAllowed
    }

    // POST navigations bypass shouldOverrideUrlLoading: a disallowed one gets an empty page instead of being sent,
    // and onPageCommitVisible closes the screen on it
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        if (request.isForMainFrame && !PromoPageUrlValidator.isAllowed(request.url)) {
            WebResourceResponse(MIME_TYPE_HTML, CHARSET_UTF8, "".byteInputStream())
        } else {
            null
        }

    override fun onPageCommitVisible(view: WebView, url: String) {
        if (!PromoPageUrlValidator.isAllowed(url.toUri())) {
            onLoadError("page is not allowed")
            return
        }
        isPageShown = true
        view.setBackgroundColor(Color.WHITE)
        onPageVisible()
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) failFirstLoad("error ${error.errorCode}")
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        if (request.isForMainFrame) failFirstLoad("HTTP ${errorResponse.statusCode}")
    }

    // SslError has no main-frame flag: an error on another host comes from a subresource and keeps the page
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        handler.cancel()
        if (error.url.toUri().host.equals(mainFrameHost, ignoreCase = true)) {
            failFirstLoad("SSL error ${error.primaryError}")
        }
    }

    // Unhandled, a crashed or killed renderer takes the app down with it
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        onLoadError("renderer gone")
        return true
    }

    private fun failFirstLoad(reason: String) {
        if (!isPageShown) onLoadError(reason)
    }
}