package com.tangem.features.onramp.settings.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.settings.OnrampSettingsComponent
import com.tangem.features.onramp.settings.entity.OnrampSettingsItemUM
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampSettingsModelTest : OnrampModelTestBase() {

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