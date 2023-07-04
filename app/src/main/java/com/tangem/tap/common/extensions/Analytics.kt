package com.tangem.tap.common.extensions

import com.tangem.core.analytics.Analytics
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor

/**
[REDACTED_AUTHOR]
 */

/**
 * Sets the new context
 */
fun Analytics.setContext(scanResponse: ScanResponse) {
    addParamsInterceptor(LinkedCardContextInterceptor(scanResponse))
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
fun Analytics.addContext(scanResponse: ScanResponse) {
    val currentContext = removeParamsInterceptor(LinkedCardContextInterceptor.id()) as? LinkedCardContextInterceptor
    val newContext = LinkedCardContextInterceptor(scanResponse, parent = currentContext)

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