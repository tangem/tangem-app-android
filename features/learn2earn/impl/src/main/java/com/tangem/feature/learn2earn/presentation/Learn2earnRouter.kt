package com.tangem.feature.learn2earn.presentation

import android.content.Context
import android.content.Intent

/**
[REDACTED_AUTHOR]
 */
class Learn2earnRouter(
    private val context: Context,
) {

    fun openWebView() {
        context.startActivity(Intent(context, Learn2earnWebViewActivity::class.java))
    }
}