package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.entity.AccountFlow
import com.tangem.domain.models.account.Account
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.models.ChangeCardsButtonState
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [REDACTED_TASK_KEY] Task 5: mode-based withdrawal detection driven by [AccountFlow] (instead of
 * inspecting the FROM slot's [Account] type) plus the hidden/no-op reverse (swap-direction) button
 * whenever the swap screen is opened in an account flow (Tangem Pay top-up/withdraw via swap).
 *
 * Behaviour gated by [com.tangem.features.swap.SwapFeatureToggles.isAccountSwapFlowEnabled]:
 *  - toggle ON:  [SwapModel.isTangemPayWithdrawal] is driven purely by `accountFlow is Withdraw`,
 *    regardless of what is in the FROM slot; the reverse button is forced [ChangeCardsButtonState.HIDDEN]
 *    whenever an [AccountFlow] is present, and [SwapModel] exposes a callback for it
 *    (`uiState.onChangeCardsClicked`) that is a no-op.
 *  - toggle OFF: legacy behaviour — [SwapModel.isTangemPayWithdrawal] inspects the FROM slot's
 *    [Account] type, and the reverse button is never forced hidden.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelAccountFlowTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
    }

    @Test
    fun `GIVEN Withdraw flow and toggle ON WHEN isTangemPayWithdrawal THEN true regardless of FROM slot`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk(relaxed = true))

        // Act
        val result = model.isTangemPayWithdrawal(fromWithPortfolioAccount)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN TopUp flow and toggle ON WHEN isTangemPayWithdrawal THEN false`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.TopUp)
        val fromWithPaymentAccount = swapCurrencyStatus(account = Account.Payment(userWalletId))

        // Act
        val result = model.isTangemPayWithdrawal(fromWithPaymentAccount)

        // Assert — mode-based detection ignores the (legacy) Payment-account FROM slot for TopUp.
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN account flow and toggle ON WHEN state emitted THEN reverse button HIDDEN`() = runTest {
        // Arrange & Act
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.TopUp)

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN account flow and toggle ON WHEN model constructed THEN the very first state is already HIDDEN`() =
        runTest {
            // Arrange — `initTokens()` (and every other `modelScope.launch { ... }` in `init {}`) is
            // dispatched on `main`/`mainImmediate`/`default`; the base's default dispatchers are all
            // `Dispatchers.Unconfined`, which would run those bodies synchronously during construction
            // and, via the setter, independently re-force HIDDEN — masking a bug in the `_uiState`
            // field initializer itself. Using `StandardTestDispatcher` for every role instead defers
            // all of that until the scheduler is advanced, so the assertion below observes the
            // initializer's value directly.
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val deferred = StandardTestDispatcher(testScheduler)

            // Act
            val model = createModel(
                accountFlow = AccountFlow.Withdraw,
                dispatchers = TestingCoroutineDispatcherProvider(
                    main = deferred,
                    mainImmediate = deferred,
                    io = deferred,
                    default = deferred,
                ),
            )

            // Assert — nothing async has run yet: the resolver `initTokens()` calls hasn't been invoked,
            // proving this is the pre-async, initializer-produced state, not a later setter-driven write.
            coVerify(exactly = 0) {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
                    applyAccountTopUpFromPriority = any(),
                )
            }
            assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)

            // Sanity: once the deferred work runs, the setter keeps enforcing the same rule.
            advanceUntilIdle()
            assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
        }

    @Test
    fun `GIVEN no account flow and toggle ON WHEN state emitted THEN reverse button not forced hidden`() = runTest {
        // Arrange & Act — standalone swap screen (no Tangem Pay account flow) must not be affected.
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = null)

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isNotEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN account flow and toggle ON WHEN onChangeCardsClicked invoked THEN no-op`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        val fromStatus = swapCurrencyStatus()
        val toStatus = swapCurrencyStatus()
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = toStatus,
        )
        val dataStateBeforeClick = model.dataState

        // Act
        model.uiState.onChangeCardsClicked()

        // Assert — the reverse (swap-direction) action never ran: dataState is untouched.
        assertThat(model.dataState).isSameInstanceAs(dataStateBeforeClick)
    }

    @Test
    fun `GIVEN toggle OFF WHEN Withdraw flow THEN legacy slot detection used`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        val fromWithPaymentAccount = swapCurrencyStatus(account = Account.Payment(userWalletId))
        val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk(relaxed = true))

        // Act & Assert
        assertThat(model.isTangemPayWithdrawal(fromWithPaymentAccount)).isTrue()
        assertThat(model.isTangemPayWithdrawal(fromWithPortfolioAccount)).isFalse()
    }

    @Test
    fun `GIVEN toggle OFF WHEN account flow present THEN reverse button not forced hidden`() = runTest {
        // Arrange & Act — legacy behaviour must be untouched by the account-flow field even if it is set.
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.Withdraw)

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isNotEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN Withdraw flow and toggle ON WHEN filterTangemPayProviders THEN only CEX providers kept`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        val cexProvider = swapProvider(id = "cex-1", type = ExchangeProviderType.CEX)
        val dexProvider = swapProvider(id = "dex-1", type = ExchangeProviderType.DEX)
        val pair = SwapPairLeast(
            from = LeastTokenInfo(contractAddress = "0xfrom", network = "ethereum"),
            to = LeastTokenInfo(contractAddress = "0xto", network = "ethereum"),
            providers = listOf(cexProvider, dexProvider),
        )

        // Act
        val result = invokeFilterTangemPayProviders(
            model = model,
            pairs = listOf(pair),
            fromSwapCurrencyStatus = swapCurrencyStatus(),
            toSwapCurrencyStatus = swapCurrencyStatus(),
        )

        // Assert
        assertThat(result).hasSize(1)
        assertThat(result.single().providers).containsExactly(cexProvider)
    }

    @Test
    fun `GIVEN TopUp flow and toggle ON WHEN filterTangemPayProviders THEN providers unfiltered`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.TopUp)
        val cexProvider = swapProvider(id = "cex-1", type = ExchangeProviderType.CEX)
        val dexProvider = swapProvider(id = "dex-1", type = ExchangeProviderType.DEX)
        val pair = SwapPairLeast(
            from = LeastTokenInfo(contractAddress = "0xfrom", network = "ethereum"),
            to = LeastTokenInfo(contractAddress = "0xto", network = "ethereum"),
            providers = listOf(cexProvider, dexProvider),
        )

        // Act
        val result = invokeFilterTangemPayProviders(
            model = model,
            pairs = listOf(pair),
            fromSwapCurrencyStatus = swapCurrencyStatus(),
            toSwapCurrencyStatus = swapCurrencyStatus(),
        )

        // Assert — TopUp is not a withdrawal, so no CEX-only filter applies.
        assertThat(result.single().providers).containsExactly(cexProvider, dexProvider)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * [SwapModel.filterTangemPayProviders] is a private member extension function on
     * `List<SwapPairLeast>`; it is exercised via reflection here (same technique used in
     * [SwapModelCombineFeesTest] for other pure private functions) rather than through the full
     * `initSwapPairs` pipeline, which requires a large amount of unrelated async wiring.
     */
    @Suppress("UNCHECKED_CAST")
    private fun invokeFilterTangemPayProviders(
        model: SwapModel,
        pairs: List<SwapPairLeast>,
        fromSwapCurrencyStatus: com.tangem.domain.swap.models.SwapCurrencyStatus,
        toSwapCurrencyStatus: com.tangem.domain.swap.models.SwapCurrencyStatus,
    ): List<SwapPairLeast> {
        val method = SwapModel::class.java.declaredMethods.first { it.name == "filterTangemPayProviders" }
            .apply { isAccessible = true }
        return method.invoke(model, pairs, fromSwapCurrencyStatus, toSwapCurrencyStatus) as List<SwapPairLeast>
    }
}