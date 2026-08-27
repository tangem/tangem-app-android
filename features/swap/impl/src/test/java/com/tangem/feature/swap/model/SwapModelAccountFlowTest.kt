package com.tangem.feature.swap.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.models.ChangeCardsButtonState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.presentation.R
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooserBlock
import com.tangem.test.mock.MockAccounts
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
 * Tests for mode-based withdrawal detection driven by [AccountFlow] (instead of
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
        val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk<Account.CryptoPortfolio>(relaxed = true))

        // Act
        val result = model.isTangemPayWithdrawal(fromWithPortfolioAccount)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN TopUp flow and toggle ON WHEN FROM is a normal wallet token THEN isTangemPayWithdrawal false`() =
        runTest {
            // Arrange — in a real TopUp flow, FROM is resolved to a wallet token from a crypto-portfolio
            // account (the Payment account is the TO side); it is never Account.Payment. See
            // InitialCurrenciesResolver.resolveAccountTopUpFromPriority, which only ever draws FROM from
            // cryptoPortfolioAccounts/cryptoCurrencyList.
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val model = createModel(accountFlow = AccountFlow.TopUp)
            val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk<Account.CryptoPortfolio>(relaxed = true))

            // Act
            val result = model.isTangemPayWithdrawal(fromWithPortfolioAccount)

            // Assert
            assertThat(result).isFalse()
        }

    @Test
    fun `GIVEN no account flow and toggle ON WHEN FROM is a Payment account THEN isTangemPayWithdrawal true`() =
        runTest {
            // Arrange — even with the account-swap-flow toggle ON, a regular (non-Tangem-Pay)
            // swap entry (accountFlow == null) still lets the user manually pick the Payment account as FROM
            // (Settings.SwapFrom keeps isShowPaymentAccount = true). The legacy slot-based check must still
            // catch that case so it is routed through withdrawal handling (CEX-only providers), matching
            // legacy (toggle-OFF) behaviour.
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val model = createModel(accountFlow = null)
            val fromWithPaymentAccount = swapCurrencyStatus(account = Account.Payment(userWalletId))

            // Act
            val result = model.isTangemPayWithdrawal(fromWithPaymentAccount)

            // Assert
            assertThat(result).isTrue()
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
                    isAccountFlowEnabled = any(),
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

    // -------------------------------------------------------------------------
    // Restrict withdraw FROM selector to the Payment account + hide Markets
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN Withdraw flow WHEN from selector settings THEN Markets hidden`() = runTest {
        // Arrange & Act
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseFromTokenBridge.settings.chooserBlock).isInstanceOf(ChooserBlock.None::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN Withdraw flow WHEN from tokenFilter applied THEN only payment account tokens pass`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()
        val paymentAccountStatus = MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId)
        val usdcPolygonStatus: CryptoCurrencyStatus = mockk(relaxed = true)
        val cryptoPortfolioStatus: AccountStatus.CryptoPortfolio = mockk(relaxed = true)
        val btcStatus: CryptoCurrencyStatus = mockk(relaxed = true)

        // Act
        val predicate = model.chooseFromTokenBridge.tokenFilter.value

        // Assert
        assertThat(predicate(paymentAccountStatus, usdcPolygonStatus)).isTrue()
        assertThat(predicate(cryptoPortfolioStatus, btcStatus)).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN Withdraw flow WHEN FROM is already resolved to the payment token THEN tokenFilter still includes it`() =
        runTest {
            // Arrange — reproduces the REAL withdraw condition, where initTokens() has already resolved FROM
            // to the account's payment token before filterTokensFromSelector() runs, so
            // dataState.fromSwapCurrencyStatus is non-null and points at that very token. baseFilter
            // (the regular/non-withdraw predicate) excludes the already-picked FROM token — applying that
            // exclusion here would filter out the only row the Payment-only selector has, rendering it
            // empty. The predicate above (which leaves fromSwapCurrencyStatus null) does not catch this.
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val model = createModel(accountFlow = AccountFlow.Withdraw)
            advanceUntilIdle()
            val paymentAccountStatus = MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId)
            val paymentAccount = paymentAccountStatus.account
            val usdcPolygonStatus: CryptoCurrencyStatus = mockk(relaxed = true)
            val resolvedFrom = swapCurrencyStatus(account = paymentAccount, currency = usdcPolygonStatus.currency)
            model.dataState = model.dataState.copy(fromSwapCurrencyStatus = resolvedFrom)

            // Act
            val predicate = model.chooseFromTokenBridge.tokenFilter.value

            // Assert — the payment token still appears despite being the currently-selected FROM.
            assertThat(predicate(paymentAccountStatus, usdcPolygonStatus)).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN Withdraw flow WHEN to selector settings THEN Markets stays shown`() = runTest {
        // Arrange & Act — the TO bridge is unaffected by the withdraw-FROM restriction.
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseToTokenBridge.settings.chooserBlock).isInstanceOf(ChooserBlock.Market::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN Withdraw flow WHEN selector settings THEN payment account lists every issued token`() = runTest {
        // Arrange & Act
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseToTokenBridge.settings.isPaymentAccountMultiTokenEnabled).isTrue()
        assertThat(model.chooseFromTokenBridge.settings.isPaymentAccountMultiTokenEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN TopUp flow WHEN from selector settings THEN Markets stays shown`() = runTest {
        // Arrange & Act — only Withdraw restricts the FROM selector; TopUp keeps legacy SwapFrom settings.
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val model = createModel(accountFlow = AccountFlow.TopUp)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseFromTokenBridge.settings.chooserBlock).isInstanceOf(ChooserBlock.Market::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle OFF WHEN Withdraw flow THEN from selector settings stay legacy`() = runTest {
        // Arrange & Act — the toggle gates the withdraw-FROM restriction just like it gates everything else.
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseFromTokenBridge.settings.chooserBlock).isInstanceOf(ChooserBlock.Market::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle OFF WHEN Withdraw flow THEN from tokenFilter is not restricted to payment account`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()
        val cryptoPortfolioStatus: AccountStatus.CryptoPortfolio = mockk(relaxed = true)
        val btcStatus: CryptoCurrencyStatus = mockk(relaxed = true)

        // Act
        val predicate = model.chooseFromTokenBridge.tokenFilter.value

        // Assert — a regular (non-Payment) account's token still passes when the toggle is off.
        assertThat(predicate(cryptoPortfolioStatus, btcStatus)).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle OFF WHEN Withdraw flow THEN from tokenFilter is byte-for-byte the to tokenFilter`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()
        val cryptoPortfolioStatus: AccountStatus.CryptoPortfolio = mockk(relaxed = true)
        val someStatus: CryptoCurrencyStatus = mockk(relaxed = true)

        // Act
        val fromPredicate = model.chooseFromTokenBridge.tokenFilter.value
        val toPredicate = model.chooseToTokenBridge.tokenFilter.value

        // Assert — same base predicate on both sides when not in a Withdraw account flow.
        assertThat(fromPredicate(cryptoPortfolioStatus, someStatus)).isEqualTo(
            toPredicate(cryptoPortfolioStatus, someStatus),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle OFF WHEN Withdraw flow THEN legacy slot detection used`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        val fromWithPaymentAccount = swapCurrencyStatus(account = Account.Payment(userWalletId))
        val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk<Account.CryptoPortfolio>(relaxed = true))

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
    // Abstract "USD" TO card + account-flow screen titles
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
                    isAccountFlowEnabled = any(),
                )
            } returns (fromStatus to toStatus)

            // Act
            val model = createModel(accountFlow = AccountFlow.TopUp)

            // Assert
            val receiveCard = model.uiState.receiveCardData as SwapCardState.SwapCardData
            assertThat(receiveCard.fiatSymbolOverride).isEqualTo("USD")
            assertThat(receiveCard.isSelectionLocked).isTrue()
            assertThat(receiveCard.currencyIconState).isInstanceOf(CurrencyIconState.PaymentAccount::class.java)
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
                isAccountFlowEnabled = any(),
            )
        } returns (fromStatus to toStatus)

        // Act
        val model = createModel(accountFlow = AccountFlow.TopUp)

        // Assert — only the TO (receive) card is abstracted; FROM keeps its real token icon, not the
        // Payment-account avatar.
        val sendCard = model.uiState.sendCardData as SwapCardState.SwapCardData
        assertThat(sendCard.fiatSymbolOverride).isNull()
        assertThat(sendCard.isSelectionLocked).isFalse()
        assertThat(sendCard.currencyIconState).isNotInstanceOf(CurrencyIconState.PaymentAccount::class.java)
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
                    isAccountFlowEnabled = any(),
                )
            } returns (fromStatus to toStatus)

            // Act
            val model = createModel(accountFlow = AccountFlow.Withdraw)

            // Assert — Withdraw only changes the title; TO stays a normal, selectable card with its real
            // token icon, not the Payment-account avatar.
            assertThat(model.uiState.titleId).isEqualTo(R.string.tangempay_card_details_withdraw)
            val receiveCard = model.uiState.receiveCardData as SwapCardState.SwapCardData
            assertThat(receiveCard.fiatSymbolOverride).isNull()
            assertThat(receiveCard.isSelectionLocked).isFalse()
            assertThat(receiveCard.currencyIconState).isNotInstanceOf(CurrencyIconState.PaymentAccount::class.java)
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
                isAccountFlowEnabled = any(),
            )
        } returns (fromStatus to toStatus)

        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val toggleOnModel = createModel(accountFlow = AccountFlow.TopUp)
        val toggleOnReceiveCard = toggleOnModel.uiState.receiveCardData as SwapCardState.SwapCardData
        assertThat(toggleOnReceiveCard.fiatSymbolOverride).isEqualTo("USD")
        assertThat(toggleOnReceiveCard.currencyIconState).isInstanceOf(CurrencyIconState.PaymentAccount::class.java)
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
        assertThat(toggleOffReceiveCard.currencyIconState).isNotInstanceOf(CurrencyIconState.PaymentAccount::class.java)
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
                    isAccountFlowEnabled = any(),
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
    // Toggle-OFF regression — legacy behaviour must be untouched by AccountFlow
    // regardless of which flow value is set, not just Withdraw (already covered above).
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN toggle OFF WHEN TopUp flow THEN legacy slot detection used for isTangemPayWithdrawal`() = runTest {
        // Arrange
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.TopUp)
        val fromWithPaymentAccount = swapCurrencyStatus(account = Account.Payment(userWalletId))
        val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk<Account.CryptoPortfolio>(relaxed = true))

        // Act & Assert — the TopUp AccountFlow value is ignored; only the FROM slot's Account type matters.
        assertThat(model.isTangemPayWithdrawal(fromWithPaymentAccount)).isTrue()
        assertThat(model.isTangemPayWithdrawal(fromWithPortfolioAccount)).isFalse()
    }

    @Test
    fun `GIVEN toggle OFF WHEN TopUp flow THEN reverse button not forced hidden`() = runTest {
        // Arrange & Act
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.TopUp)

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isNotEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN toggle OFF WHEN TopUp flow THEN from and to selector settings are the legacy SwapFrom and SwapTo`() =
        runTest {
            // Arrange & Act — whole-object equality: SwapFrom/WithdrawFrom differ only in chooserBlock, so this
            // also proves the withdraw-only Market-hiding restriction does not leak into TopUp.
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
            val model = createModel(accountFlow = AccountFlow.TopUp)
            advanceUntilIdle()

            // Assert
            assertThat(model.chooseFromTokenBridge.settings).isEqualTo(ChooseTokenBridge.Settings.SwapFrom)
            assertThat(model.chooseToTokenBridge.settings).isEqualTo(ChooseTokenBridge.Settings.SwapTo)
            model.onDestroy()
        }

    @Test
    fun `GIVEN toggle OFF WHEN Withdraw flow THEN from and to selector settings are the legacy SwapFrom and SwapTo`() =
        runTest {
            // Arrange & Act — complements the existing chooserBlock-only checks with a whole-object assertion
            // and covers the TO side, which toggle-OFF Withdraw had no coverage for yet.
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
            val model = createModel(accountFlow = AccountFlow.Withdraw)
            advanceUntilIdle()

            // Assert
            assertThat(model.chooseFromTokenBridge.settings).isEqualTo(ChooseTokenBridge.Settings.SwapFrom)
            assertThat(model.chooseToTokenBridge.settings).isEqualTo(ChooseTokenBridge.Settings.SwapTo)
            model.onDestroy()
        }

    @Test
    fun `GIVEN toggle OFF WHEN Withdraw flow THEN title stays default Swap`() = runTest {
        // Arrange & Act — Withdraw+toggle-ON title override (asserted above) must not leak when the toggle is off.
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns false
        val model = createModel(accountFlow = AccountFlow.Withdraw)

        // Assert
        assertThat(model.uiState.titleId).isEqualTo(R.string.common_swap)
    }

    // -------------------------------------------------------------------------
    // Non-account swap entries unchanged (accountFlow = null, toggle ON) — completes the
    // existing title/reverse coverage with the selector-settings assertion.
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN no account flow and toggle ON WHEN model constructed THEN from and to selector settings are SwapFrom and SwapTo`() =
        runTest {
            // Arrange & Act
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val model = createModel(accountFlow = null)
            advanceUntilIdle()

            // Assert — a regular (non-Tangem-Pay) swap entry is unaffected by the account-swap-flow toggle.
            assertThat(model.chooseFromTokenBridge.settings).isEqualTo(ChooseTokenBridge.Settings.SwapFrom)
            assertThat(model.chooseToTokenBridge.settings).isEqualTo(ChooseTokenBridge.Settings.SwapTo)
            model.onDestroy()
        }

    // -------------------------------------------------------------------------
    // Transfer <-> swap branch on the FROM token, locked against the mode refactor.
    // `shouldTransferInsteadOfSwap`/`updateTransfer` are stubbed directly (same technique as the rest of this
    // file) rather than exercised through the real SwapTransferInteractorImpl (covered by its own domain
    // tests) — the goal here is only to prove SwapModel reacts correctly to whichever branch the interactor
    // picks: a real account top-up has FROM = the account's own USDC-Polygon token exactly when the transfer
    // branch is taken (TO is the abstract-USD card over that same underlying token).
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN TopUp flow WHEN from is transfer-eligible (account's own token) THEN transfer path taken and no provider requested`() =
        runTest {
            // Arrange
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val fromStatus = swapCurrencyStatus()
            val toStatus = swapCurrencyStatus()
            coEvery {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
                    isAccountFlowEnabled = any(),
                )
            } returns (fromStatus to toStatus)
            every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns true
            val transferState: SwapState.Transfer = mockk(relaxed = true)
            coEvery {
                swapTransferInteractor.updateTransfer(
                    fromSwapCurrencyStatus = any(),
                    toSwapCurrencyStatus = any(),
                    fromTokenAmount = any(),
                    feePaidCurrencyStatus = any(),
                    fee = any(),
                )
            } returns transferState

            // Act
            val model = createModel(accountFlow = AccountFlow.TopUp)
            advanceUntilIdle()

            // Assert — transfer path taken: the transfer state lands in dataState and no provider list was
            // ever requested from the express backend.
            assertThat(model.dataState.currentTransferState).isSameInstanceAs(transferState)
            coVerify(exactly = 0) {
                swapInteractor.getPair(
                    fromSwapCurrencyStatus = any(),
                    toSwapCurrencyStatus = any(),
                    filterProviderTypes = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN TopUp flow WHEN from is not transfer-eligible (a different token) THEN swap path taken and provider list requested`() =
        runTest {
            // Arrange
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val fromStatus = swapCurrencyStatus()
            val toStatus = swapCurrencyStatus()
            coEvery {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
                    isAccountFlowEnabled = any(),
                )
            } returns (fromStatus to toStatus)
            every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns false

            // Act
            val model = createModel(accountFlow = AccountFlow.TopUp)
            advanceUntilIdle()

            // Assert — swap path taken: the provider list was requested for the exact FROM/TO pair, and no
            // transfer state was ever recorded.
            coVerify(exactly = 1) {
                swapInteractor.getPair(
                    fromSwapCurrencyStatus = fromStatus,
                    toSwapCurrencyStatus = toStatus,
                    filterProviderTypes = any(),
                )
            }
            assertThat(model.dataState.currentTransferState).isNull()
            model.onDestroy()
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

    // Multi-token payment account: the account owns one currency per issued network once the multichain
    // data is available, and collapses back to a single one when it is not.

    @Test
    fun `GIVEN withdraw and a multi token account WHEN from filter applied THEN every account currency passes`() =
        runTest {
            // Arrange
            every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
            val polygonCurrency = accountCurrencyStatus(polygonCurrencyId, polygonNetworkId)
            val tronCurrency = accountCurrencyStatus(tronCurrencyId, tronNetworkId)
            val model = createModel(accountFlow = AccountFlow.Withdraw)
            advanceUntilIdle()
            val paymentAccountStatus = MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId)

            // Act
            val predicate = model.chooseFromTokenBridge.tokenFilter.value

            // Assert — the withdrawal endpoints take the source network, so every issued network can be drawn from.
            assertThat(predicate(paymentAccountStatus, polygonCurrency)).isTrue()
            assertThat(predicate(paymentAccountStatus, tronCurrency)).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN top up and source matches an account currency WHEN model constructed THEN that currency becomes TO`() =
        runTest {
            // Arrange
            val tronAccountCurrency = accountCurrencyStatus(tronCurrencyId, tronNetworkId)
            val model = createTopUpModelWithAccountCurrencies(
                sourceStatus = accountCurrencyStatus(tronCurrencyId, tronNetworkId),
                accountCurrencies = listOf(
                    accountCurrencyStatus(polygonCurrencyId, polygonNetworkId),
                    tronAccountCurrency,
                ),
            )
            advanceUntilIdle()

            // Assert — same currency on both sides, so the operation becomes a plain transfer.
            assertThat(model.dataState.toSwapCurrencyStatus?.status).isSameInstanceAs(tronAccountCurrency)
            model.onDestroy()
        }

    @Test
    fun `GIVEN top up and source shares a network with an account currency WHEN model constructed THEN TO stays in that network`() =
        runTest {
            // Arrange — the wallet's USDT on Tron is not held by the account, but the account has Tron.
            val tronAccountCurrency = accountCurrencyStatus(tronCurrencyId, tronNetworkId)
            val model = createTopUpModelWithAccountCurrencies(
                sourceStatus = accountCurrencyStatus(mockk(relaxed = true), tronNetworkId),
                accountCurrencies = listOf(
                    accountCurrencyStatus(polygonCurrencyId, polygonNetworkId),
                    tronAccountCurrency,
                ),
            )
            advanceUntilIdle()

            // Assert — the swap stays inside the source's network instead of crossing to the default one.
            assertThat(model.dataState.toSwapCurrencyStatus?.status).isSameInstanceAs(tronAccountCurrency)
            model.onDestroy()
        }

    @Test
    fun `GIVEN top up and source network is not held by the account WHEN model constructed THEN TO keeps the default currency`() =
        runTest {
            // Arrange
            val defaultCurrency = accountCurrencyStatus(polygonCurrencyId, polygonNetworkId)
            val model = createTopUpModelWithAccountCurrencies(
                sourceStatus = accountCurrencyStatus(mockk(relaxed = true), mockk(relaxed = true)),
                accountCurrencies = listOf(defaultCurrency, accountCurrencyStatus(tronCurrencyId, tronNetworkId)),
                anchoredToStatus = defaultCurrency,
            )
            advanceUntilIdle()

            // Assert
            assertThat(model.dataState.toSwapCurrencyStatus?.status).isSameInstanceAs(defaultCurrency)
            model.onDestroy()
        }

    @Test
    fun `GIVEN top up and a single currency account WHEN model constructed THEN TO is left untouched`() = runTest {
        // Arrange — without multichain data the account owns one currency and nothing may re-anchor TO.
        val defaultCurrency = accountCurrencyStatus(polygonCurrencyId, polygonNetworkId)
        val model = createTopUpModelWithAccountCurrencies(
            sourceStatus = accountCurrencyStatus(tronCurrencyId, tronNetworkId),
            accountCurrencies = listOf(defaultCurrency),
            anchoredToStatus = defaultCurrency,
        )
        advanceUntilIdle()

        // Assert
        assertThat(model.dataState.toSwapCurrencyStatus?.status).isSameInstanceAs(defaultCurrency)
        model.onDestroy()
    }

    private val polygonCurrencyId: CryptoCurrency.ID = mockk(relaxed = true)
    private val polygonNetworkId: Network.ID = mockk(relaxed = true)
    private val tronCurrencyId: CryptoCurrency.ID = mockk(relaxed = true)
    private val tronNetworkId: Network.ID = mockk(relaxed = true)

    private fun accountCurrencyStatus(
        currencyId: CryptoCurrency.ID,
        networkId: Network.ID,
    ): CryptoCurrencyStatus = mockk(relaxed = true) {
        every { currency } returns mockk(relaxed = true) {
            every { id } returns currencyId
            every { network } returns mockk(relaxed = true) { every { id } returns networkId }
        }
    }

    private suspend fun createTopUpModelWithAccountCurrencies(
        sourceStatus: CryptoCurrencyStatus,
        accountCurrencies: List<CryptoCurrencyStatus>,
        anchoredToStatus: CryptoCurrencyStatus = accountCurrencies.first(),
    ): SwapModel {
        every { swapFeatureToggles.isAccountSwapFlowEnabled } returns true
        val wallet = accountFlowUserWallet()
        val paymentAccount = MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId).account
        val from = SwapCurrencyStatus(
            userWallet = wallet,
            status = sourceStatus,
            account = Account.Personal.createMainAccount(userWalletId),
        )
        val to = SwapCurrencyStatus(userWallet = wallet, status = anchoredToStatus, account = paymentAccount)
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
                isAccountFlowEnabled = any(),
            )
        } returns (from to to)
        coEvery { accountUnderlyingCurrencies.get(userWalletId) } returns accountCurrencies

        return createModel(accountFlow = AccountFlow.TopUp)
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