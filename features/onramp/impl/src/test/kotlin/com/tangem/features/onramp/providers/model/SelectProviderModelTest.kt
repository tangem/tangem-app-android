package com.tangem.features.onramp.providers.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.onramp.GetOnrampPaymentMethodsUseCase
import com.tangem.domain.onramp.GetOnrampProviderWithQuoteUseCase
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.providers.SelectProviderComponent
import com.tangem.features.onramp.providers.entity.ProviderListItemUM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SelectProviderModelTest : OnrampModelTestBase() {
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