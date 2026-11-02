package com.tangem.data.onramp.converters

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.grow.datasource.onramp.models.response.OnrampItemResponse
import org.junit.jupiter.api.Test

internal class StatusConverterTest {

    private val converter = StatusConverter(
        Moshi.Builder().build(),
    )

    @Test
    fun `GIVEN response WHEN convert THEN all status fields are mapped`() {
        // Arrange
        val response = OnrampItemResponse(
            txId = "tx-1",
            providerId = "provider",
            payoutAddress = "address",
            status = "payment-processing",
            failReason = "",
            externalTxId = "external-1",
            externalTxUrl = "https://provider/tx-1",
            payoutHash = "hash",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:01:00Z",
            fromCurrencyCode = "USD",
            fromAmount = "10.00",
            fromPrecision = 2,
            toContractAddress = "0x0",
            toNetwork = "ethereum",
            toDecimals = 6,
            toAmount = "1000000",
            toActualAmount = "999000",
            paymentMethod = "card",
            countryCode = "US",
        )

        // Act
        val actual = converter.convert(response)

        // Assert
        assertThat(actual).isEqualTo(
            OnrampStatus(
                txId = "tx-1",
                providerId = "provider",
                payoutAddress = "address",
                status = OnrampStatus.Status.PaymentProcessing,
                failReason = "",
                externalTxId = "external-1",
                externalTxUrl = "https://provider/tx-1",
                payoutHash = "hash",
                createdAt = "2026-01-01T00:00:00Z",
                fromCurrencyCode = "USD",
                fromAmount = "10.00",
                toContractAddress = "0x0",
                toNetwork = "ethereum",
                toDecimals = "6",
                toAmount = "1000000",
                toActualAmount = "999000",
                paymentMethod = "card",
                countryCode = "US",
            ),
        )
    }

    @Test
    fun `GIVEN unknown status WHEN convert THEN conversion fails`() {
        // Arrange
        val response = OnrampItemResponse(
            txId = "tx-1", providerId = "provider", payoutAddress = "address", status = "unknown",
            failReason = null, externalTxId = null, externalTxUrl = null, payoutHash = null,
            createdAt = "", updatedAt = "", fromCurrencyCode = "USD", fromAmount = "1", fromPrecision = 2,
            toContractAddress = "0x0", toNetwork = "ethereum", toDecimals = 6, toAmount = null,
            toActualAmount = null, paymentMethod = "card", countryCode = "US",
        )

        // Act
        val error = runCatching { converter.convert(response) }.exceptionOrNull()

        // Assert
        assertThat(error).isNotNull()
    }
}