package com.tangem.domain.markets

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.promo.PromoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShouldShowYieldModeMarketPromoUseCase(
    private val promoRepository: PromoRepository,
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    operator fun invoke(appCurrency: AppCurrency, interval: TokenMarketListConfig.Interval): Flow<Boolean> {
        val hideClickedFlow = promoRepository.isMarketsYieldSupplyNotificationHideClicked()

        return hideClickedFlow.map { hideClicked ->
            !hideClicked && marketsTokenRepository.showYieldModePromo(
                appCurrency = appCurrency,
                interval = interval,
            )
        }
    }
}