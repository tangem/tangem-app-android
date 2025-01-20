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

    fun handleIntent(intent: Intent?, isFromForeground: Boolean, skipNavigationHandlers: Boolean = false) {
        intentHandlers
            .filterNot { handler -> skipNavigationHandlers && handler is AffectsNavigation }
            .forEach { handler -> handler.handleIntent(intent, isFromForeground) }
    }
}
