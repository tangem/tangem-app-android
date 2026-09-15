package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CustomerOffersResponse
import com.tangem.data.visa.utils.getJavaCurrencyByCode
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.utils.converter.Converter

internal object OfferConverter : Converter<CustomerOffersResponse.Offer, Offer?> {

    private const val MAIN_IMAGE_TYPE = "MAIN"

    override fun convert(value: CustomerOffersResponse.Offer): Offer? {
        val fee = value.fee ?: return null
        val data = value.data?.toData() ?: return null
        return Offer(
            type = Offer.Type.fromString(value.type),
            fee = Offer.Fee(amount = fee.amount, currency = getJavaCurrencyByCode(fee.currency)),
            data = data,
            mainImageUrl = value.images
                .orEmpty()
                .firstOrNull { it.type.equals(MAIN_IMAGE_TYPE, ignoreCase = true) }
                ?.url,
        )
    }

    private fun CustomerOffersResponse.Data.toData(): Offer.Data? {
        val orderType = orderType ?: return null
        return Offer.Data(
            specificationName = specificationName,
            orderType = OrderType.fromString(orderType),
            deliveryEta = toDeliveryEta(),
        )
    }

    private fun CustomerOffersResponse.Data.toDeliveryEta(): Offer.DeliveryEta? {
        val maxDays = deliveryEtaMaxDays ?: return null
        return Offer.DeliveryEta(minBusinessDays = deliveryEtaMinDays, maxBusinessDays = maxDays)
    }
}