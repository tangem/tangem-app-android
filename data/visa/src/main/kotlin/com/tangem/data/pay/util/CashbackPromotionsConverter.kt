package com.tangem.data.pay.util

import com.tangem.datasource.api.pay.models.response.CashbackPromotionsResponse
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.utils.converter.Converter

/** Maps [CashbackPromotionsResponse] (BFF) to the domain [CashbackPromotions]. */
internal object CashbackPromotionsConverter : Converter<CashbackPromotionsResponse, CashbackPromotions> {

    override fun convert(value: CashbackPromotionsResponse): CashbackPromotions {
        return CashbackPromotions(
            cardTiers = value.cashbackOnCards?.tiers.orEmpty().map(::convertTier),
        )
    }

    private fun convertTier(tier: CashbackPromotionsResponse.CardTier): CashbackPromotions.CardTier {
        return CashbackPromotions.CardTier(
            tier = tier.tier.orEmpty(),
            label = tier.label.orEmpty(),
            scope = tier.scope.orEmpty(),
            minTransactionAmount = tier.minTransactionAmount,
            monthlyCapAmount = tier.tierMonthlyCapAmount,
        )
    }
}