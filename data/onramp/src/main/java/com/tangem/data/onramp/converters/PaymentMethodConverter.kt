package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.model.PaymentMethodDTO
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.PaymentMethodType
import com.tangem.domain.onramp.repositories.OnrampFeatureToggles
import com.tangem.utils.converter.TwoWayConverter

internal class PaymentMethodConverter(
    private val onrampFeatureToggles: OnrampFeatureToggles,
) : TwoWayConverter<PaymentMethodDTO, OnrampPaymentMethod> {

    override fun convert(value: PaymentMethodDTO): OnrampPaymentMethod {
        val isThemed = onrampFeatureToggles.isThemedPaymentMethodImagesEnabled
        return OnrampPaymentMethod(
            id = value.id,
            name = value.name,
            imageUrl = value.image,
            type = PaymentMethodType.getType(value.id),
            imageUrlLight = if (isThemed) value.imageLight else null,
            imageUrlDark = if (isThemed) value.imageDark else null,
        )
    }

    override fun convertBack(value: OnrampPaymentMethod): PaymentMethodDTO = PaymentMethodDTO(
        id = value.id,
        name = value.name,
        image = value.imageUrl,
        imageLight = value.imageUrlLight,
        imageDark = value.imageUrlDark,
    )
}