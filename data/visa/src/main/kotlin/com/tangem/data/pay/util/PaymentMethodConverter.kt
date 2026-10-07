package com.tangem.data.pay.util

import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.spend.datasource.pay.models.response.PaymentMethodsResponse
import com.tangem.utils.converter.Converter

internal object PaymentMethodConverter :
    Converter<PaymentMethodsResponse.PaymentMethod, MoneyAccountPaymentMethod?> {

    override fun convert(value: PaymentMethodsResponse.PaymentMethod): MoneyAccountPaymentMethod? {
        return MoneyAccountPaymentMethod(
            id = value.id?.takeIf(String::isNotBlank) ?: return null,
            name = value.name?.takeIf(String::isNotBlank) ?: return null,
            countryCode = value.countryCode ?: return null,
            iconUrl = value.iconUrl,
            receiveFee = value.fees?.receive?.let(::convertFee),
            sendFee = value.fees?.send?.let(::convertFee),
        )
    }

    private fun convertFee(value: PaymentMethodsResponse.Fee): MoneyAccountPaymentMethod.Fee? {
        return MoneyAccountPaymentMethod.Fee(
            type = value.type ?: return null,
            amount = value.amount ?: return null,
            currency = value.currency?.takeIf(String::isNotBlank) ?: return null,
        )
    }
}