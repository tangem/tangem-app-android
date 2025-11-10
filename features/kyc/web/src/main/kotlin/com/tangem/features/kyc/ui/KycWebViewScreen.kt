package com.tangem.features.kyc.ui

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.kyc.SumSubWebViewClient
import com.tangem.features.kyc.entity.WebSdkKycUM

@Composable
internal fun KycWebViewScreen(state: WebSdkKycUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        AppBarWithBackButton(
            modifier = Modifier.statusBarsPadding(),
            onBackClick = state.onBackClick,
            text = TextReference.EMPTY.resolveReference(),
            containerColor = TangemTheme.colors.background.primary,
        )
        WebViewContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = state,
        )
    }
}

@Composable
private fun WebViewContent(state: WebSdkKycUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isLoading || state.accessToken == null) {
            CircularProgressIndicator(color = TangemTheme.colors.icon.primary1)
        } else {
            val sumSubWebViewClient = remember { SumSubWebViewClient(accessToken = state.accessToken) }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        with(settings) { javaScriptEnabled = true }
                        webViewClient = sumSubWebViewClient
                        loadUrl(state.url)
                    }
                },
            )
        }
    }
}