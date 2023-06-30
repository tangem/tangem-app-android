package com.tangem.tap.common.extensions

import com.tangem.core.analytics.Analytics
import com.tangem.domain.card.CardTypeResolver
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor

/**
[REDACTED_AUTHOR]
 */

/**
 * Sets the new context
 */
fun Analytics.setContext(scanResponse: ScanResponse, cardTypeResolver: CardTypeResolver) {
    addParamsInterceptor(LinkedCardContextInterceptor(scanResponse, cardTypeResolver))
}

/**
 * Erases the context
 */
fun Analytics.eraseContext() {
    removeParamsInterceptor(LinkedCardContextInterceptor.id())
}

/**
 * Adds a new context and keeps a previous context as the parent of the new one
 */
fun Analytics.addContext(scanResponse: ScanResponse, cardTypeResolver: CardTypeResolver) {
    val currentContext = removeParamsInterceptor(LinkedCardContextInterceptor.id()) as? LinkedCardContextInterceptor
    val newContext = LinkedCardContextInterceptor(
        scanResponse = scanResponse,
        cardTypeResolver = cardTypeResolver,
        parent = currentContext,
    )

    addParamsInterceptor(newContext)
}

/**
 * Removes the current context and restores the previous one if it was present.
 */
fun Analytics.removeContext() {
    val currentContext = removeParamsInterceptor(LinkedCardContextInterceptor.id()) as? LinkedCardContextInterceptor
    val previousContext = currentContext?.parent ?: return

    addParamsInterceptor(previousContext)
}