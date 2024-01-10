package com.tangem.tap.features.intentHandler

import android.content.Intent
import java.util.concurrent.CopyOnWriteArrayList

/**
* [REDACTED_AUTHOR]
 */
// [REDACTED_TODO_COMMENT]
class IntentProcessor {

    private val intentHandlers = CopyOnWriteArrayList<IntentHandler>()

    fun addHandler(handler: IntentHandler) {
        intentHandlers.add(handler)
    }

    fun removeAll() {
        intentHandlers.clear()
    }

    fun handleIntent(intent: Intent?) {
        intentHandlers.forEach {
            it.handleIntent(intent)
        }
    }
}
