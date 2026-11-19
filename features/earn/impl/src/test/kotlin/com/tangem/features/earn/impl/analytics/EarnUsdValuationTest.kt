package com.tangem.features.earn.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnUsdValuationTest {

    private val getCurrencyUSDQuote: GetCurrencyUSDQuoteUseCase = mockk()
    private val currencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = currencyFactory.ethereum

    private val valuation = EarnUsdValuation(getCurrencyUSDQuote)

    @BeforeEach
    fun resetMocks() {
        clearMocks(getCurrencyUSDQuote)
    }

    @Test
    fun `GIVEN quote 2000 WHEN bucket of 0 point 1 ETH THEN 50-250`() = runTest {
        // Arrange
        coEvery { getCurrencyUSDQuote(ethereum.id.rawCurrencyId!!) } returns BigDecimal("2000")

        // Act
        val actual = valuation.bucket(currency = ethereum, cryptoAmount = BigDecimal("0.1"))

        // Assert
        assertThat(actual).isEqualTo("50-250")
    }

    @Test
    fun `GIVEN quote null WHEN bucket THEN null`() = runTest {
        // Arrange
        coEvery { getCurrencyUSDQuote(ethereum.id.rawCurrencyId!!) } returns null

        // Act
        val actual = valuation.bucket(currency = ethereum, cryptoAmount = BigDecimal("0.1"))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN amount null WHEN bucket THEN null and quote not requested`() = runTest {
        // Act
        val actual = valuation.bucket(currency = ethereum, cryptoAmount = null)

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { getCurrencyUSDQuote(any()) }
    }

    @Test
    fun `GIVEN currency without rawCurrencyId WHEN bucket THEN null and quote not requested`() = runTest {
        // Arrange
        val customToken = createTokenWithoutRawCurrencyId()

        // Act
        val actual = valuation.bucket(currency = customToken, cryptoAmount = BigDecimal("0.1"))

        // Assert
        assertThat(actual).isNull()
        coVerify(exactly = 0) { getCurrencyUSDQuote(any()) }
    }

    private fun createTokenWithoutRawCurrencyId(): CryptoCurrency {
        val token = currencyFactory.createToken(blockchain = Blockchain.Ethereum)

        return token.copy(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                suffix = CryptoCurrency.ID.Suffix.ContractAddress(token.contractAddress),
            ),
            isCustom = true,
        )
    }
}