package com.tangem.tap.common.analytics

import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.analytics.paramsInterceptor.HotWalletContextInterceptor
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor

/**
[REDACTED_AUTHOR]
 */
internal class DefaultTrackingContextProxy(
    private val paramsInterceptorHolder: ParamsInterceptorHolder,
    private val userIdHandler: AnalyticsUserIdHandler,
    private val observers: Set<TrackingContextProxy.Observer>,
) : TrackingContextProxy {

    override fun setContext(scanResponse: ScanResponse) {
        observers.forEach { it.setContext(scanResponse) }
    }

    override fun setContext(userWallet: UserWallet) {
        userIdHandler.setUserId(userWallet.walletId.stringValue)

        when (userWallet) {
            is UserWallet.Cold -> {
                paramsInterceptorHolder.removeParamsInterceptor(HotWalletContextInterceptor.id())
                paramsInterceptorHolder.addParamsInterceptor(LinkedCardContextInterceptor(userWallet.scanResponse))
            }
            is UserWallet.Hot -> {
                paramsInterceptorHolder.removeParamsInterceptor(LinkedCardContextInterceptor.id())
                paramsInterceptorHolder.addParamsInterceptor(HotWalletContextInterceptor())
            }
        }

        observers.forEach { it.setContext(userWallet) }
    }

    /** Adds a new context and keeps the previous one as its parent, see [removeContext]. */
    override fun addContext(userWallet: UserWallet) {
        val currentContext = removeCurrentContext()

        val newContext = when (userWallet) {
            is UserWallet.Cold -> LinkedCardContextInterceptor(userWallet.scanResponse, parent = currentContext)
            is UserWallet.Hot -> HotWalletContextInterceptor(parent = currentContext)
        }

        userIdHandler.setUserId(userWallet.walletId.stringValue)
        paramsInterceptorHolder.addParamsInterceptor(newContext)

        observers.forEach { it.addContext(userWallet) }
    }

    override fun setHotWalletContext() {
        paramsInterceptorHolder.addParamsInterceptor(HotWalletContextInterceptor())

        observers.forEach { it.setHotWalletContext() }
    }

    override fun eraseContext() {
        userIdHandler.clearUserId()
        paramsInterceptorHolder.removeParamsInterceptor(LinkedCardContextInterceptor.id())
        paramsInterceptorHolder.removeParamsInterceptor(HotWalletContextInterceptor.id())

        observers.forEach { it.eraseContext() }
    }

    /** Adds a new context and keeps the previous one as its parent, see [removeContext]. */
    override fun addContext(scanResponse: ScanResponse) {
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
        if (userWalletId != null) {
            userIdHandler.setUserId(userWalletId.stringValue)
        }

        val newContext = LinkedCardContextInterceptor(scanResponse, parent = removeCurrentContext())
        paramsInterceptorHolder.addParamsInterceptor(newContext)

        observers.forEach { it.addContext(scanResponse) }
    }

    override fun addHotWalletContext() {
        paramsInterceptorHolder.addParamsInterceptor(HotWalletContextInterceptor(parent = removeCurrentContext()))

        observers.forEach { it.addHotWalletContext() }
    }

    /** Removes the current context and restores its parent, if any. */
    override fun removeContext() {
        val previousContext = when (val currentContext = removeCurrentContext()) {
            is LinkedCardContextInterceptor -> currentContext.parent
            is HotWalletContextInterceptor -> currentContext.parent
            else -> null
        }
        previousContext?.let(paramsInterceptorHolder::addParamsInterceptor)

        observers.forEach { it.removeContext() }
    }

    private fun removeCurrentContext(): ParamsInterceptor? {
        return paramsInterceptorHolder.removeParamsInterceptor(LinkedCardContextInterceptor.id())
            ?: paramsInterceptorHolder.removeParamsInterceptor(HotWalletContextInterceptor.id())
    }
}