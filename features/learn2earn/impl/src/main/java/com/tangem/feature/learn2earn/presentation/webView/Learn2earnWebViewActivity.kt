package com.tangem.feature.learn2earn.presentation.webView

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.tangem.feature.learn2earn.impl.R
import dagger.hilt.android.AndroidEntryPoint

/**
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
class Learn2earnWebViewActivity : AppCompatActivity() {

    private val viewModel by viewModels<WebViewViewModel>()

    private lateinit var actionBar: ActionBar
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webViewData = viewModel.extractData(intent)

        setSupportActionBar(findViewById(R.id.toolbar))
        actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.title = webViewData.uri.authority
        webView = findViewById(R.id.web_view)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true

        webView.webViewClient = Learn2earnWebViewClient(
            redirectHandler = viewModel,
            headers = webViewData.headers,
            finishSessionHandler = { finish() },
        )
        webView.loadUrl(
            webViewData.uri.toString(),
            webViewData.headers,
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        webView.clearHistory()
        webView.clearCache(true)
        webView.destroy()

        super.onDestroy()
    }

    companion object {
        const val EXTRA_WEB_URI = "webViewUri"
        const val EXTRA_WEB_HEADERS = "webViewHeaders"
    }
}