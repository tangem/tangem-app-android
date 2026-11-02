package com.tangem.features.onramp.hottokens.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.hottokens.HotCryptoComponent
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class HotCryptoModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN callback receivers WHEN portfolio and token callbacks fire THEN events reach the active flow`() = runTest {
        // Arrange
        val delegate = HotCryptoModelCallbackDelegate()
        val status = mockk<com.tangem.domain.models.currency.CryptoCurrencyStatus>()
        val portfolio = async { delegate.onChangePortfolioClick.receive() }
        val token = async { delegate.onTokenAdded.receive() }
        runCurrent()

        // Act
        delegate.onChangePortfolioClick()
        delegate.onTokenAdded(status)

        // Assert
        assertThat(portfolio.await()).isEqualTo(Unit)
        assertThat(token.await()).isSameInstanceAs(status)
    }

    @Test
    fun `GIVEN hot tokens WHEN list is cleared THEN header disappears and destroy unsubscribes`() = runTest {
        // Arrange
        val currency = MockCryptoCurrencyFactory().ethereum
        val data = HotCryptoPortfolioData(
            allHotCrypto = listOf(HotCryptoCurrency(currency, QuoteStatus(currency.id.rawCurrencyId!!))),
            wallet = mockk(), appCurrency = AppCurrency.Default, jointSupportedNetworkIds = emptySet(),
        )
        val source = MutableStateFlow(data)
        val loader = mockk<HotCryptoPortfolioDataLoader> { every { loadPortfolioData(userWalletId) } returns source }
        val fetcher = mockk<PortfolioFetcher.Factory> { every { create(any(), any()) } returns mockk() }
        val params = mockk<HotCryptoComponent.Params>(relaxed = true) {
            every { userWalletId } returns this@HotCryptoModelTest.userWalletId
        }
        val model = HotCryptoModel(
            MutableParamsContainer(params), HotCryptoModelCallbackDelegate(), testDispatchers(), loader,
            mockk(), mockk(), fetcher,
        )
        advanceUntilIdle()
        assertThat(model.state.value.items.first()).isInstanceOf(TokensListItemUM.GroupTitle::class.java)
        assertThat(model.state.value.items).hasSize(2)

        // Act
        source.value = data.copy(allHotCrypto = emptyList())
        advanceUntilIdle()
        model.onDestroy()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.items).isEmpty()
        assertThat(source.subscriptionCount.value).isEqualTo(0)
    }
}