package com.tangem.tap.features.sprinklr.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.tangem.tap.features.sprinklr.ui.webview.SprinklrWebViewClient

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SprinklrScreenContent(state: SprinklrScreenState, modifier: Modifier = Modifier) {
    WebView(
        modifier = modifier,
        state = rememberWebViewState(url = state.initialUrl),
        onCreated = { webView ->
            with(webView.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
        },
        client = remember { SprinklrWebViewClient(state.onNewUrl) },
    )
}
