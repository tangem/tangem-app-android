package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CashbackPromotionsResponse
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

/** Maps [CashbackPromotionsResponse] (BFF) to the domain [CashbackPromotions]. */
internal object CashbackPromotionsConverter : Converter<CashbackPromotionsResponse, CashbackPromotions> {

    override fun convert(value: CashbackPromotionsResponse): CashbackPromotions {
        return CashbackPromotions(
            cardTiers = value.cashbackOnCards?.tiers.orEmpty().map(::convertTier),
            monthlyCap = value.cashbackOnCards?.toMonthlyCap(),
            additionalCashback = value.additionalCashback.orEmpty().map(::convertAdditional),
        )
    }

    private fun CashbackPromotionsResponse.CashbackOnCards.toMonthlyCap(): CashbackPromotions.MonthlyCap? {
        val amount = monthlyCapAmount ?: return null
        return CashbackPromotions.MonthlyCap(amount = amount, currency = monthlyCapCurrency)
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

    private fun convertAdditional(
        promo: CashbackPromotionsResponse.AdditionalCashback,
    ): CashbackPromotions.AdditionalCashback {
        val endDate = promo.endDate?.let { runCatching { DateTime.parse(it) }.getOrNull() }
        return CashbackPromotions.AdditionalCashback(
            id = promo.id.orEmpty(),
            name = promo.name.orEmpty(),
            description = promo.description.orEmpty(),
            isPermanent = promo.isPermanent ?: (endDate == null),
            endDate = endDate,
        )
    }
}