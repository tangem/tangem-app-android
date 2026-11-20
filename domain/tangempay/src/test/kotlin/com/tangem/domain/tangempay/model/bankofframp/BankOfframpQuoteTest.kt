package com.tangem.domain.tangempay.model.bankofframp

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.tangempay.model.FeeCalculationType
import org.joda.time.DateTime
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankOfframpQuoteTest {

    @Test
    fun `GIVEN quote expiry WHEN sendDeadline THEN thirty seconds before it`() {
        // Arrange
        val quote = BankOfframpQuote(
            quoteId = "quote-1",
            bankAccountId = "acc-1",
            feeAmount = BigDecimal("1.50"),
            sourceAmount = BigDecimal("100.00"),
            destinationAmount = BigDecimal("1699.13"),
            destinationCurrency = "MXN",
            exchangeRate = BigDecimal("17.25"),
            fee = OfframpFee(
                name = "SPEI",
                calculationType = FeeCalculationType.PERCENTAGE,
                amount = BigDecimal("1"),
                currency = "USD",
                description = null,
                fixedAmount = BigDecimal("0.50"),
            ),
            eta = OfframpEta(minDays = 0, maxDays = 0),
            expiresAt = DateTime.parse("2026-11-09T12:15:00Z"),
        )

        // Act
        val actual = quote.sendDeadline

        // Assert
        assertThat(actual).isEqualTo(DateTime.parse("2026-11-09T12:14:30Z"))
    }
}