package com.tangem.tap.common.analytics

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.tap.common.extensions.addContext
import com.tangem.tap.common.extensions.addHotWalletContext
import com.tangem.tap.common.extensions.eraseContext
import com.tangem.tap.common.extensions.removeContext
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.common.extensions.setHotWalletContext
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.wallet.UserWalletId

/**
[REDACTED_AUTHOR]
 */
internal class DefaultTrackingContextProxy(private val abTestsManager: ABTestsManager) : TrackingContextProxy {

    override fun setContext(scanResponse: ScanResponse) {
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build()

        abTestsManager.setUserProperties(
            userId = calculateUserIdHash(userWalletId),
            batch = scanResponse.card.batchId,
            productType = scanResponse.productType.name,
            firmware = scanResponse.card.firmwareVersion.stringValue,
        )
    }

    override fun setContext(userWallet: UserWallet) {
        Analytics.setContext(userWallet)

        when (userWallet) {
            is UserWallet.Cold -> {
                setColdWalletUserProperties(userWallet)
            }
            is UserWallet.Hot -> {
                setHotWalletUserProperties(userWallet)
            }
        }
    }

    override fun addContext(userWallet: UserWallet) {
        Analytics.addContext(userWallet)
    }

    override fun setHotWalletContext() {
        Analytics.setHotWalletContext()
    }

    override fun eraseContext() {
        Analytics.eraseContext()
        abTestsManager.removeUserProperties()
    }

    override fun addContext(scanResponse: ScanResponse) {
        Analytics.addContext(scanResponse)
    }

    override fun addHotWalletContext() {
        Analytics.addHotWalletContext()
    }

    override fun removeContext() {
        Analytics.removeContext()
    }

    override fun proceedWithContext(userWallet: UserWallet, action: () -> Unit) {
        setContext(userWallet)
        action()
        eraseContext()
    }

    private fun calculateUserIdHash(userWalletId: UserWalletId?): String? {
        return userWalletId?.value
            ?.calculateSha256()
            ?.toHexString()
    }

    private fun setColdWalletUserProperties(userWallet: UserWallet.Cold) {
        abTestsManager.setUserProperties(
            userId = calculateUserIdHash(userWallet.walletId),
            batch = userWallet.scanResponse.card.batchId,
            productType = userWallet.scanResponse.productType.name,
            firmware = userWallet.scanResponse.card.firmwareVersion.stringValue,
        )
    }

    private fun setHotWalletUserProperties(userWallet: UserWallet.Hot) {
        abTestsManager.setUserProperties(
            userId = calculateUserIdHash(userWallet.walletId),
            batch = null,
            productType = AnalyticsParam.ProductType.MobileWallet.value,
            firmware = null,
        )
    }
}