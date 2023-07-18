package com.tangem.feature.learn2earn.presentation.webView

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.res.TangemTheme
import dagger.hilt.android.AndroidEntryPoint

/**
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
class Learn2earnWebViewActivity : AppCompatActivity() {

    private val viewModel by viewModels<WebViewViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webViewData = viewModel.extractData(intent)

        setContent {
            TangemTheme {
                BackHandler(onBack = { finish() })
                ScreenContent(webViewData)
            }
        }
    }

    @Composable
    private fun ScreenContent(webViewData: WebViewData) {
        Column {
            AppBarWithBackButton(
                modifier = Modifier.background(TangemTheme.colors.background.secondary),
                text = webViewData.uri.authority,
                onBackClick = { finish() },
            )
            WebView(
                state = rememberWebViewState(
                    url = webViewData.uri.toString(),
                    additionalHttpHeaders = webViewData.headers
                ),
                onCreated = {
                    it.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = true
                    }
                },
                client = remember {
                    Learn2earnWebViewClient(
                        redirectHandler = viewModel,
                        headers = webViewData.headers,
                        finishSessionHandler = { finish() },
                    )
                },
            )
        }
    }

    companion object {
        const val EXTRA_WEB_URI = "webViewUri"
        const val EXTRA_WEB_HEADERS = "webViewHeaders"
    }
}