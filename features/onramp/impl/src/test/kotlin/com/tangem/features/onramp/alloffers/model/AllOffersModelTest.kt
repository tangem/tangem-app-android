package com.tangem.features.onramp.alloffers.model

import arrow.core.left
import arrow.core.right
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampAllOffersUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampPaymentMethodGroup
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.alloffers.AllOffersComponent
import com.tangem.features.onramp.alloffers.entity.AllOffersStateUM
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AllOffersModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN active failed source WHEN refreshed THEN old source cancelled and replacement content retained`() =
        runTest {
            // Arrange
            val old = MutableStateFlow<Either<OnrampError, List<OnrampPaymentMethodGroup>>>(
                OnrampError.DomainError("offline").left(),
            )
            val replacement = MutableStateFlow<Either<OnrampError, List<OnrampPaymentMethodGroup>>>(
                emptyList<OnrampPaymentMethodGroup>().right(),
            )
            val offers = mockk<GetOnrampAllOffersUseCase>()
            every { offers(any(), any()) } returnsMany listOf(old, replacement)
            val params = mockk<AllOffersComponent.Params>(relaxed = true)
            every { params.isBuyEnabled } returns MutableStateFlow(true)
            val model = AllOffersModel(
                testDispatchers(), analyticsEventHandler, offers, mockk(), mockk(),
                MutableParamsContainer(params),
            )
            advanceUntilIdle()
            assertThat(old.subscriptionCount.value).isEqualTo(1)

            // Act
            model.onRefresh()
            advanceUntilIdle()
            val content = model.state.value
            old.value = OnrampError.DomainError("late old error").left()
            advanceUntilIdle()

            // Assert
            assertThat(content).isInstanceOf(AllOffersStateUM.Content::class.java)
            assertThat(model.state.value).isEqualTo(content)
            assertThat(old.subscriptionCount.value).isEqualTo(0)
            assertThat(replacement.subscriptionCount.value).isEqualTo(1)
            model.onDestroy()
            advanceUntilIdle()
            assertThat(replacement.subscriptionCount.value).isEqualTo(0)
        }

    @Test
    fun `GIVEN failed offers WHEN refreshed THEN subscription restarts and restricted buy stays inert`() = runTest {
        // Arrange
        val params = mockk<AllOffersComponent.Params>(relaxed = true)
        every { params.isBuyEnabled } returns MutableStateFlow(true)
        val offers = mockk<GetOnrampAllOffersUseCase>()
        var subscriptions = 0
        every { offers(any(), any()) } returns flow {
            subscriptions++
            emit(OnrampError.DomainError("offline").left())
        }
        val model = AllOffersModel(
            testDispatchers(), analyticsEventHandler, offers, mockk(), mockk(),
            MutableParamsContainer(params),
        )
        advanceUntilIdle()
        val error = model.state.value

        // Act
        model.onRefresh()
        model.onBuyClick(mockk(), OnrampOfferAdvantagesUM.Restricted)
        advanceUntilIdle()

        // Assert
        assertThat(subscriptions).isEqualTo(2)
        assertThat(error).isInstanceOf(AllOffersStateUM.Error::class.java)
        verify(exactly = 0) { params.openRedirectPage(any()) }
        verify(exactly = 0) { params.onDismiss() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN buy disabled WHEN buy clicked THEN no analytics dismiss or redirect`() = runTest {
        // Arrange
        val params = createBuyParams(isBuyEnabled = MutableStateFlow(false))
        val model = createBuyModel(params)
        advanceUntilIdle()
        clearMocks(analyticsEventHandler, answers = false)

        // Act
        model.onBuyClick(createQuote(), OnrampOfferAdvantagesUM.GreatRate)

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
        verify(exactly = 0) { params.onDismiss() }
        verify(exactly = 0) { params.openRedirectPage(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN buy re-enabled WHEN buy clicked THEN buy analytics sent and purchase opened`() = runTest {
        // Arrange
        val isBuyEnabled = MutableStateFlow(false)
        val params = createBuyParams(isBuyEnabled)
        val model = createBuyModel(params)
        val quote = createQuote()
        advanceUntilIdle()
        model.onBuyClick(quote, OnrampOfferAdvantagesUM.GreatRate)
        clearMocks(analyticsEventHandler, answers = false)

        // Act
        isBuyEnabled.value = true
        advanceUntilIdle()
        model.onBuyClick(quote, OnrampOfferAdvantagesUM.GreatRate)

        // Assert
        verifyOrder {
            analyticsEventHandler.send(
                OnrampAnalyticsEvent.OnBuyClick(providerName = "Mercuryo", currency = "USD", tokenSymbol = "BTC"),
            )
            analyticsEventHandler.send(
                OnrampAnalyticsEvent.BestRateClicked(
                    tokenSymbol = "BTC",
                    providerName = "Mercuryo",
                    paymentMethod = "Card",
                ),
            )
            params.onDismiss()
            params.openRedirectPage(quote)
        }
        model.onDestroy()
    }

    private fun createBuyParams(isBuyEnabled: StateFlow<Boolean>): AllOffersComponent.Params {
        val params = mockk<AllOffersComponent.Params>(relaxed = true)
        every { params.isBuyEnabled } returns isBuyEnabled
        every { params.amountCurrencyCode } returns "USD"
        every { params.cryptoCurrency.symbol } returns "BTC"
        every { params.userWallet } returns mockk<UserWallet.Hot>(relaxed = true)
        return params
    }

    private fun TestScope.createBuyModel(params: AllOffersComponent.Params): AllOffersModel {
        val offers = mockk<GetOnrampAllOffersUseCase>()
        every { offers(any(), any()) } returns flowOf(emptyList<OnrampPaymentMethodGroup>().right())
        return AllOffersModel(
            testDispatchers(), analyticsEventHandler, offers, mockk(), mockk(relaxed = true),
            MutableParamsContainer(params),
        )
    }

    private fun createQuote(): OnrampProviderWithQuote.Data {
        val quote = mockk<OnrampProviderWithQuote.Data>()
        every { quote.provider.info.name } returns "Mercuryo"
        every { quote.paymentMethod.name } returns "Card"
        return quote
    }
}