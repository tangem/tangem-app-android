package com.tangem.features.onramp.settings.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.settings.OnrampSettingsComponent
import com.tangem.features.onramp.settings.entity.OnrampSettingsItemUM
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampSettingsModelTest : OnrampModelTestBase() {

    @Test
    fun `GIVEN country stream WHEN error arrives and model is destroyed THEN state is retained and collection stops`() =
        runTest {
            // Arrange
            val countries = MutableStateFlow<Either<OnrampError, OnrampCountry?>>(null.right())
            val getCountry = mockk<GetOnrampCountryUseCase>()
            every { getCountry() } returns countries
            every { cryptoCurrency.symbol } returns "BTC"
            val model = OnrampSettingsModel(
                dispatchers = testDispatchers(),
                analyticsEventHandler = analyticsEventHandler,
                getOnrampCountryUseCase = getCountry,
                paramsContainer = MutableParamsContainer(
                    OnrampSettingsComponent.Params(userWalletId, cryptoCurrency, {}),
                ),
            )
            advanceUntilIdle()
            val initialState = model.state.value

            // Act
            countries.value = OnrampError.DomainError("offline").left()
            advanceUntilIdle()
            model.onDestroy()
            advanceUntilIdle()
            countries.value = OnrampError.DomainError("after destroy").left()
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value).isEqualTo(initialState)
            assertThat(countries.subscriptionCount.value).isEqualTo(0)
            verify(exactly = 1) {
                analyticsEventHandler.send(OnrampAnalyticsEvent.AppErrors("BTC", "offline"))
            }
            verify(exactly = 0) {
                analyticsEventHandler.send(OnrampAnalyticsEvent.AppErrors("BTC", "after destroy"))
            }
        }

    @Test
    fun `GIVEN country is available WHEN model starts THEN residence item is populated`() = runTest {
        // Arrange
        val country = OnrampCountry(
            id = "se",
            name = "Sweden",
            code = "SE",
            image = "flag",
            alpha3 = "SWE",
            continent = "Europe",
            defaultCurrency = com.tangem.domain.onramp.model.OnrampCurrency("Euro", "EUR", null, 2, "€"),
            isOnrampAvailable = true,
        )
        val getCountry = mockk<GetOnrampCountryUseCase>()
        every { getCountry() } returns flowOf(country.right())
        val onBack: () -> Unit = mockk(relaxed = true)

        // Act
        val model = OnrampSettingsModel(
            dispatchers = testDispatchers(),
            analyticsEventHandler = analyticsEventHandler,
            getOnrampCountryUseCase = getCountry,
            paramsContainer = MutableParamsContainer(
                OnrampSettingsComponent.Params(userWalletId, cryptoCurrency, onBack),
            ),
        )
        advanceUntilIdle()

        // Assert
        val residence = model.state.value.items.single() as OnrampSettingsItemUM.Residence
        assertThat(residence.countryName).isEqualTo("Sweden")
        assertThat(residence.flagUrl).isEqualTo("flag")
        model.onDestroy()
    }
}