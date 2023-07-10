package com.tangem.tap.features.intentHandler

import android.content.Intent

/**
[REDACTED_AUTHOR]
 */
interface IntentHandler {
    suspend fun handleIntent(intent: Intent?): Boolean
}