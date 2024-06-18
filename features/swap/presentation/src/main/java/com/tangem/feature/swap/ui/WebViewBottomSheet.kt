package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.models.states.WebViewBottomSheetConfig

@Composable
fun WebViewBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content: WebViewBottomSheetConfig ->
        WebViewBottomSheetContent(
            WebViewBottomSheetConfig(url = content.url),
        )
    }
}

@Composable
private fun WebViewBottomSheetContent(content: WebViewBottomSheetConfig) {
    val state = rememberWebViewState(content.url)
    val isInPreviewMode = LocalInspectionMode.current
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
    ) {
        WebView(
            state = state,
            modifier = Modifier
                .background(TangemTheme.colors.background.secondary),
            captureBackPresses = false,
            onCreated = {
                if (!isInPreviewMode) {
                    it.settings.apply {
                        javaScriptEnabled = false
                        allowFileAccess = false
                    }
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_WebViewBottomSheetContent() {
    TangemThemePreview {
        WebViewBottomSheetContent(
            content = WebViewBottomSheetConfig(
                url = "https://tangem.com/en/",
            ),
        )
    }
}