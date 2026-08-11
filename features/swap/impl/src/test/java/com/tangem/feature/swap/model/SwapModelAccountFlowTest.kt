package com.tangem.feature.swap.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.entity.AccountFlow
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.models.ChangeCardsButtonState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
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
        // initTokens() always reaches updateFeePaidCryptoCurrencyFor(...) for a non-null FROM slot (unless
        // the FROM account is Account.Payment); a bare relaxed mock return for this use case fails an
        // internal Either cast, which silently cancels the initTokens() coroutine under its SupervisorJob
        // and leaves uiState at its Empty-card initial value. Stub it so tests that populate real FROM/TO
        // statuses actually observe the resolved card content.
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns Either.Right(null)
        // With a non-null FROM/TO pair, initTokens() also kicks off initSwapPairs() → swapInteractor.getPair(...)
        // under Unconfined dispatchers. A bare relaxed-mock Either return fails a downstream cast; stub a
        // real Left so that branch resolves via createInitialErrorState (which does not touch receiveCardData/
        // sendCardData's fiatSymbolOverride/isSelectionLocked/tokenSymbol, so it doesn't affect these tests).
        coEvery {
            swapInteractor.getPair(fromSwapCurrencyStatus = any(), toSwapCurrencyStatus = any(), filterProviderTypes = any())
        } returns Either.Left(ExpressError.UnknownError)
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
    // [REDACTED_TASK_KEY] Task 6: abstract "USD" TO card + account-flow screen titles
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN TopUp flow and toggle ON WHEN model constructed THEN receive card is USD locked and title is Add funds`() =
        runTest {
            // Arrange
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val wallet = accountFlowUserWallet()
            val fromStatus = buildSwapCurrencyStatus(wallet)
            val toStatus = buildSwapCurrencyStatus(wallet)
            coEvery {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
                    applyAccountTopUpFromPriority = any(),
                )
            } returns (fromStatus to toStatus)

            // Act
            val model = createModel(accountFlow = AccountFlow.TopUp)

            // Assert
            val receiveCard = model.uiState.receiveCardData as SwapCardState.SwapCardData
            assertThat(receiveCard.fiatSymbolOverride).isEqualTo("USD")
            assertThat(receiveCard.isSelectionLocked).isTrue()
            assertThat(model.uiState.titleId).isEqualTo(R.string.tangempay_card_details_add_funds)
        }

    @Test
    fun `GIVEN TopUp flow and toggle ON WHEN model constructed THEN send card is not locked`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val wallet = accountFlowUserWallet()
        val fromStatus = buildSwapCurrencyStatus(wallet)
        val toStatus = buildSwapCurrencyStatus(wallet)
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
                applyAccountTopUpFromPriority = any(),
            )
        } returns (fromStatus to toStatus)

        // Act
        val model = createModel(accountFlow = AccountFlow.TopUp)

        // Assert — only the TO (receive) card is abstracted; FROM keeps its real token.
        val sendCard = model.uiState.sendCardData as SwapCardState.SwapCardData
        assertThat(sendCard.fiatSymbolOverride).isNull()
        assertThat(sendCard.isSelectionLocked).isFalse()
    }

    @Test
    fun `GIVEN Withdraw flow and toggle ON WHEN model constructed THEN title is Withdraw and receive card unaffected`() =
        runTest {
            // Arrange
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val wallet = accountFlowUserWallet()
            val fromStatus = buildSwapCurrencyStatus(wallet)
            val toStatus = buildSwapCurrencyStatus(wallet)
            coEvery {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
                    applyAccountTopUpFromPriority = any(),
                )
            } returns (fromStatus to toStatus)

            // Act
            val model = createModel(accountFlow = AccountFlow.Withdraw)

            // Assert — Withdraw only changes the title; TO stays a normal, selectable card.
            assertThat(model.uiState.titleId).isEqualTo(R.string.tangempay_card_details_withdraw)
            val receiveCard = model.uiState.receiveCardData as SwapCardState.SwapCardData
            assertThat(receiveCard.fiatSymbolOverride).isNull()
            assertThat(receiveCard.isSelectionLocked).isFalse()
        }

    @Test
    fun `GIVEN no account flow WHEN model constructed THEN title stays default Swap`() = runTest {
        // Arrange & Act
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = null)

        // Assert
        assertThat(model.uiState.titleId).isEqualTo(R.string.common_swap)
    }

    @Test
    fun `GIVEN toggle OFF WHEN TopUp flow THEN title and receive card differ from the toggle-ON equivalent`() = runTest {
        // Arrange — drive the SAME stubbed FROM/TO pair through a toggle-ON run first, capturing its (already
        // separately verified) transformed title/card as a baseline. Asserting toggle-OFF differs from that
        // baseline — rather than only asserting it equals the fields' own defaults — proves the toggle
        // actually suppresses a real transform, not just that null/false happen to be the untouched defaults.
        val wallet = accountFlowUserWallet()
        val fromStatus = buildSwapCurrencyStatus(wallet)
        val toStatus = buildSwapCurrencyStatus(wallet)
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
                applyAccountTopUpFromPriority = any(),
            )
        } returns (fromStatus to toStatus)

        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val toggleOnModel = createModel(accountFlow = AccountFlow.TopUp)
        val toggleOnReceiveCard = toggleOnModel.uiState.receiveCardData as SwapCardState.SwapCardData
        assertThat(toggleOnReceiveCard.fiatSymbolOverride).isEqualTo("USD")
        assertThat(toggleOnModel.uiState.titleId).isEqualTo(R.string.tangempay_card_details_add_funds)

        // Act
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val toggleOffModel = createModel(accountFlow = AccountFlow.TopUp)

        // Assert — toggle OFF genuinely diverges from the toggle-ON baseline above.
        assertThat(toggleOffModel.uiState.titleId).isNotEqualTo(toggleOnModel.uiState.titleId)
        assertThat(toggleOffModel.uiState.titleId).isEqualTo(R.string.common_swap)
        val toggleOffReceiveCard = toggleOffModel.uiState.receiveCardData as SwapCardState.SwapCardData
        assertThat(toggleOffReceiveCard.fiatSymbolOverride).isNotEqualTo(toggleOnReceiveCard.fiatSymbolOverride)
        assertThat(toggleOffReceiveCard.fiatSymbolOverride).isNull()
        assertThat(toggleOffReceiveCard.isSelectionLocked).isFalse()
    }

    @Test
    fun `GIVEN TopUp flow and toggle ON WHEN model constructed THEN CTA mode matches an equivalent non-account swap run`() =
        runTest {
            // Arrange — hard constraint: abstracting the TO card must NOT repurpose the main button. Rather
            // than asserting the mode is "in" the full set of Mode entries (which is tautological — no value
            // of that type could ever fail it), drive the SAME stubbed FROM/TO pair through a plain,
            // non-account swap run and an account-flow (TopUp) run and assert they land on the IDENTICAL
            // CTA mode. This directly proves the account-flow presentation layer does not alter the CTA — a
            // future change that hardcodes/repurposes the CTA under an account flow would make the two
            // diverge and fail this test.
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val wallet = accountFlowUserWallet()
            val fromStatus = buildSwapCurrencyStatus(wallet)
            val toStatus = buildSwapCurrencyStatus(wallet)
            coEvery {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
                    applyAccountTopUpFromPriority = any(),
                )
            } returns (fromStatus to toStatus)

            // Act
            val plainSwapModel = createModel(accountFlow = null)
            val topUpModel = createModel(accountFlow = AccountFlow.TopUp)

            // Assert — same CTA mode regardless of account flow, and it is a real (non-"Send") mode.
            assertThat(topUpModel.uiState.swapButton.mode).isEqualTo(plainSwapModel.uiState.swapButton.mode)
            assertThat(topUpModel.uiState.swapButton.mode).isEqualTo(SwapButton.Mode.SWAP)
        }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * A real (non-relaxed-mock-for-`value`) [SwapCurrencyStatus] source wallet, built via the shared
     * [buildSwapCurrencyStatus] helper so the resulting card actually reaches [SwapCardState.SwapCardData]
     * (a fully relaxed [CryptoCurrencyStatus] mock's sealed `value` fails the state builder's `when`).
     */
    private fun accountFlowUserWallet(): UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns userWalletId
    }

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