package com.tangem.tap.features.intentHandler

import android.content.Intent

/**
[REDACTED_AUTHOR]
 */
interface IntentHandler {

    fun handleIntent(intent: Intent?, isFromForeground: Boolean): Boolean
}