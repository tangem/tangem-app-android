package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.usecase.GetPredictionTopUpMinimumUseCase
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

internal class PredictionTopUpLimitsResolverTest {

    private val getPredictionTopUpMinimumUseCase: GetPredictionTopUpMinimumUseCase = mockk()
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase = mockk()

    private val resolver = PredictionTopUpLimitsResolver(
        getPredictionTopUpMinimumUseCase = getPredictionTopUpMinimumUseCase,
        getCurrencyUSDQuoteUseCase = getCurrencyUSDQuoteUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(getPredictionTopUpMinimumUseCase, getCurrencyUSDQuoteUseCase)
        coEvery { getPredictionTopUpMinimumUseCase(USER_WALLET_ID) } returns BigDecimal("2")
        coEvery { getCurrencyUSDQuoteUseCase(USDC_RAW_ID) } returns BigDecimal("0.9999")
    }

    @Test
    fun `GIVEN minimum and a non-1 rate WHEN resolve THEN limits are converted to the token rounding up`() = runTest {
        // Act
        val actual = resolver.resolve(USER_WALLET_ID, usdc())

        // Assert
        assertThat(actual?.minimum).isEqualTo(BigDecimal("2.000201"))
        assertThat(actual?.maximum).isEqualTo(BigDecimal("50005.000501"))
        assertThat(actual?.minimumText).contains("2.00")
        assertThat(actual?.maximumText).contains("50,000.00")
    }

    @Test
    fun `GIVEN no minimum WHEN resolve THEN no limits`() = runTest {
        // Arrange
        coEvery { getPredictionTopUpMinimumUseCase(USER_WALLET_ID) } returns null

        // Act
        val actual = resolver.resolve(USER_WALLET_ID, usdc())

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN no USD quote WHEN resolve THEN no limits`() = runTest {
        // Arrange
        coEvery { getCurrencyUSDQuoteUseCase(USDC_RAW_ID) } returns null

        // Act
        val actual = resolver.resolve(USER_WALLET_ID, usdc())

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = ["0", "-1"])
    fun `GIVEN a non-positive USD quote WHEN resolve THEN no limits`(rate: String) = runTest {
        // Arrange
        coEvery { getCurrencyUSDQuoteUseCase(USDC_RAW_ID) } returns BigDecimal(rate)

        // Act
        val actual = resolver.resolve(USER_WALLET_ID, usdc())

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a currency without raw id WHEN resolve THEN no limits`() = runTest {
        // Act
        val actual = resolver.resolve(USER_WALLET_ID, usdc(rawId = null))

        // Assert
        assertThat(actual).isNull()
    }

    private fun usdc(rawId: CryptoCurrency.RawID? = USDC_RAW_ID): CryptoCurrency {
        val currencyId: CryptoCurrency.ID = mockk { every { rawCurrencyId } returns rawId }
        return mockk {
            every { id } returns currencyId
            every { decimals } returns 6
        }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId(stringValue = "deadbeef")
        val USDC_RAW_ID = CryptoCurrency.RawID("usd-coin")
    }
}