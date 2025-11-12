package com.tangem.tap.common.extensions

import com.tangem.core.analytics.Analytics
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.analytics.paramsInterceptor.HotWalletContextInterceptor
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor

/**
[REDACTED_AUTHOR]
 */

/**
 * Sets the new context
 */
fun Analytics.setContext(userWalletId: UserWalletId?, scanResponse: ScanResponse) {
    if (userWalletId != null) {
        setUserId(userWalletId.stringValue)
    }

    addParamsInterceptor(LinkedCardContextInterceptor(scanResponse))
}

fun Analytics.setContext(userWallet: UserWallet) {
    setUserId(userWallet.walletId.stringValue)

    when (userWallet) {
        is UserWallet.Cold -> {
            removeParamsInterceptor(HotWalletContextInterceptor.id())
            addParamsInterceptor(LinkedCardContextInterceptor(userWallet.scanResponse))
        }
        is UserWallet.Hot -> {
            removeParamsInterceptor(LinkedCardContextInterceptor.id())
            addParamsInterceptor(HotWalletContextInterceptor())
        }
    }
}

fun Analytics.setHotWalletContext() {
    addParamsInterceptor(HotWalletContextInterceptor())
}

/**
 * Erases the context
 */
fun Analytics.eraseContext() {
    clearUserId()
    removeParamsInterceptor(LinkedCardContextInterceptor.id())
    removeParamsInterceptor(HotWalletContextInterceptor.id())
}

/**
 * Adds a new context and keeps a previous context as the parent of the new one
 */
fun Analytics.addContext(userWallet: UserWallet) {
    val currentContext = removeParamsInterceptor(LinkedCardContextInterceptor.id())
        ?: removeParamsInterceptor(HotWalletContextInterceptor.id())

    val newContext = when (userWallet) {
        is UserWallet.Cold -> LinkedCardContextInterceptor(userWallet.scanResponse, parent = currentContext)
        is UserWallet.Hot -> HotWalletContextInterceptor(parent = currentContext)
    }

    setUserId(userId = userWallet.walletId.stringValue)
    addParamsInterceptor(newContext)
}

/**
 * Adds a new context and keeps a previous context as the parent of the new one
 */
fun Analytics.addContext(scanResponse: ScanResponse) {
    val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
    if (userWalletId != null) {
        setUserId(userWalletId.stringValue)
    }

    val currentContext = removeParamsInterceptor(LinkedCardContextInterceptor.id())
        ?: removeParamsInterceptor(HotWalletContextInterceptor.id())
    val newContext = LinkedCardContextInterceptor(scanResponse, parent = currentContext)

    addParamsInterceptor(newContext)
}

fun Analytics.addHotWalletContext() {
    val currentContext = removeParamsInterceptor(LinkedCardContextInterceptor.id()) as? LinkedCardContextInterceptor
    val newContext = HotWalletContextInterceptor(currentContext)

    addParamsInterceptor(newContext)
}

/**
 * Removes the current context and restores the previous one if it was present.
 */
fun Analytics.removeContext() {
    val currentContext = removeParamsInterceptor(LinkedCardContextInterceptor.id())
        ?: removeParamsInterceptor(HotWalletContextInterceptor.id())

    val previousContext = when (currentContext) {
        is LinkedCardContextInterceptor -> currentContext.parent
        is HotWalletContextInterceptor -> currentContext.parent
        else -> null
    } ?: return

    addParamsInterceptor(previousContext)
}