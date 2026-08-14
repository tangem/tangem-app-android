package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CardDeliveryQuoteResponse
import com.tangem.domain.pay.model.CardDeliveryQuote
import com.tangem.utils.converter.Converter
import java.util.Currency

internal object CardDeliveryQuoteConverter : Converter<CardDeliveryQuoteResponse, CardDeliveryQuote> {

    override fun convert(value: CardDeliveryQuoteResponse): CardDeliveryQuote {
        return CardDeliveryQuote(
            country = value.country,
            isPlasticAvailable = value.isPlasticAvailable,
            isDeliveryFeeWaived = value.isDeliveryFeeWaived,
            deliveryFee = CardDeliveryQuote.DeliveryFee(
                amount = value.deliveryFee.amount,
                currency = Currency.getInstance(value.deliveryFee.currency),
            ),
            deliveryEta = CardDeliveryQuote.DeliveryEta(
                minBusinessDays = value.deliveryEta.minBusinessDays,
                maxBusinessDays = value.deliveryEta.maxBusinessDays,
            ),
            hasSufficientBalance = value.hasSufficientBalance,
        )
    }
}