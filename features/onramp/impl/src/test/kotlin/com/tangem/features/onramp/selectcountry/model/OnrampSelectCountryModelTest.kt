package com.tangem.features.onramp.selectcountry.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.*
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.selectcountry.SelectCountryComponent
import com.tangem.features.onramp.selectcountry.entity.CountryItemState
import com.tangem.features.onramp.selectcountry.entity.CountryListUM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampSelectCountryModelTest : OnrampModelTestBase() {
    private val wallet: UserWallet = mockk { every { walletId } returns userWalletId }
    private val wallets: GetWalletsUseCase = mockk { every { invokeSync() } returns listOf(wallet) }
    private val getCountries: GetOnrampCountriesUseCase = mockk()
    private val getCountry: GetOnrampCountryUseCase = mockk {
        every { this@mockk.invoke() } returns flowOf(null.right())
    }
    private val fetchCountries: FetchOnrampCountriesUseCase = mockk()
    private val saveCountry: OnrampSaveDefaultCountryUseCase = mockk()
    private val dismiss: (Boolean) -> Unit = mockk(relaxed = true)
    private val country = OnrampCountry(
        "se", "Sweden", "SE", "flag", "SWE", "Europe",
        OnrampCurrency("Euro", "EUR", null, 2, "€"), true,
    )

    @Test
    fun `GIVEN country WHEN searched by alpha code and selected THEN selection is persisted before dismissal`() = runTest {
        // Arrange
        every { getCountries() } returns flowOf(listOf(country).right())
        coEvery { fetchCountries(wallet) } returns Unit.right()
        coEvery { saveCountry(country) } returns Unit.right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.state.value.searchBarUM.onQueryChange("sWe")
        advanceUntilIdle()
        val item = (model.state.value as CountryListUM.Content).items.single() as CountryItemState.WithContent.Content
        item.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(item.countryName).isEqualTo(country.name)
        coVerifyOrder { saveCountry(country); dismiss(true) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN save fails WHEN country selected again THEN error reported and dismiss follows successful save`() = runTest {
        // Arrange
        every { cryptoCurrency.symbol } returns "BTC"
        every { getCountries() } returns flowOf(listOf(country).right())
        coEvery { fetchCountries(wallet) } returns Unit.right()
        coEvery { saveCountry(country) } returnsMany listOf(OnrampError.DomainError("offline").left(), Unit.right())
        val model = createModel()
        advanceUntilIdle()
        val item = (model.state.value as CountryListUM.Content).items.single() as CountryItemState.WithContent.Content

        // Act
        item.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { dismiss(any()) }
        verify(exactly = 1) { analyticsEventHandler.send(OnrampAnalyticsEvent.AppErrors("BTC", "offline")) }

        // Act
        item.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 2) { saveCountry(country) }
        verify(exactly = 1) { dismiss(true) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN fetch failure WHEN retry clicked THEN fetch repeats and loading is restored`() = runTest {
        // Arrange
        every { getCountries() } returns emptyFlow()
        coEvery { fetchCountries(wallet) } returnsMany listOf(OnrampError.PairsNotFound.left(), Unit.right())
        val model = createModel()
        advanceUntilIdle()

        // Act
        (model.state.value as CountryListUM.Error).onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value).isInstanceOf(CountryListUM.Loading::class.java)
        coVerify(exactly = 2) { fetchCountries(wallet) }
        model.onDestroy()
    }

    private fun TestScope.createModel() = OnrampSelectCountryModel(
        dispatchers = testDispatchers(),
        analyticsEventHandler = analyticsEventHandler,
        searchManager = InputManager(),
        getOnrampCountriesUseCase = getCountries,
        saveDefaultCountryUseCase = saveCountry,
        getOnrampCountryUseCase = getCountry,
        fetchOnrampCountriesUseCase = fetchCountries,
        getWalletsUseCase = wallets,
        paramsContainer = MutableParamsContainer(SelectCountryComponent.Params(userWalletId, cryptoCurrency, dismiss)),
    )
}