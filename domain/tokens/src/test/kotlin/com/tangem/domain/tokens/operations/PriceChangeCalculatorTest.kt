package com.tangem.domain.tokens.operations

import arrow.core.nonEmptyListOf
import com.google.common.truth.Truth
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.tokens.mock.MockTokens
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PriceChangeCalculatorTest {

    @Test
    fun `calculate if statuses is empty list`() {
        // Act
        val actual = PriceChangeCalculator.calculate(statuses = emptyList())

        // Assert
        val expected = PriceChange(
            value = BigDecimal("0.00"),
            source = StatusSource.ACTUAL,
        ).lceContent()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `calculate if any status is Loading`() {
        // Arrange
        val statuses = nonEmptyListOf(
            create(
                amount = BigDecimal.ZERO,
                priceChange = BigDecimal("0.06"),
            ),
            CryptoCurrencyStatus(
                currency = MockTokens.token1,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

        // Act
        val actual = PriceChangeCalculator.calculate(statuses)

        // Assert
        val expected = lceLoading<PriceChange>()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `calculate if any status is Unreachable`() {
        // Arrange
        val statuses = nonEmptyListOf(
            create(
                amount = BigDecimal.ZERO,
                priceChange = BigDecimal("0.06"),
            ),
            CryptoCurrencyStatus(
                currency = MockTokens.token1,
                value = CryptoCurrencyStatus.Unreachable(priceChange = null, fiatRate = null, networkAddress = null),
            ),
        )

        // Act
        val actual = PriceChangeCalculator.calculate(statuses)

        // Assert
        val expected = Unit.lceError()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `calculate if total fiat balance is zero`() {
        // Arrange
        val statuses = nonEmptyListOf(
            create(
                amount = BigDecimal.ZERO,
                priceChange = BigDecimal("0.06"),
            ),
            create(
                amount = BigDecimal("0.00"),
                priceChange = BigDecimal("0.00"),
            ),
        )

        // Act
        val actual = PriceChangeCalculator.calculate(statuses)

        // Assert
        val expected = PriceChange(
            value = BigDecimal("0.00"),
            source = StatusSource.ACTUAL,
        ).lceContent()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `calculate if all statuses are loaded`() {
        // Arrange
        val statuses = nonEmptyListOf(
            create(
                amount = BigDecimal("5"), // 50%
                priceChange = BigDecimal("0.06"), // 6%
            ),
            create(
                amount = BigDecimal("3"), // 30%
                priceChange = BigDecimal("-0.02"), // -2%
            ),
            create(
                amount = BigDecimal("2"), // 20%
                priceChange = BigDecimal("0.04"), // 4%
            ),
        )

        // Act
        val actual = PriceChangeCalculator.calculate(statuses)

        // Assert
        val expected = PriceChange(
            value = BigDecimal("0.032"), // 3.2%
            source = StatusSource.ACTUAL,
        ).lceContent()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun create(amount: BigDecimal, priceChange: BigDecimal): CryptoCurrencyStatus {
        val value = CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount,
            fiatRate = BigDecimal.ONE,
            priceChange = priceChange,
            yieldBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    value = "0x123",
                    type = NetworkAddress.Address.Type.Primary,
                ),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        )

        return CryptoCurrencyStatus(currency = MockTokens.token1, value = value)
    }
}