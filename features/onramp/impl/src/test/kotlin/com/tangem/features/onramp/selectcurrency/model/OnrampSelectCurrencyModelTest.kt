package com.tangem.features.onramp.selectcurrency.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.FetchOnrampCurrenciesUseCase
import com.tangem.domain.onramp.GetOnrampCurrenciesUseCase
import com.tangem.domain.onramp.OnrampSaveDefaultCurrencyUseCase
import com.tangem.domain.onramp.model.OnrampCurrencies
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.selectcurrency.SelectCurrencyComponent
import com.tangem.features.onramp.selectcurrency.entity.CurrenciesListUM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampSelectCurrencyModelTest : OnrampModelTestBase() {
    private val wallet: UserWallet = mockk()
    private val getCurrencies: GetOnrampCurrenciesUseCase = mockk()
    private val fetchCurrencies: FetchOnrampCurrenciesUseCase = mockk()
    private val saveCurrency: OnrampSaveDefaultCurrencyUseCase = mockk()
    private val dismiss: () -> Unit = mockk(relaxed = true)
    private val usd = OnrampCurrency("Dollar", "USD", null, 2, "$")
    private val eur = OnrampCurrency("Euro", "EUR", null, 2, "€")

    @Test
    fun `GIVEN currencies WHEN searched and selected THEN matching currency is saved before dismissal`() = runTest {
        // Arrange
        every { getCurrencies() } returns flowOf(OnrampCurrencies(listOf(usd), listOf(eur)).right())
        coEvery { fetchCurrencies(wallet) } returns Unit.right()
        coEvery { saveCurrency(eur) } returns Unit.right()
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.state.value.searchBarUM.onQueryChange("eUr")
        advanceUntilIdle()
        val content = model.state.value as CurrenciesListUM.Content
        content.sections.single().items.single().onClick()
        advanceUntilIdle()

        // Assert
        assertThat(content.sections.single().items.single().onrampCurrency).isEqualTo(eur)
        coVerifyOrder { saveCurrency(eur); dismiss() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN fetch error WHEN retried THEN loading state returns and fetch runs again`() = runTest {
        // Arrange
        every { getCurrencies() } returns emptyFlow()
        coEvery { fetchCurrencies(wallet) } returnsMany listOf(OnrampError.PairsNotFound.left(), Unit.right())
        val model = createModel()
        advanceUntilIdle()
        val error = model.state.value as CurrenciesListUM.Error

        // Act
        error.onRetry()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value).isInstanceOf(CurrenciesListUM.Loading::class.java)
        coVerify(exactly = 2) { fetchCurrencies(wallet) }
        model.onDestroy()
    }

    private fun TestScope.createModel() = OnrampSelectCurrencyModel(
        dispatchers = testDispatchers(),
        analyticsEventHandler = analyticsEventHandler,
        searchManager = InputManager(),
        getOnrampCurrenciesUseCase = getCurrencies,
        saveDefaultCurrencyUseCase = saveCurrency,
        fetchOnrampCurrenciesUseCase = fetchCurrencies,
        paramsContainer = MutableParamsContainer(SelectCurrencyComponent.Params(wallet, cryptoCurrency, dismiss)),
    )
}