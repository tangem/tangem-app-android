package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.jupiter.api.Test

internal class TokenActionsModelTest {

    private val userWallet: UserWallet = mockk(relaxed = true)
    private val firstToken: CryptoCurrencyData = mockk(relaxed = true) {
        every { userWallet } returns this@TokenActionsModelTest.userWallet
    }
    private val data = MutableStateFlow(firstToken)
    private val resolver: OnrampRegionRestrictionResolver = mockk()
    private val builtInputs = mutableListOf<TokenActionsBuildInput>()

    @Test
    fun `GIVEN previously supported region WHEN screen returns in restricted region THEN banner input refreshes`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolveRestrictedRegionName(userWallet) } returns null
        val model = createModel(scheduler)
        scheduler.runCurrent()
        coEvery { resolver.resolveRestrictedRegionName(userWallet) } returns "United Kingdom"

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRestrictedRegionName).isEqualTo("United Kingdom")
        coVerify(exactly = 2) { resolver.resolveRestrictedRegionName(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN initial lookup failed WHEN screen returns THEN lookup is retried`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolveRestrictedRegionName(userWallet) } returnsMany listOf(null, "United Kingdom")
        val model = createModel(scheduler)
        scheduler.runCurrent()

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRestrictedRegionName).isEqualTo("United Kingdom")
        coVerify(exactly = 2) { resolver.resolveRestrictedRegionName(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN same wallet WHEN another token is selected THEN region is checked again`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolveRestrictedRegionName(userWallet) } returnsMany listOf("United Kingdom", null)
        val model = createModel(scheduler)
        scheduler.runCurrent()
        val secondToken: CryptoCurrencyData = mockk(relaxed = true) {
            every { userWallet } returns this@TokenActionsModelTest.userWallet
        }

        // Act
        data.value = secondToken
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRestrictedRegionName).isNull()
        coVerify(exactly = 2) { resolver.resolveRestrictedRegionName(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted region WHEN screen returns in supported region THEN banner input clears`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolveRestrictedRegionName(userWallet) } returnsMany listOf("United Kingdom", null)
        val model = createModel(scheduler)
        scheduler.runCurrent()

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRestrictedRegionName).isNull()
        coVerify(exactly = 2) { resolver.resolveRestrictedRegionName(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN same token WHEN balance updates THEN IP lookup is not repeated`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolveRestrictedRegionName(userWallet) } returns "United Kingdom"
        val model = createModel(scheduler)
        scheduler.runCurrent()
        val updatedToken: CryptoCurrencyData = mockk(relaxed = true) {
            every { userWallet } returns this@TokenActionsModelTest.userWallet
            every { status.currency.id } returns firstToken.status.currency.id
        }

        // Act
        data.value = updatedToken
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRestrictedRegionName).isEqualTo("United Kingdom")
        coVerify(exactly = 1) { resolver.resolveRestrictedRegionName(userWallet) }
        model.onDestroy()
    }

    private fun createModel(scheduler: TestCoroutineScheduler): TokenActionsModel {
        val appCurrency: GetSelectedAppCurrencyUseCase = mockk {
            every { invokeOrDefault() } returns flowOf(AppCurrency.Default)
        }
        val balanceHiding: GetBalanceHidingSettingsUseCase = mockk {
            every { isBalanceHidden() } returns flowOf(false)
        }
        val handlerFactory: TokenActionsHandler.Factory = mockk {
            every { create(any(), any(), any()) } returns mockk()
        }
        val uiBuilder: TokenActionsUiBuilder = mockk {
            every { build(capture(builtInputs), any(), any()) } returns mockk()
        }
        return TokenActionsModel(
            paramsContainer = MutableParamsContainer(
                TokenActionsComponent.Params(data = data, callbacks = mockk(relaxed = true)),
            ),
            getSelectedAppCurrencyUseCase = appCurrency,
            getBalanceHidingSettingsUseCase = balanceHiding,
            tokenActionsIntentsFactory = handlerFactory,
            dispatchers = scheduler.createTestingCoroutineDispatcherProvider(),
            uiBuilder = uiBuilder,
            receiveAddressesFactory = mockk(),
            onrampRegionRestrictionResolver = resolver,
        )
    }
}