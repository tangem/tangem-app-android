package com.tangem.tap.features.intentHandler

import android.content.Intent
import java.util.concurrent.CopyOnWriteArrayList

/**
[REDACTED_AUTHOR]
 */
// TODO: fixme: close it with the combined interfaces IntentHandler and IntentHandlerHolder
class IntentProcessor {

    private val intentHandlers = CopyOnWriteArrayList<IntentHandler>()

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
        intentHandlers.forEach {
            it.handleIntent(intent)
        }
    }
}