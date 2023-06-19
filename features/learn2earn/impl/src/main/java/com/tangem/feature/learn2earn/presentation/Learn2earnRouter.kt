package com.tangem.feature.learn2earn.presentation

import android.content.Context
import android.content.Intent
import timber.log.Timber
import java.lang.ref.WeakReference

/**
[REDACTED_AUTHOR]
 */
class Learn2earnRouter(
    private val wContext: WeakReference<Context>,
) {

    fun openWebView() {
        val context = wContext.get()
        if (context == null) {
            Timber.e("Can't open the Learn2earnWebViewActivity")
        } else {
            context.startActivity(Intent(context, Learn2earnWebViewActivity::class.java))
        }
    }
}