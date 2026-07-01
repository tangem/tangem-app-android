package com.tangem.data.pay.converter

import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.domain.models.account.TangemPayTariffPlan

internal object TangemPayTariffPlanConverter {

    private const val MAIN_IMAGE_TYPE = "MAIN"

    fun convert(value: CustomerMeResponse.TariffPlan?): TangemPayTariffPlan? {
        val name = value?.name ?: return null
        return TangemPayTariffPlan(
            type = TangemPayTariffPlan.Type.fromString(value.type),
            name = name,
            descriptionItems = value.descriptionItems.orEmpty().map(::convertDescriptionItem),
            imageUrl = value.images?.firstOrNull { it.type == MAIN_IMAGE_TYPE }?.url,
        )
    }

    private fun convertDescriptionItem(item: CustomerMeResponse.DescriptionItem): TangemPayTariffPlan.DescriptionItem {
        return TangemPayTariffPlan.DescriptionItem(
            section = TangemPayTariffPlan.Section.fromString(item.type),
            order = item.order ?: 0,
            title = item.title.orEmpty(),
            body = item.body.orEmpty(),
        )
    }
}