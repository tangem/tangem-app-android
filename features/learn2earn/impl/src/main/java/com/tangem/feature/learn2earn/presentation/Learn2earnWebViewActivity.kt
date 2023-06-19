package com.tangem.feature.learn2earn.presentation

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.tangem.feature.learn2earn.domain.models.RedirectConsequences
import com.tangem.feature.learn2earn.domain.models.WebViewHelper
import com.tangem.feature.learn2earn.impl.R
import dagger.hilt.android.AndroidEntryPoint

/**
[REDACTED_AUTHOR]
 */
@AndroidEntryPoint
class Learn2earnWebViewActivity : AppCompatActivity() {

    private val learn2earnViewModel by viewModels<Learn2earnViewModel>()

    private lateinit var actionBar: ActionBar
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        setSupportActionBar(findViewById(R.id.toolbar))
        actionBar = supportActionBar!!
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.title = learn2earnViewModel.webViewUri.authority

        webView = findViewById(R.id.web_view)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true

        webView.webViewClient = Learn2earnWebViewClient(
            helper = learn2earnViewModel,
            finishSessionHandler = { finish() },
        )
        webView.loadUrl(
            learn2earnViewModel.webViewUri.toString(),
            learn2earnViewModel.getWebViewHeaders(),
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
}

private class Learn2earnWebViewClient(
    private val helper: WebViewHelper,
    private val finishSessionHandler: () -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        when (helper.handleWebViewRedirect(request.url)) {
            RedirectConsequences.NOTHING -> Unit
            RedirectConsequences.PROCEED -> {
                val headers = helper.getWebViewHeaders().toMutableMap()
                request.requestHeaders?.let { headers.putAll(it) }
                view.loadUrl(request.url.toString(), headers)
            }
            RedirectConsequences.FINISH_SESSION -> {
                finishSessionHandler.invoke()
            }
        }
        return true
    }
}