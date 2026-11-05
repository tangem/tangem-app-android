package com.tangem.features.polymarket.impl.main.addfunds

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCollateralCurrencyFactory
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.common.funding.RequisitesState
import javax.inject.Inject

internal class PolymarketReceiveSheetFactory @Inject constructor(
    private val featureToggles: PolymarketFeatureToggles,
    private val collateralCurrencyFactory: PolymarketCollateralCurrencyFactory,
) {

    fun create(
        collateral: CryptoCurrency,
        userWalletId: UserWalletId,
        requisites: RequisitesState.Ready,
    ): PolymarketFundingSheetConfig {
        if (!featureToggles.isPolymarketPhaseTwoEnabled) {
            return PolymarketFundingSheetConfig.Receive(
                config = PolymarketReceiveConfigFactory.create(
                    cryptoCurrency = collateral,
                    userWalletId = userWalletId,
                    target = requisites.target,
                ),
            )
        }

        return PolymarketFundingSheetConfig.ChooseNetwork(
            receiveConfigs = requisites.options.map { option ->
                PolymarketReceiveConfigFactory.create(
                    cryptoCurrency = collateralCurrencyFactory.createDepositTarget(option.asset),
                    userWalletId = userWalletId,
                    target = option.target,
                )
            },
        )
    }
}