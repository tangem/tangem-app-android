package com.tangem.data.pay.converter

import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.domain.models.account.TangemPayTariffPlan

internal object TangemPayTariffPlanConverter {

    fun convert(value: CustomerMeResponse.TariffPlan?): TangemPayTariffPlan? {
        val id = value?.id ?: return null
        val name = value.name ?: return null
        return TangemPayTariffPlan(
            id = id,
            type = TangemPayTariffPlan.Type.fromString(value.type),
            name = name,
            descriptionItems = value.descriptionItems.orEmpty().mapNotNull(::convertDescriptionItem),
            images = value.images.orEmpty().mapNotNull(::convertImage),
        )
    }

    private fun convertDescriptionItem(item: CustomerMeResponse.DescriptionItem): TangemPayTariffPlan.DescriptionItem? {
        val title = item.title ?: return null
        return TangemPayTariffPlan.DescriptionItem(
            section = TangemPayTariffPlan.Section.fromString(item.type),
            order = item.order ?: 0,
            title = title,
            body = item.body.orEmpty(),
        )
    }

    private fun convertImage(image: CustomerMeResponse.Image): TangemPayTariffPlan.Image? {
        val url = image.url ?: return null
        return TangemPayTariffPlan.Image(
            type = TangemPayTariffPlan.Image.Type.fromString(image.type),
            url = url,
        )
    }
}