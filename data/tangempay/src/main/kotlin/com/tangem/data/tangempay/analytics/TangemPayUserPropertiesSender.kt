package com.tangem.data.tangempay.analytics

import com.tangem.core.analytics.api.AnalyticsUserPropertiesHandler
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.TangemPayUserProperties
import javax.inject.Inject

internal class TangemPayUserPropertiesSender @Inject constructor(
    private val userPropertiesHandler: AnalyticsUserPropertiesHandler,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    fun onSummaryFetched(userWalletId: UserWalletId, summary: CustomerSummary) {
        sendForSelectedWallet(userWalletId = userWalletId, properties = TangemPayUserProperties.of(summary))
    }

    fun onCustomerInfoFetched(userWalletId: UserWalletId, customerInfo: CustomerInfo) {
        sendForSelectedWallet(
            userWalletId = userWalletId,
            properties = TangemPayUserProperties.of(customerInfo.tariffPlan),
        )
    }

    private fun sendForSelectedWallet(userWalletId: UserWalletId, properties: Map<String, String>) {
        if (userWalletsListRepository.selectedUserWallet.value?.walletId != userWalletId) return
        userPropertiesHandler.setUserProperties(properties)
    }
}