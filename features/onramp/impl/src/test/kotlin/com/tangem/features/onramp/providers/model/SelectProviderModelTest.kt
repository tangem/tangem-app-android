package com.tangem.features.onramp.providers.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.onramp.GetOnrampPaymentMethodsUseCase
import com.tangem.domain.onramp.GetOnrampProviderWithQuoteUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.*
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.providers.SelectProviderComponent
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import com.tangem.features.onramp.providers.entity.SelectProviderResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SelectProviderModelTest : OnrampModelTestBase() {
    private val paymentMethod = OnrampPaymentMethod("card", "Card", "", PaymentMethodType.CARD)
    private val onProviderClick: (SelectProviderResult, Boolean) -> Unit = mockk(relaxed = true)
    private val onDismiss: () -> Unit = mockk(relaxed = true)

    @ParameterizedTest
    @ValueSource(strings = ["90", "90.00", "90.00000000"])
    fun `GIVEN scaled data quotes WHEN provider selected THEN difference and callback are correct`(value: String) =
        runTest {
            // Arrange
            every { cryptoCurrency.symbol } returns "BTC"
            val best = quote(id = "best", value = "100")
            val other = quote(id = "other", value = value)
            val model = createModel(listOf(other, best))

            // Act
            advanceUntilIdle()
            val items = model.state.value.selectedPaymentMethod.providers
                .map { it as ProviderListItemUM.Available.Content }
            items.last().onClick()

            // Assert
            assertThat(items.map { it.providerId }).containsExactly("best", "other").inOrder()
            assertThat(items.last().diffRate).isEqualTo(stringReference("-${BigDecimal("0.1").format { percent() }}"))
            assertThat(items.first().isBestRate).isTrue()
            assertThat(model.state.value.selectedProviderId).isEqualTo("other")
            verify(exactly = 1) {
                onProviderClick(
                    SelectProviderResult.ProviderWithQuote(
                        other.paymentMethod, other.provider, other.fromAmount, other.toAmount,
                    ),
                    false,
                )
            }
            verify(exactly = 1) { onDismiss() }
            verify(exactly = 1) {
                analyticsEventHandler.send(OnrampAnalyticsEvent.OnProviderChosen("other", "BTC"))
            }
            model.onDestroy()
        }

    @ParameterizedTest
    @ValueSource(strings = ["0", "100"])
    fun `GIVEN single provider WHEN loaded THEN difference and best rate badge are absent`(value: String) = runTest {
        // Arrange
        val model = createModel(listOf(quote(value = value)))

        // Act
        advanceUntilIdle()

        // Assert
        val item = model.state.value.selectedPaymentMethod.providers.single() as ProviderListItemUM.Available.Content
        assertThat(item.diffRate).isNull()
        assertThat(item.isBestRate).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN zero best amount with multiple providers WHEN loaded THEN differences are absent`() = runTest {
        // Arrange
        val model = createModel(listOf(quote(id = "first", value = "0"), quote(id = "second", value = "0.00")))

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.selectedPaymentMethod.providers.map {
            (it as ProviderListItemUM.Available.Content).diffRate
        }).containsExactly(null, null)
        model.onDestroy()
    }

    private fun kotlinx.coroutines.test.TestScope.createModel(data: List<OnrampProviderWithQuote>) = SelectProviderModel(
        testDispatchers(),
        analyticsEventHandler,
        mockk<GetOnrampPaymentMethodsUseCase> { coEvery { this@mockk.invoke() } returns setOf(paymentMethod).right() },
        mockk<GetOnrampProviderWithQuoteUseCase> {
            coEvery { this@mockk.invoke(paymentMethod) } returns data.right()
        },
        mockk<GetUserCountryUseCase> { every { invokeSync() } returns UserCountry.Other("us").right() },
        MutableParamsContainer(
            SelectProviderComponent.Params(onProviderClick, onDismiss, paymentMethod, "best", cryptoCurrency),
        ),
    )

    private fun quote(id: String = "best", value: String = "100") = OnrampProviderWithQuote.Data(
        provider = OnrampProvider(id, OnrampProviderInfo(id, "", null, null), listOf(paymentMethod)),
        paymentMethod = paymentMethod,
        fromAmount = OnrampAmount("USD", BigDecimal.TEN, 2),
        toAmount = OnrampAmount("BTC", BigDecimal(value), 8),
    )

    @Test
    fun `GIVEN unsupported payment provider WHEN loaded THEN payment selection stays disabled`() = runTest {
        // Arrange
        val params = mockk<SelectProviderComponent.Params>(relaxed = true)
        val methods = mockk<GetOnrampPaymentMethodsUseCase>()
        val quotes = mockk<GetOnrampProviderWithQuoteUseCase>()
        val country = mockk<GetUserCountryUseCase> {
            every { invokeSync() } returns mockk<com.tangem.domain.settings.usercountry.models.UserCountry>().right()
        }
        val unavailable = mockk<OnrampProviderWithQuote.Unavailable.NotSupportedPaymentMethod>(relaxed = true)
        every { unavailable.availablePaymentMethods } returns emptyList()
        coEvery { methods() } returns setOf(params.selectedPaymentMethod).right()
        coEvery { quotes(params.selectedPaymentMethod) } returns listOf(unavailable).right()
        val model = SelectProviderModel(
            testDispatchers(), analyticsEventHandler, methods, quotes, country, MutableParamsContainer(params),
        )

        // Act
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.paymentMethods).isEmpty()
        assertThat(model.state.value.isPaymentMethodClickEnabled).isFalse()
        assertThat(model.state.value.selectedPaymentMethod.providers.single())
            .isInstanceOf(ProviderListItemUM.Unavailable::class.java)
        model.onDestroy()
    }
}