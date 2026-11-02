package com.tangem.features.onramp.confirmresidency.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.onramp.OnrampSaveDefaultCountryUseCase
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.confirmresidency.ConfirmResidencyComponent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConfirmResidencyModelTest : OnrampModelTestBase() {

    @Test
    fun `GIVEN supported country WHEN confirm clicked THEN country is saved and sheet dismissed`() = runTest {
        // Arrange
        val country = country(isAvailable = true)
        val onDismiss: () -> Unit = io.mockk.mockk(relaxed = true)
        val saveCountry = io.mockk.mockk<OnrampSaveDefaultCountryUseCase>()
        coEvery { saveCountry(country) } returns Unit.right()
        val model = createModel(country, saveCountry, onDismiss)

        // Act
        model.state.value.primaryButtonConfig.onClick()
        advanceUntilIdle()

        // Assert
        coVerify { saveCountry(country) }
        verify { onDismiss() }
        assertThat(model.state.value.isCountrySupported).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN unsupported country WHEN close clicked THEN router pops without saving`() = runTest {
        // Arrange
        val country = country(isAvailable = false)
        val saveCountry = io.mockk.mockk<OnrampSaveDefaultCountryUseCase>()
        val model = createModel(country, saveCountry, io.mockk.mockk(relaxed = true))

        // Act
        model.state.value.primaryButtonConfig.onClick()
        advanceUntilIdle()

        // Assert
        verify { router.pop() }
        coVerify(exactly = 0) { saveCountry(any()) }
        assertThat(model.state.value.isCountrySupported).isFalse()
        model.onDestroy()
    }

    private fun kotlinx.coroutines.test.TestScope.createModel(
        country: OnrampCountry,
        saveCountry: OnrampSaveDefaultCountryUseCase,
        onDismiss: () -> Unit,
    ) = ConfirmResidencyModel(
        dispatchers = testDispatchers(),
        analyticsEventHandler = analyticsEventHandler,
        router = router,
        saveDefaultCountryUseCase = saveCountry,
        paramsContainer = MutableParamsContainer(
            ConfirmResidencyComponent.Params(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                country = country,
                onDismiss = onDismiss,
            ),
        ),
    )

    private fun country(isAvailable: Boolean) = OnrampCountry(
        id = "se",
        name = "Sweden",
        code = "SE",
        image = "flag",
        alpha3 = "SWE",
        continent = "Europe",
        defaultCurrency = com.tangem.domain.onramp.model.OnrampCurrency("Euro", "EUR", null, 2, "€"),
        isOnrampAvailable = isAvailable,
    )
}