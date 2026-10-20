package com.tangem.data.collectibles.converter

import com.tangem.data.collectibles.CollectiblesConst.CENTS_SCALE
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.grow.datasource.collectibles.models.response.BuybackAvailabilityResponse
import com.tangem.utils.converter.Converter
import java.time.Instant

/** No offer until the backend gives both the go-ahead and a price; a malformed expiry is dropped, not fatal. */
internal object CollectibleBuybackOfferConverter : Converter<BuybackAvailabilityResponse, CollectibleBuybackOffer?> {

    override fun convert(value: BuybackAvailabilityResponse): CollectibleBuybackOffer? {
        val amount = value.amount?.takeIf { value.isAvailable } ?: return null

        return CollectibleBuybackOffer(
            amount = amount.toBigDecimal().movePointLeft(CENTS_SCALE),
            expiresAt = value.expiresAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        )
    }
}