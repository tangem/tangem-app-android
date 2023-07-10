package com.tangem.feature.learn2earn.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tangem.feature.learn2earn.presentation.webView.Learn2earnWebViewActivity

/**
[REDACTED_AUTHOR]
 */
class Learn2earnRouter(
    private val context: Context,
) {

    fun openWebView(uri: Uri, rawHeaders: ArrayList<String>) {
        val intent = Intent(context, Learn2earnWebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Learn2earnWebViewActivity.EXTRA_WEB_URI, uri.toString())
            .putStringArrayListExtra(Learn2earnWebViewActivity.EXTRA_WEB_HEADERS, rawHeaders)

        context.startActivity(intent)
    }
}