package com.tangem.data.pay.util

import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.spend.datasource.pay.models.response.CashbackPromotionsResponse
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

internal object CashbackPromotionsConverter : Converter<CashbackPromotionsResponse, CashbackPromotions> {

    override fun convert(value: CashbackPromotionsResponse): CashbackPromotions {
        val result = value.result
        return CashbackPromotions(
            cards = result?.cashbackOnCards?.cards.orEmpty().map(::convertCard),
            accountMonthlyCap = result?.cashbackOnCards?.toAccountMonthlyCap(),
            additionalCashback = result?.additionalCashback.orEmpty().mapNotNull(::convertAdditional),
        )
    }

    private fun CashbackPromotionsResponse.CashbackOnCards.toAccountMonthlyCap(): CashbackPromotions.MonthlyCap? {
        val amount = accountMonthlyCapAmount ?: return null
        return CashbackPromotions.MonthlyCap(amount = amount, currency = accountMonthlyCapCurrency)
    }

    private fun convertCard(card: CashbackPromotionsResponse.Card): CashbackPromotions.CardPromotion {
        val rate = card.cardCashbackRate
        return CashbackPromotions.CardPromotion(
            cardType = card.cardType,
            title = card.title?.takeIf(String::isNotBlank),
            cashbackRate = rate,
            minTransactionAmount = card.minTransactionAmount,
            promotionId = card.promotionId,
        )
    }

    private fun convertAdditional(
        promo: CashbackPromotionsResponse.AdditionalCashback,
    ): CashbackPromotions.AdditionalCashback? {
        val name = promo.name.takeIf(String::isNotBlank) ?: return null
        return CashbackPromotions.AdditionalCashback(
            id = promo.id,
            cardType = promo.cardType,
            name = name,
            description = promo.description?.takeIf(String::isNotBlank),
            endDate = promo.endDate?.let { runCatching { DateTime.parse(it) }.getOrNull() },
            promoCap = promo.toPromoCap(),
            minTransactionAmount = promo.minTransactionAmount,
            priority = promo.priority,
        )
    }

    private fun CashbackPromotionsResponse.AdditionalCashback.toPromoCap(): CashbackPromotions.PromoCap? {
        val amount = promoCapAmount ?: return null
        return CashbackPromotions.PromoCap(
            amount = amount,
            period = CashbackPromotions.PromoCap.Period.fromString(promoCapPeriod),
            currency = capCurrency,
        )
    }
}