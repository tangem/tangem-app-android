package com.tangem.features.promobanners.impl.converters

import com.tangem.datasource.api.tangemTech.models.promobanners.PromoBannerDisplayDTO
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import com.tangem.features.promobanners.impl.model.PromoBannerPriority
import com.tangem.utils.converter.Converter
import java.util.Locale

internal class PromoBannerDisplayDTOConverter : Converter<PromoBannerDisplayDTO, PromoBannerDisplay> {

    override fun convert(value: PromoBannerDisplayDTO): PromoBannerDisplay {
        return PromoBannerDisplay(
            id = value.id,
            placeholder = value.placeholder,
            priority = convertPriority(value.priority),
            title = value.title,
            subtitle = value.subtitle,
            iconUrl = value.iconUrl,
            deeplink = value.deeplink,
            isButtonEnabled = value.buttonEnabled,
            buttonText = value.buttonText,
            isDismissable = value.dismissable,
        )
    }

    private fun convertPriority(value: String): PromoBannerPriority {
        return when (value.uppercase(Locale.ROOT)) {
            "IMPORTANT" -> PromoBannerPriority.IMPORTANT
            "HIGH" -> PromoBannerPriority.HIGH
            "MEDIUM" -> PromoBannerPriority.MEDIUM
            "LOW" -> PromoBannerPriority.LOW
            else -> PromoBannerPriority.LOW
        }
    }
}