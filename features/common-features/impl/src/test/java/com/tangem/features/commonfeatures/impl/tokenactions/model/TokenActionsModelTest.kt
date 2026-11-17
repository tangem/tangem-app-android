package com.tangem.features.commonfeatures.impl.tokenactions.model

import com.arkivanov.decompose.router.slot.activate
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.markets.action.CryptoCurrencyData
import com.tangem.common.ui.markets.action.TokenActionsContext
import com.tangem.common.ui.markets.action.TokenActionsHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.features.commonfeatures.impl.tokenactions.TokenActionsComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.jupiter.api.Test

internal class TokenActionsModelTest {

    private val userWallet: UserWallet = mockk(relaxed = true)
    private val firstToken: CryptoCurrencyData = mockk(relaxed = true) {
        every { userWallet } returns this@TokenActionsModelTest.userWallet
    }
    private val data = MutableStateFlow(firstToken)
    private val resolver: OnrampRegionRestrictionResolver = mockk { every { isEnabled } returns true }
    private val restricted = OnrampRegionState.Restricted("United Kingdom")
    private val buyCheck = slot<() -> Boolean>()
    private val builtInputs = mutableListOf<TokenActionsBuildInput>()

    @Test
    fun `GIVEN previously supported region WHEN screen returns in restricted region THEN banner input refreshes`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returns OnrampRegionState.Allowed
        val model = createModel(scheduler)
        model.onStart()
        scheduler.runCurrent()
        coEvery { resolver.resolve(userWallet) } returns restricted

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(restricted)
        coVerify(exactly = 2) { resolver.resolve(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN initial lookup failed WHEN screen returns THEN lookup is retried`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returnsMany listOf(OnrampRegionState.Unknown, OnrampRegionState.Allowed)
        val model = createModel(scheduler)
        model.onStart()
        scheduler.runCurrent()

        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Unknown)

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Allowed)
        coVerify(exactly = 2) { resolver.resolve(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN same wallet WHEN another token is selected THEN region is checked again`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returnsMany listOf(restricted, OnrampRegionState.Allowed)
        val model = createModel(scheduler)
        model.onStart()
        scheduler.runCurrent()
        val secondToken: CryptoCurrencyData = mockk(relaxed = true) {
            every { userWallet } returns this@TokenActionsModelTest.userWallet
        }

        // Act
        data.value = secondToken
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Allowed)
        coVerify(exactly = 2) { resolver.resolve(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a pending wallet lookup WHEN another wallet is selected THEN only its result applies`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val firstResponse = CompletableDeferred<OnrampRegionState>()
        coEvery { resolver.resolve(userWallet) } coAnswers { firstResponse.await() }
        val nextWallet: UserWallet = mockk { every { walletId } returns mockk() }
        val nextResponse = CompletableDeferred<OnrampRegionState>()
        coEvery { resolver.resolve(nextWallet) } coAnswers { nextResponse.await() }
        val model = createModel(scheduler)
        model.onStart()
        scheduler.runCurrent()

        // Act
        data.value = mockk(relaxed = true) {
            every { userWallet } returns nextWallet
            every { status.currency.id } returns firstToken.status.currency.id
        }
        scheduler.runCurrent()
        firstResponse.complete(OnrampRegionState.Allowed)
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Checking)
        nextResponse.complete(restricted)
        scheduler.runCurrent()
        assertThat(builtInputs.last().onrampRegion).isEqualTo(restricted)
        model.onDestroy()
    }

    @Test
    fun `GIVEN restricted region WHEN screen returns in supported region THEN banner input clears`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returnsMany listOf(restricted, OnrampRegionState.Allowed)
        val model = createModel(scheduler)
        model.onStart()
        scheduler.runCurrent()

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Allowed)
        coVerify(exactly = 2) { resolver.resolve(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN same token WHEN balance updates THEN IP lookup is not repeated`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returns restricted
        val model = createModel(scheduler)
        model.onStart()
        scheduler.runCurrent()
        val updatedToken: CryptoCurrencyData = mockk(relaxed = true) {
            every { userWallet } returns this@TokenActionsModelTest.userWallet
            every { status.currency.id } returns firstToken.status.currency.id
        }

        // Act
        data.value = updatedToken
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(restricted)
        coVerify(exactly = 1) { resolver.resolve(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN new sheet WHEN first started THEN IP is looked up once`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returns restricted
        val model = createModel(scheduler)

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(restricted)
        coVerify(exactly = 1) { resolver.resolve(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN transfer context WHEN data emitted THEN no IP lookup and no banner input`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returns restricted

        // Act
        val model = createModel(scheduler, context = TokenActionsContext.Transfer)
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Allowed)
        coVerify(exactly = 0) { resolver.resolve(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN cold token data WHEN model built THEN upstream collected once`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returns OnrampRegionState.Allowed
        var collections = 0
        val coldData = flow {
            collections++
            emit(firstToken)
        }

        // Act
        val model = createModel(scheduler, tokenData = coldData)
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(collections).isEqualTo(1)
        assertThat(builtInputs).isNotEmpty()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a pending first lookup WHEN the sheet opens THEN Buy waits for the result`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        val result = CompletableDeferred<OnrampRegionState>()
        coEvery { resolver.resolve(userWallet) } coAnswers { result.await() }
        val model = createModel(scheduler)

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Checking)
        result.complete(restricted)
        scheduler.runCurrent()
        assertThat(builtInputs.last().onrampRegion).isEqualTo(restricted)
        model.onDestroy()
    }

    @Test
    fun `GIVEN Receive over an allowed sheet WHEN dismissed THEN recheck before allowing Buy`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        coEvery { resolver.resolve(userWallet) } returns OnrampRegionState.Allowed
        val model = createModel(scheduler)
        var receiveSlot: TokenReceiveConfig? = null
        model.bottomSheetNavigation.subscribe { event ->
            val previous = receiveSlot
            receiveSlot = event.transformer(previous)
            event.onComplete(receiveSlot, previous)
        }
        model.onStart()
        scheduler.runCurrent()
        val previousBuyCheck = buyCheck.captured
        assertThat(previousBuyCheck()).isTrue()
        model.bottomSheetNavigation.activate(mockk<TokenReceiveConfig>())
        val result = CompletableDeferred<OnrampRegionState>()
        coEvery { resolver.resolve(userWallet) } coAnswers { result.await() }

        // Act
        model.dismissReceive()
        assertThat(previousBuyCheck()).isFalse()
        scheduler.runCurrent()

        // Assert
        assertThat(receiveSlot).isNull()
        assertThat(builtInputs.last().onrampRegion).isEqualTo(OnrampRegionState.Checking)
        result.complete(restricted)
        scheduler.runCurrent()
        assertThat(builtInputs.last().onrampRegion).isEqualTo(restricted)
        coVerify(exactly = 2) { resolver.resolve(userWallet) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle disabled WHEN Add funds opens THEN Buy is available without an IP lookup`() {
        // Arrange
        val scheduler = TestCoroutineScheduler()
        every { resolver.isEnabled } returns false
        val model = createModel(scheduler)

        // Act
        model.onStart()
        scheduler.runCurrent()

        // Assert
        assertThat(builtInputs.map { it.onrampRegion }).containsExactly(OnrampRegionState.Allowed)
        coVerify(exactly = 0) { resolver.resolve(any()) }
        model.onDestroy()
    }

    private fun createModel(
        scheduler: TestCoroutineScheduler,
        context: TokenActionsContext = TokenActionsContext.AddFunds,
        tokenData: Flow<CryptoCurrencyData> = data,
    ): TokenActionsModel {
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
            every { build(capture(builtInputs), any(), any(), capture(buyCheck)) } returns mockk()
        }
        return TokenActionsModel(
            paramsContainer = MutableParamsContainer(
                TokenActionsComponent.Params(data = tokenData, callbacks = mockk(relaxed = true), context = context),
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