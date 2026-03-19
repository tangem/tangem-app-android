package com.tangem.domain.tokens.wallet.implementor

import com.google.common.truth.Truth
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.common.tokens.CardCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.FetchingSource
import com.tangem.domain.tokens.wallet.WalletFetchingSource
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SingleWalletWithTokenBalanceFetcherTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()

    private val cardCryptoCurrencyFactory: CardCryptoCurrencyFactory = mockk()

    private val fetcher = SingleWalletWithTokenBalanceFetcher(
        cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(cardCryptoCurrencyFactory)
    }

    @Test
    fun getFetchingSources() {
        // Act
        val actual = fetcher.fetchingSources

        // Assert
        val expected = setOf(
            WalletFetchingSource.Balance(
                sources = setOf(FetchingSource.NETWORK, FetchingSource.QUOTE),
            ),
        )
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun getCryptoCurrencies() = runTest {
        // Arrange
        val currencies = cryptoCurrencyFactory.ethereumAndStellar
        val coldWallet = mockk<UserWallet.Cold>()

        every {
            cardCryptoCurrencyFactory.createCurrenciesForSingleCurrencyCardWithToken(coldWallet)
        } returns currencies

        // Act
        val actual = fetcher.getCryptoCurrencies(userWallet = coldWallet)

        // Assert
        val expected = currencies.toSet()
        Truth.assertThat(actual).isEqualTo(expected)
    }
}