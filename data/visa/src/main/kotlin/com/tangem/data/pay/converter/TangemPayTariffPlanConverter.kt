package com.tangem.data.pay.converter

import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.domain.models.account.TangemPayTariffPlan
import java.util.Locale

internal object TangemPayTariffPlanConverter {

    fun convert(value: CustomerMeResponse.TariffPlan?): TangemPayTariffPlan? {
        val id = value?.id ?: return null
        val name = value.name ?: return null
        val programName = value.programName ?: return null

        // We cannot base our logic on knowledge what exact tier type is it.
        // Use it only as identifier to get data from other responses
        val tierId = value.type ?: return null

        // Basic tier is a default tier. We can base some features on it.
        // Other tiers are adjusted from admin panel. It is not guaranteed to have it in future
        val isBasicTier = tierId.uppercase(Locale.US) == "BASIC"

        return TangemPayTariffPlan(
            id = id,
            tierId = tierId,
            isBasicTier = isBasicTier,
            name = name,
            programName = programName,
            descriptionItems = value.descriptionItems.orEmpty().mapNotNull(::convertDescriptionItem),
            images = value.images.orEmpty().mapNotNull(::convertImage),
            fees = value.fees.orEmpty().mapNotNull(::convertFee),
        )
    }

    private fun convertFee(fee: CustomerMeResponse.Fee): TangemPayTariffPlan.Fee? {
        val amount = fee.amount ?: return null
        return TangemPayTariffPlan.Fee(
            type = TangemPayTariffPlan.Fee.Type.fromString(fee.type),
            amount = amount,
            currency = fee.currency.orEmpty(),
            description = fee.description.orEmpty(),
            period = fee.period?.let(TangemPayTariffPlan.Fee.Period::fromString),
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