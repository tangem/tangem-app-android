package com.tangem.data.jointaccount.gas

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.transaction.GaslessTransactionRepository
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.MathContext

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EvmJointGasTokenResolverTest {

    private val gaslessTransactionRepository: GaslessTransactionRepository = mockk()
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier = mockk()

    private val resolver = EvmJointGasTokenResolver(
        gaslessTransactionRepository = gaslessTransactionRepository,
        singleQuoteStatusSupplier = singleQuoteStatusSupplier,
    )

    private val usdt = MockCryptoCurrencyFactory().createToken(
        blockchain = Blockchain.Ethereum,
        id = "tether",
        contractAddress = TOKEN_CONTRACT,
    ) as CryptoCurrency.Token
    private val network = usdt.network

    @BeforeEach
    fun resetMocks() {
        clearMocks(gaslessTransactionRepository, singleQuoteStatusSupplier)
        coEvery { gaslessTransactionRepository.getSupportedTokens(network) } returns setOf(usdt)
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(params = quoteParams("ethereum")) } returns
            quote("ethereum", fiatRate = "3000")
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(params = quoteParams("tether")) } returns
            quote("tether", fiatRate = "1.5")
    }

    @Test
    fun `GIVEN the coin address WHEN resolve THEN coin without asking the gasless service`() = runTest {
        // Act
        val actual = resolver.resolve(network = network, address = SafeGasToken.Coin.address, rawCurrencyId = null)

        // Assert
        assertThat(actual).isEqualTo(SafeGasToken.Coin.right())
        coVerify(exactly = 0) { gaslessTransactionRepository.getSupportedTokens(any()) }
    }

    @Test
    fun `GIVEN a smart gas token WHEN resolve THEN priced with the coin to token fiat rate`() = runTest {
        // Act
        val actual = resolver.resolve(
            network = network,
            address = TOKEN_CONTRACT.uppercase().replace("0X", "0x"),
            rawCurrencyId = TETHER,
        )

        // Assert
        val expected = SafeGasToken.Token(
            address = TOKEN_CONTRACT.lowercase(),
            decimals = usdt.decimals,
            coinDecimals = 18,
            nativeToTokenRate = BigDecimal("3000").divide(BigDecimal("1.5"), MathContext.DECIMAL128),
        )
        assertThat(actual).isEqualTo(expected.right())
    }

    @Test
    fun `GIVEN the coin address without the 0x prefix WHEN resolve THEN coin`() = runTest {
        // Act
        val actual = resolver.resolve(
            network = network,
            address = SafeGasToken.Coin.address.removePrefix(prefix = "0x"),
            rawCurrencyId = null,
        )

        // Assert
        assertThat(actual).isEqualTo(SafeGasToken.Coin.right())
    }

    @Test
    fun `GIVEN a token outside smart gas WHEN resolve THEN null`() = runTest {
        // Act
        val actual = resolver.resolve(
            network = network,
            address = "0x1111111111111111111111111111111111111111",
            rawCurrencyId = TETHER,
        )

        // Assert
        assertThat(actual).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN no quote for the token WHEN resolve THEN error`() = runTest {
        // Arrange
        coEvery { singleQuoteStatusSupplier.getSyncOrNull(params = quoteParams("tether")) } returns null

        // Act
        val actual = resolver.resolve(network = network, address = TOKEN_CONTRACT, rawCurrencyId = TETHER)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    /** The Smart Gas list carries contracts only; its tokens have no quote id of their own */
    @Test
    fun `GIVEN a listed token whose own id has no quote id WHEN resolve THEN the caller's quote id prices it`() =
        runTest {
            // Arrange
            val listedWithoutQuoteId = MockCryptoCurrencyFactory().createToken(
                blockchain = Blockchain.Ethereum,
                contractAddress = TOKEN_CONTRACT,
            )
            coEvery { gaslessTransactionRepository.getSupportedTokens(network) } returns setOf(listedWithoutQuoteId)

            // Act
            val actual = resolver.resolve(network = network, address = TOKEN_CONTRACT, rawCurrencyId = TETHER)

            // Assert
            assertThat(actual.getOrNull()).isInstanceOf(SafeGasToken.Token::class.java)
        }

    @Test
    fun `GIVEN no quote id from the caller WHEN resolve a token THEN error`() = runTest {
        // Act
        val actual = resolver.resolve(network = network, address = TOKEN_CONTRACT, rawCurrencyId = null)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    private fun quoteParams(rawId: String) =
        SingleQuoteStatusProducer.Params(rawCurrencyId = CryptoCurrency.RawID(rawId))

    /** The app-currency rate is deliberately off: only the USD one is read */
    private fun quote(rawId: String, fiatRate: String) = QuoteStatus(
        rawCurrencyId = CryptoCurrency.RawID(rawId),
        value = QuoteStatus.Data(
            source = StatusSource.ACTUAL,
            fiatRate = BigDecimal(fiatRate).multiply(BigDecimal("150")),
            fiatRateUSD = BigDecimal(fiatRate),
            priceChange = BigDecimal.ZERO,
        ),
    )

    private companion object {
        val TETHER = CryptoCurrency.RawID("tether")
        const val TOKEN_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
    }
}