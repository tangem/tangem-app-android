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
        )
    }
}