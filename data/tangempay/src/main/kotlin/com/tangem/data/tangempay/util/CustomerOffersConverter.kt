package com.tangem.data.tangempay.util

import com.tangem.domain.tangempay.model.CustomerOffers
import com.tangem.domain.tangempay.model.Offer
import com.tangem.spend.datasource.tangempay.models.response.CustomerOffersResponse
import com.tangem.utils.converter.Converter

internal object CustomerOffersConverter : Converter<CustomerOffersResponse, CustomerOffers> {

    override fun convert(value: CustomerOffersResponse): CustomerOffers {
        val offers = value.result.orEmpty()
        return CustomerOffers(
            orderable = offers.mapNotNull(OfferConverter::convert),
            artwork = offers.mapNotNull(::artworkEntry).toMap(),
        )
    }

    private fun artworkEntry(value: CustomerOffersResponse.Offer): Pair<Offer.Type, String>? {
        val type = Offer.Type.fromString(value.type).takeUnless { it == Offer.Type.UNKNOWN } ?: return null
        val url = OfferConverter.mainImageUrl(value) ?: return null
        return type to url
    }
}