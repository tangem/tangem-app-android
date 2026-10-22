package com.tangem.tap.common.analytics

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.hashedStringValue
import com.tangem.domain.wallets.builder.UserWalletIdBuilder

internal class ABTestsTrackingContextObserver(
    private val abTestsManager: ABTestsManager,
) : TrackingContextProxy.Observer {

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
        when (userWallet) {
            is UserWallet.Cold -> abTestsManager.setUserProperties(
                userId = userWallet.walletId.hashedStringValue,
                batch = userWallet.scanResponse.card.batchId,
                productType = userWallet.scanResponse.productType.name,
                firmware = userWallet.scanResponse.card.firmwareVersion.stringValue,
            )
            is UserWallet.Hot -> abTestsManager.setUserProperties(
                userId = userWallet.walletId.hashedStringValue,
                batch = null,
                productType = AnalyticsParam.ProductType.MobileWallet.value,
                firmware = null,
            )
        }
    }

    override fun eraseContext() {
        abTestsManager.removeUserProperties()
    }
}