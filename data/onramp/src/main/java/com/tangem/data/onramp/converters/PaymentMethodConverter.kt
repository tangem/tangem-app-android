package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.utils.converter.TwoWayConverter

internal class PaymentMethodConverter : TwoWayConverter<PaymentMethodDTO, OnrampPaymentMethod> {
    override fun convert(value: PaymentMethodDTO): OnrampPaymentMethod = OnrampPaymentMethod(
        id = value.id,
        name = value.name,
        imageUrl = value.image,
        type = PaymentMethodType.getType(value.id),
    )

    override fun convertBack(value: OnrampPaymentMethod): PaymentMethodDTO = PaymentMethodDTO(
        id = value.id,
        name = value.name,
        image = value.imageUrl,
    )
}