package com.tangem.tap.features.intentHandler

import android.content.Intent

/**
[REDACTED_AUTHOR]
 */
class MainIntentHandler {

    private val intentHandlers = mutableListOf<IntentHandler>()

    fun addHandler(handler: IntentHandler) {
        intentHandlers.add(handler)
    }

    fun removeIntentHandler(handler: IntentHandler) {
        intentHandlers.remove(handler)
    }

    fun removeAll() {
        intentHandlers.clear()
    }

    suspend fun handleIntent(intent: Intent?) {
        intentHandlers.toList().forEach {
            it.handleIntent(intent)
        }
    }
}