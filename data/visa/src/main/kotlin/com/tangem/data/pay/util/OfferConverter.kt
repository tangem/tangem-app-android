package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.CustomerOffersResponse
import com.tangem.data.visa.utils.getJavaCurrencyByCode
import com.tangem.domain.pay.model.Offer
import com.tangem.domain.pay.model.OrderType
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger

internal object OfferConverter : Converter<CustomerOffersResponse.Offer, Offer?> {

    private const val TAG = "OfferConverter"
    private const val MAIN_IMAGE_TYPE = "MAIN"

    override fun convert(value: CustomerOffersResponse.Offer): Offer? {
        val fee = value.fee?.toFee() ?: return value.drop(reason = "no fee")
        val data = value.data?.toData() ?: return value.drop(reason = "no order data")
        return Offer(
            type = Offer.Type.fromString(value.type),
            fee = fee,
            data = data,
            mainImageUrl = mainImageUrl(value),
        )
    }

    fun mainImageUrl(value: CustomerOffersResponse.Offer): String? = value.images
        .orEmpty()
        .firstOrNull { it.type.equals(MAIN_IMAGE_TYPE, ignoreCase = true) }
        ?.url

    private fun CustomerOffersResponse.Offer.drop(reason: String): Offer? {
        TangemLogger.withTag(TAG).i("Offer '$type' is not orderable and was skipped: $reason")
        return null
    }

    private fun CustomerOffersResponse.Fee.toFee(): Offer.Fee? {
        val amount = amount ?: return null
        return Offer.Fee(amount = amount, currency = getJavaCurrencyByCode(currency.orEmpty()))
    }

    private fun CustomerOffersResponse.Data.toData(): Offer.Data? {
        val orderType = orderType?.takeUnless(String::isBlank) ?: return null
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