package com.tangem.tap.common.analytics

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.hashedStringValue
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.analytics.paramsInterceptor.HotWalletContextInterceptor
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor

/**
[REDACTED_AUTHOR]
 */
internal class DefaultTrackingContextProxy(
    private val abTestsManager: ABTestsManager,
    private val paramsInterceptorHolder: ParamsInterceptorHolder,
    private val userIdHandler: AnalyticsUserIdHandler,
) : TrackingContextProxy {

    override fun setContext(scanResponse: ScanResponse) {
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()

        abTestsManager.setUserProperties(
            userId = userWalletId?.hashedStringValue,
            batch = scanResponse.card.batchId,
            productType = scanResponse.productType.name,
            firmware = scanResponse.card.firmwareVersion.stringValue,
        )
    }

    override fun setContext(userWallet: UserWallet) {
        userIdHandler.setUserId(userWallet.walletId.stringValue)

        when (userWallet) {
            is UserWallet.Cold -> {
                paramsInterceptorHolder.removeParamsInterceptor(HotWalletContextInterceptor.id())
                paramsInterceptorHolder.addParamsInterceptor(LinkedCardContextInterceptor(userWallet.scanResponse))
                setColdWalletUserProperties(userWallet)
            }
            is UserWallet.Hot -> {
                paramsInterceptorHolder.removeParamsInterceptor(LinkedCardContextInterceptor.id())
                paramsInterceptorHolder.addParamsInterceptor(HotWalletContextInterceptor())
                setHotWalletUserProperties(userWallet)
            }
        }
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
    }

    override fun setHotWalletContext() {
        paramsInterceptorHolder.addParamsInterceptor(HotWalletContextInterceptor())
    }

    override fun eraseContext() {
        userIdHandler.clearUserId()
        paramsInterceptorHolder.removeParamsInterceptor(LinkedCardContextInterceptor.id())
        paramsInterceptorHolder.removeParamsInterceptor(HotWalletContextInterceptor.id())
        abTestsManager.removeUserProperties()
    }

    /** Adds a new context and keeps the previous one as its parent, see [removeContext]. */
    override fun addContext(scanResponse: ScanResponse) {
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()
        if (userWalletId != null) {
            userIdHandler.setUserId(userWalletId.stringValue)
        }

        val newContext = LinkedCardContextInterceptor(scanResponse, parent = removeCurrentContext())
        paramsInterceptorHolder.addParamsInterceptor(newContext)
    }

    override fun addHotWalletContext() {
        paramsInterceptorHolder.addParamsInterceptor(HotWalletContextInterceptor(parent = removeCurrentContext()))
    }

    /** Removes the current context and restores its parent, if any. */
    override fun removeContext() {
        val previousContext = when (val currentContext = removeCurrentContext()) {
            is LinkedCardContextInterceptor -> currentContext.parent
            is HotWalletContextInterceptor -> currentContext.parent
            else -> null
        } ?: return

        paramsInterceptorHolder.addParamsInterceptor(previousContext)
    }

    private fun removeCurrentContext(): ParamsInterceptor? {
        return paramsInterceptorHolder.removeParamsInterceptor(LinkedCardContextInterceptor.id())
            ?: paramsInterceptorHolder.removeParamsInterceptor(HotWalletContextInterceptor.id())
    }

    private fun setColdWalletUserProperties(userWallet: UserWallet.Cold) {
        abTestsManager.setUserProperties(
            userId = userWallet.walletId.hashedStringValue,
            batch = userWallet.scanResponse.card.batchId,
            productType = userWallet.scanResponse.productType.name,
            firmware = userWallet.scanResponse.card.firmwareVersion.stringValue,
        )
    }

    private fun setHotWalletUserProperties(userWallet: UserWallet.Hot) {
        abTestsManager.setUserProperties(
            userId = userWallet.walletId.hashedStringValue,
            batch = null,
            productType = AnalyticsParam.ProductType.MobileWallet.value,
            firmware = null,
        )
    }
}