package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CustomerOffersResponse
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.utils.converter.Converter
import java.util.Currency

internal object OfferConverter : Converter<CustomerOffersResponse.Offer, Offer> {

    private const val MAIN_IMAGE_TYPE = "MAIN"

    override fun convert(value: CustomerOffersResponse.Offer): Offer {
        return Offer(
            type = Offer.Type.fromString(value.type),
            fee = Offer.Fee(amount = value.fee.amount, currency = Currency.getInstance(value.fee.currency)),
            data = Offer.Data(
                specificationName = value.data.specificationName,
                orderType = OrderType.fromString(value.data.orderType),
                deliveryEta = value.data.toDeliveryEta(),
            ),
            mainImageUrl = value.images.firstOrNull { it.type.equals(MAIN_IMAGE_TYPE, ignoreCase = true) }?.url,
        )
    }

    private fun CustomerOffersResponse.Data.toDeliveryEta(): Offer.DeliveryEta? {
        val maxDays = deliveryEtaMaxDays ?: return null
        return Offer.DeliveryEta(minBusinessDays = deliveryEtaMinDays, maxBusinessDays = maxDays)
    }
}