package com.tangem.tap.common.extensions

import com.tangem.core.analytics.Analytics
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor

/**
[REDACTED_AUTHOR]
 */

/**
 * Sets the new context
 */
fun Analytics.setContext(scanResponse: ScanResponse) {
    val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
    if (userWalletId != null) {
        setUserId(userWalletId.stringValue)
    }

    addParamsInterceptor(LinkedCardContextInterceptor(scanResponse))
}

fun Analytics.setContext(userWallet: UserWallet) {
    setUserId(userWallet.walletId.stringValue)
    // TODO add product type for hot ([REDACTED_TASK_KEY] [Hot Wallet] Analytics)

    if (userWallet is UserWallet.Cold) {
        addParamsInterceptor(LinkedCardContextInterceptor(userWallet.scanResponse))
    }
}

/**
 * Erases the context
 */
fun Analytics.eraseContext() {
    clearUserId()
    removeParamsInterceptor(LinkedCardContextInterceptor.id())
}

/**
 * Adds a new context and keeps a previous context as the parent of the new one
 */
fun Analytics.addContext(scanResponse: ScanResponse) {
    val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
    if (userWalletId != null) {
        setUserId(userWalletId.stringValue)
    }

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