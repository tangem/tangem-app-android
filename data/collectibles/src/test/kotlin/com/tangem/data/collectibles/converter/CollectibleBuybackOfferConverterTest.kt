package com.tangem.data.collectibles.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.models.CollectibleBuybackOffer
import com.tangem.grow.datasource.collectibles.models.response.BuybackAvailabilityResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CollectibleBuybackOfferConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN an availability response WHEN convert THEN the offer is built or absent`(model: OfferModel) {
        // Act
        val actual = CollectibleBuybackOfferConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class OfferModel(
        val description: String,
        val response: BuybackAvailabilityResponse,
        val expected: CollectibleBuybackOffer?,
    ) {
        override fun toString() = description
    }

    private fun provideTestModels() = listOf(
        OfferModel(
            description = "available: cents become dollars, expiry parsed",
            response = createResponse(),
            expected = CollectibleBuybackOffer(amount = BigDecimal("42.50"), expiresAt = EXPIRES_AT),
        ),
        OfferModel(
            description = "not available yet",
            response = createResponse(isAvailable = false),
            expected = null,
        ),
        OfferModel(
            description = "available without an amount",
            response = createResponse(amount = null),
            expected = null,
        ),
        OfferModel(
            description = "malformed expiry",
            response = createResponse(expiresAt = "in three days"),
            expected = CollectibleBuybackOffer(amount = BigDecimal("42.50"), expiresAt = null),
        ),
        OfferModel(
            description = "no expiry",
            response = createResponse(expiresAt = null),
            expected = CollectibleBuybackOffer(amount = BigDecimal("42.50"), expiresAt = null),
        ),
    )

    private fun createResponse(
        isAvailable: Boolean = true,
        amount: Long? = 4250L,
        expiresAt: String? = "2026-10-18T12:00:00Z",
    ) = BuybackAvailabilityResponse(isAvailable = isAvailable, amount = amount, expiresAt = expiresAt)

    private companion object {
        val EXPIRES_AT: Instant = Instant.parse("2026-10-18T12:00:00Z")
    }
}