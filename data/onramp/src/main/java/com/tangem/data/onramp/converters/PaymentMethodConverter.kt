package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.utils.converter.Converter

internal class PaymentMethodConverter : Converter<PaymentMethodDTO, OnrampPaymentMethod> {
    override fun convert(value: PaymentMethodDTO): OnrampPaymentMethod = OnrampPaymentMethod(
        id = value.id,
        name = value.name,
        imageUrl = value.image,
    )
}
