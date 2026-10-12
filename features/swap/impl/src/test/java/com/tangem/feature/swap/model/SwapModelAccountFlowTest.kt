package com.tangem.feature.swap.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute.Swap.AccountFlow
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.WithdrawalResult
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.interactor.PredictionWithdrawalFailure
import com.tangem.domain.polymarket.interactor.SubmitPredictionWithdrawalInteractor
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionWithdrawDestination
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt
import com.tangem.domain.polymarket.model.PreparedPredictionWithdrawal
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.ChangeCardsButtonState
import com.tangem.feature.swap.models.SwapButton
import com.tangem.feature.swap.models.SwapCardState
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.presentation.R
import com.tangem.feature.swap.router.SwapRoute
import com.tangem.feature.swap.ui.transfer.SwapTransferNotificationsFactory
import com.tangem.feature.swap.ui.transfer.SwapTransferStateBuilder
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.commonfeatures.api.choosetoken.ChooserBlock
import com.tangem.features.commonfeatures.api.choosetoken.PaymentAccountTokens
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.flow.flowOf
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

/**
 * Tests for mode-based withdrawal detection driven by [AccountFlow] plus the hidden/no-op reverse
 * (swap-direction) button whenever the swap screen is opened in an account flow (Tangem Pay
 * top-up/withdraw via swap).
 *
 * [SwapModel.isTangemPayWithdrawal] is true as soon as `accountFlow is Withdraw`, regardless of what is
 * in the FROM slot, and stays true for a FROM slot holding an [Account.Payment] token outside any account
 * flow. The reverse button is forced [ChangeCardsButtonState.HIDDEN] whenever an [AccountFlow] is present,
 * and the callback [SwapModel] exposes for it (`uiState.onChangeCardsClicked`) is a no-op.
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
        coEvery {
            getMinimumTransactionAmountSyncUseCase(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns Either.Right(null)
    }

    @Test
    fun `GIVEN Withdraw flow WHEN isTangemPayWithdrawal THEN true regardless of FROM slot`() = runTest {
        // Arrange
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk<Account.CryptoPortfolio>(relaxed = true))

        // Act
        val result = model.isTangemPayWithdrawal(fromWithPortfolioAccount)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN TopUp flow WHEN FROM is a normal wallet token THEN isTangemPayWithdrawal false`() =
        runTest {
            // Arrange — in a real TopUp flow, FROM is resolved to a wallet token from a crypto-portfolio
            // account (the Payment account is the TO side); it is never Account.Payment. See
            // InitialCurrenciesResolver.resolveAccountTopUpFromPriority, which only ever draws FROM from
            // cryptoPortfolioAccounts/cryptoCurrencyList.
            val model = createModel(accountFlow = AccountFlow.TopUp)
            val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk<Account.CryptoPortfolio>(relaxed = true))

            // Act
            val result = model.isTangemPayWithdrawal(fromWithPortfolioAccount)

            // Assert
            assertThat(result).isFalse()
        }

    @Test
    fun `GIVEN no account flow WHEN FROM is a Payment account THEN isTangemPayWithdrawal true`() =
        runTest {
            // Arrange — a regular (non-Tangem-Pay) swap entry (accountFlow == null) still lets the user
            // manually pick the Payment account as FROM (Settings.SwapFrom keeps the payment account
            // visible). The slot-based check must catch that case so it is routed through withdrawal
            // handling (CEX-only providers).
            val model = createModel(accountFlow = null)
            val fromWithPaymentAccount = swapCurrencyStatus(account = Account.Payment(userWalletId))

            // Act
            val result = model.isTangemPayWithdrawal(fromWithPaymentAccount)

            // Assert
            assertThat(result).isTrue()
        }

    @Test
    fun `GIVEN a target-only destination WHEN state emitted THEN reverse button HIDDEN`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(userWallet = mockk(relaxed = true))
        val to = from.copy(account = Account.Prediction(userWalletId))
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (from to to)

        // Act
        val model = createModel(accountFlow = null)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN a portfolio destination WHEN state emitted THEN reverse button not forced hidden`() = runTest {
        // Arrange
        val from = buildSwapCurrencyStatus(userWallet = mockk(relaxed = true))
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (from to from)

        // Act
        val model = createModel(accountFlow = null)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isNotEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN an account flow WHEN state emitted THEN reverse button HIDDEN`() = runTest {
        // Arrange & Act
        val model = createModel(accountFlow = AccountFlow.TopUp)

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN an account flow WHEN model constructed THEN the very first state is already HIDDEN`() =
        runTest {
            // Arrange — `initTokens()` (and every other `modelScope.launch { ... }` in `init {}`) is
            // dispatched on `main`/`mainImmediate`/`default`; the base's default dispatchers are all
            // `Dispatchers.Unconfined`, which would run those bodies synchronously during construction
            // and, via the setter, independently re-force HIDDEN — masking a bug in the `_uiState`
            // field initializer itself. Using `StandardTestDispatcher` for every role instead defers
            // all of that until the scheduler is advanced, so the assertion below observes the
            // initializer's value directly.
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
                )
            }
            assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)

            // Sanity: once the deferred work runs, the setter keeps enforcing the same rule.
            advanceUntilIdle()
            assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
        }

    @Test
    fun `GIVEN no account flow WHEN state emitted THEN reverse button not forced hidden`() = runTest {
        // Arrange & Act — standalone swap screen (no Tangem Pay account flow) must not be affected.
        val model = createModel(accountFlow = null)

        // Assert
        assertThat(model.uiState.changeCardsButtonState).isNotEqualTo(ChangeCardsButtonState.HIDDEN)
    }

    @Test
    fun `GIVEN an account flow WHEN onChangeCardsClicked invoked THEN no-op`() = runTest {
        // Arrange
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
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseFromTokenBridge.settings.chooserBlock).isInstanceOf(ChooserBlock.None::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN Withdraw flow WHEN from tokenFilter applied THEN only payment account tokens pass`() = runTest {
        // Arrange
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()
        val paymentAccountStatus = MockAccounts.createPaymentAccountStatus(userWalletId = userWalletId)
        val usdcPolygonStatus: CryptoCurrencyStatus = mockk(relaxed = true)
        val cryptoPortfolioStatus: AccountStatus.Personal = mockk(relaxed = true)
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
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseToTokenBridge.settings.chooserBlock).isInstanceOf(ChooserBlock.Market::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN Withdraw flow WHEN from selector settings THEN payment account lists every issued token`() = runTest {
        // Arrange & Act
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseFromTokenBridge.settings.paymentAccountTokens)
            .isEqualTo(PaymentAccountTokens.IssuedTokens)
        model.onDestroy()
    }

    @Test
    fun `GIVEN Withdraw flow WHEN to selector settings THEN the payment account is hidden`() = runTest {
        // Arrange & Act — withdrawing into the same account is not an operation the user can make.
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseToTokenBridge.settings.paymentAccountTokens)
            .isEqualTo(PaymentAccountTokens.Hidden)
        model.onDestroy()
    }

    @Test
    fun `GIVEN TopUp flow WHEN from selector settings THEN the payment account is hidden`() = runTest {
        // Arrange & Act — funding the account from itself is not an operation the user can make.
        val model = createModel(accountFlow = AccountFlow.TopUp)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseFromTokenBridge.settings.paymentAccountTokens)
            .isEqualTo(PaymentAccountTokens.Hidden)
        model.onDestroy()
    }

    @Test
    fun `GIVEN TopUp flow WHEN from selector settings THEN Markets stays shown`() = runTest {
        // Arrange & Act — only Withdraw restricts the FROM selector; TopUp keeps the plain SwapFrom settings.
        val model = createModel(accountFlow = AccountFlow.TopUp)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseFromTokenBridge.settings.chooserBlock).isInstanceOf(ChooserBlock.Market::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN TopUp flow WHEN to selector settings THEN the account's issued tokens are the destinations`() = runTest {
        // Arrange & Act — topping up targets the account, so every network it is issued on is a destination.
        val model = createModel(accountFlow = AccountFlow.TopUp)
        advanceUntilIdle()

        // Assert
        assertThat(model.chooseToTokenBridge.settings).isEqualTo(
            ChooseTokenBridge.Settings.SwapTo.copy(paymentAccountTokens = PaymentAccountTokens.IssuedTokens),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN Withdraw flow WHEN filterTangemPayProviders THEN only CEX providers kept`() = runTest {
        // Arrange
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
    fun `GIVEN TopUp flow WHEN filterTangemPayProviders THEN providers unfiltered`() = runTest {
        // Arrange
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
    fun `GIVEN TopUp flow WHEN model constructed THEN receive card is USD locked and title is Add funds`() =
        runTest {
            // Arrange
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
    fun `GIVEN TopUp flow WHEN model constructed THEN send card is not locked`() = runTest {
        // Arrange
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
    fun `GIVEN Withdraw flow WHEN model constructed THEN title is Withdraw and receive card unaffected`() =
        runTest {
            // Arrange
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
        val model = createModel(accountFlow = null)

        // Assert
        assertThat(model.uiState.titleId).isEqualTo(R.string.common_swap)
    }

    @Test
    fun `GIVEN TopUp flow WHEN model constructed THEN CTA mode matches an equivalent non-account swap run`() =
        runTest {
            // Arrange — hard constraint: abstracting the TO card must NOT repurpose the main button. Rather
            // than asserting the mode is "in" the full set of Mode entries (which is tautological — no value
            // of that type could ever fail it), drive the SAME stubbed FROM/TO pair through a plain,
            // non-account swap run and an account-flow (TopUp) run and assert they land on the IDENTICAL
            // CTA mode. This directly proves the account-flow presentation layer does not alter the CTA — a
            // future change that hardcodes/repurposes the CTA under an account flow would make the two
            // diverge and fail this test.
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
                )
            } returns (fromStatus to toStatus)

            // Act
            val plainSwapModel = createModel(accountFlow = null)
            val topUpModel = createModel(accountFlow = AccountFlow.TopUp)

            // Assert — same CTA mode regardless of account flow, and it is a real (non-"Send") mode.
            assertThat(topUpModel.uiState.swapButton.mode).isEqualTo(plainSwapModel.uiState.swapButton.mode)
            assertThat(topUpModel.uiState.swapButton.mode).isEqualTo(SwapButton.Mode.SWAP)
        }

    @Test
    fun `GIVEN TopUp flow WHEN isTangemPayWithdrawal THEN only the FROM slot account decides`() = runTest {
        // Arrange
        val model = createModel(accountFlow = AccountFlow.TopUp)
        val fromWithPaymentAccount = swapCurrencyStatus(account = Account.Payment(userWalletId))
        val fromWithPortfolioAccount = swapCurrencyStatus(account = mockk<Account.CryptoPortfolio>(relaxed = true))

        // Act & Assert — TopUp is not a withdrawal, so only the FROM slot's Account type matters.
        assertThat(model.isTangemPayWithdrawal(fromWithPaymentAccount)).isTrue()
        assertThat(model.isTangemPayWithdrawal(fromWithPortfolioAccount)).isFalse()
    }

    // -------------------------------------------------------------------------
    // Non-account swap entries unchanged (accountFlow = null) — completes the
    // existing title/reverse coverage with the selector-settings assertion.
    // -------------------------------------------------------------------------

    @Test
    fun `GIVEN no account flow WHEN model constructed THEN from and to selector settings are SwapFrom and SwapTo`() =
        runTest {
            // Arrange & Act
            val model = createModel(accountFlow = null)
            advanceUntilIdle()

            // Assert — a regular (non-Tangem-Pay) swap entry keeps the plain selector settings.
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
            val fromStatus = swapCurrencyStatus()
            val toStatus = swapCurrencyStatus()
            coEvery {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
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
            val fromStatus = swapCurrencyStatus()
            val toStatus = swapCurrencyStatus()
            coEvery {
                initialCurrenciesResolver.invoke(
                    userWalletId = any(),
                    initialCryptoCurrency = any(),
                    swapCurrencyPosition = any(),
                    accountFlow = any(),
                    initialToCryptoCurrency = any(),
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

    @Test
    fun `GIVEN a prediction withdraw WHEN checked THEN it is not a Tangem Pay withdrawal`() = runTest {
        // Arrange
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
        val fromCollateral = swapCurrencyStatus(account = Account.Prediction(userWalletId))

        // Act
        val actual = model.isTangemPayWithdrawal(fromCollateral)

        // Assert
        assertThat(actual).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction withdraw WHEN reverse is clicked THEN the pair does not flip`() = runTest {
        // Arrange
        val (collateral, destination) = stubResolvedPredictionPair()
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
        advanceUntilIdle()

        // Act
        model.uiState.onChangeCardsClicked()
        advanceUntilIdle()

        // Assert
        assertThat(model.dataState.fromSwapCurrencyStatus).isSameInstanceAs(collateral)
        assertThat(model.dataState.toSwapCurrencyStatus).isSameInstanceAs(destination)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no account flow and the collateral in FROM WHEN reverse is clicked THEN the pair does not flip`() =
        runTest {
            // Arrange
            val (collateral, destination) = stubResolvedPredictionPair()
            val model = createModel(accountFlow = null)
            advanceUntilIdle()

            // Act
            model.uiState.onChangeCardsClicked()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
            assertThat(model.dataState.fromSwapCurrencyStatus).isSameInstanceAs(collateral)
            assertThat(model.dataState.toSwapCurrencyStatus).isSameInstanceAs(destination)
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction withdraw WHEN the state is built THEN the send card is locked and the title is Withdraw`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair()
            stubTransferState()
            stubTransferStateBuilderPassthrough()

            // Act
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
            advanceUntilIdle()

            // Assert
            val sendCard = model.uiState.sendCardData as SwapCardState.SwapCardData
            assertThat(sendCard.isSelectionLocked).isTrue()
            assertThat(model.uiState.titleId).isEqualTo(R.string.prediction_main_withdraw)
            assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction top-up WHEN the state is built THEN the receive card is locked and the title is Add funds`() =
        runTest {
            // Arrange
            stubResolvedTopUpPair()

            // Act
            val model = createModel(accountFlow = AccountFlow.PredictionTopUp)
            advanceUntilIdle()

            // Assert
            val receiveCard = model.uiState.receiveCardData as SwapCardState.SwapCardData
            assertThat(receiveCard.isSelectionLocked).isTrue()
            assertThat(model.uiState.titleId).isEqualTo(R.string.common_add_funds)
            assertThat(model.uiState.changeCardsButtonState).isEqualTo(ChangeCardsButtonState.HIDDEN)
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction top-up WHEN the state is built THEN the send card stays selectable`() = runTest {
        // Arrange
        stubResolvedTopUpPair()

        // Act
        val model = createModel(accountFlow = AccountFlow.PredictionTopUp)
        advanceUntilIdle()

        // Assert
        val sendCard = model.uiState.sendCardData as SwapCardState.SwapCardData
        assertThat(sendCard.isSelectionLocked).isFalse()
        assertThat(sendCard.currencyIconState).isNotInstanceOf(CurrencyIconState.PaymentAccount::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction top-up WHEN the destination is unresolved THEN the empty receive card is still locked`() =
        runTest {
            // Arrange
            stubUnresolvedDestination()

            // Act
            val model = createModel(accountFlow = AccountFlow.PredictionTopUp)
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.receiveCardData as SwapCardState.Empty).isSelectionLocked).isTrue()
            assertThat((model.uiState.sendCardData as SwapCardState.SwapCardData).isSelectionLocked).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN an account top-up WHEN the destination is unresolved THEN the empty receive card is still locked`() =
        runTest {
            // Arrange
            stubUnresolvedDestination()

            // Act
            val model = createModel(accountFlow = AccountFlow.TopUp)
            advanceUntilIdle()

            // Assert
            assertThat((model.uiState.receiveCardData as SwapCardState.Empty).isSelectionLocked).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction top-up WHEN reverse is clicked THEN the pair does not flip`() = runTest {
        // Arrange
        val (from, collateral) = stubResolvedTopUpPair()
        val model = createModel(accountFlow = AccountFlow.PredictionTopUp)
        advanceUntilIdle()

        // Act
        model.uiState.onChangeCardsClicked()
        advanceUntilIdle()

        // Assert
        assertThat(model.dataState.fromSwapCurrencyStatus).isSameInstanceAs(from)
        assertThat(model.dataState.toSwapCurrencyStatus).isSameInstanceAs(collateral)
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction top-up WHEN the transfer is sent THEN the funding window opens`() = runTest {
        // Arrange
        val model = createTopUpTransferModel(accountFlow = AccountFlow.PredictionTopUp)

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { predictionFundingWindow.open(userWalletId) }
        verify { router.replaceAll(SwapRoute.Success) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction top-up WHEN the swap is sent THEN the funding window opens`() = runTest {
        // Arrange
        val model = createTopUpSwapModel(accountFlow = AccountFlow.PredictionTopUp)

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { predictionFundingWindow.open(userWalletId) }
        verify { router.replaceAll(SwapRoute.Success) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideTransferFeeTypes")
    fun `GIVEN a selected fee WHEN the transfer is sent THEN one Transaction sent with Source Transfer and its fee type`(
        feeType: AnalyticsParam.FeeType,
    ) = runTest {
        // Arrange
        val model = createTopUpTransferModel(accountFlow = null, feeType = feeType)
        val events = mutableListOf<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(events)) } returns Unit

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        val transactionSent = events.filterIsInstance<Basic.TransactionSent>()
        assertThat(transactionSent).hasSize(1)
        assertThat(transactionSent.single().params).containsAtLeast(
            AnalyticsParam.SOURCE, "Transfer",
            AnalyticsParam.FEE_TYPE, feeType.value,
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN no account flow WHEN the same transfer is sent THEN no funding window opens`() = runTest {
        // Arrange
        val model = createTopUpTransferModel(accountFlow = null)

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { predictionFundingWindow.open(any()) }
        verify { router.replaceAll(SwapRoute.Success) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction withdraw WHEN to selector settings THEN the chooser offers USDC on Polygon to add, hides payment tokens and joint accounts`() =
        runTest {
            // Arrange
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
            advanceUntilIdle()

            // Act
            val settings = model.chooseToTokenBridge.settings

            // Assert
            val chooserBlock = settings.chooserBlock as ChooserBlock.Predefined
            val predefinedTokens = chooserBlock.predefinedTokens.value
            assertThat(predefinedTokens).hasSize(1)
            val predefinedToken = predefinedTokens.single()
            assertThat(predefinedToken.token.id).isEqualTo(CryptoCurrency.RawID("usd-coin"))
            assertThat(predefinedToken.network.networkId).isEqualTo("polygon-pos")
            assertThat(predefinedToken.network.contractAddress)
                .isEqualTo(PolymarketContracts.USDC_TOKEN_ADDRESS)
            assertThat(predefinedToken.network.decimalCount).isEqualTo(6)
            assertThat(settings.paymentAccountTokens).isEqualTo(PaymentAccountTokens.Hidden)
            assertThat(settings.isJointAccountsShown).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction withdraw WHEN the currencies settle THEN the to tokenFilter is left to the predefined chooser`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
            advanceUntilIdle()
            val cryptoPortfolioStatus: AccountStatus.Personal = mockk(relaxed = true)
            val customStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
                every { currency.isCustom } returns true
            }

            // Act
            val toPredicate = model.chooseToTokenBridge.tokenFilter.value

            // Assert
            assertThat(toPredicate(cryptoPortfolioStatus, customStatus)).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction withdraw WHEN no amount is entered yet THEN the button already reads Withdraw`() = runTest {
        // Arrange
        stubResolvedPredictionPair()
        stubTransferState()
        stubTransferStateBuilderPassthrough()

        // Act
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.swapButton.mode).isEqualTo(SwapButton.Mode.TRANSFER)
        assertThat(model.uiState.swapButton.title).isEqualTo(resourceReference(R.string.prediction_main_withdraw))
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction withdraw WHEN the button goes into progress THEN it reads Withdrawing`() = runTest {
        // Arrange
        stubResolvedPredictionPair()
        stubTransferState()
        stubTransferStateBuilderPassthrough()
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
        advanceUntilIdle()

        // Act
        model.uiState = model.uiState.copy(
            swapButton = model.uiState.swapButton.copy(mode = SwapButton.Mode.SWAP_PROGRESSING),
        )

        // Assert
        assertThat(model.uiState.swapButton.mode).isEqualTo(SwapButton.Mode.TRANSFER_PROGRESSING)
        assertThat(model.uiState.swapButton.title).isEqualTo(resourceReference(R.string.common_withdrawing))
        model.onDestroy()
    }

    @Test
    fun `GIVEN a Tangem Pay withdraw WHEN no amount is entered yet THEN the button keeps its default label`() = runTest {
        // Arrange

        // Act
        val model = createModel(accountFlow = AccountFlow.Withdraw)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.swapButton.title).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction withdraw WHEN the destinations request fails THEN an error is shown`() = runTest {
        // Arrange
        val currencyStatus: CryptoCurrencyStatus = mockk(relaxed = true)
        val portfolioStatus = personalAccountStatus(listOf(currencyStatus))
        every { multiAccountStatusListSupplier() } returns
            flowOf(listOf(accountStatusList(listOf(portfolioStatus))))
        coEvery { getPredictionWithdrawDestinationsUseCase(any()) } returns PolymarketBridgeError.Network.left()

        // Act
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a prediction withdraw WHEN a destination is picked THEN the transfer path is taken and no provider is requested`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair()
            every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns false
            val transferState = stubTransferState()

            // Act
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw)
            advanceUntilIdle()

            // Assert
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
    fun `GIVEN a prediction withdraw WHEN the amount is entered THEN no network fee is requested, none is shown and the button needs none`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair()
            stubTransferState()
            stubTransferStateBuilderPassthrough()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
            advanceUntilIdle()

            // Act
            model.enterAmount("5")
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { feeSelectorReloadTrigger.triggerUpdate(any()) }
            verify {
                swapTransferStateBuilder.updateTransferButtonEnableState(
                    dataState = any(),
                    transferState = any(),
                    actions = any(),
                    uiStateHolder = any(),
                    feePaidCryptoCurrencyStatus = any(),
                    fee = null,
                    isTangemPayWithdrawal = true,
                    feeSelectorUM = null,
                    isHighNetworkFee = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction withdraw WHEN the transfer state is built THEN the fee selector state is withheld from the builder`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair()
            stubTransferState()
            stubTransferStateBuilderPassthrough()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
            advanceUntilIdle()

            // Act
            model.enterAmount("5")
            advanceUntilIdle()

            // Assert
            verify {
                swapTransferStateBuilder.createTransferState(
                    actions = any(),
                    transferState = any(),
                    uiStateHolder = any(),
                    feePaidCryptoCurrencyStatus = any(),
                    feeSelectorUM = null,
                    isHighNetworkFee = any(),
                )
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction withdraw WHEN confirmed THEN it is signed at once, tracked and the transfer success screen opens`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair(destination = polygonUsdcDestination())
            stubTransferState(sendingAmount = BigDecimal("5"))
            stubTransferStateBuilderPassthrough()
            stubStoredPolymarketAddresses()
            every { swapTransferStateBuilder.createPredictionWithdrawalSentState(any(), any()) } answers { firstArg() }
            val prepared = preparedWithdrawal()
            val receipt: PredictionWithdrawalReceipt = mockk()
            coEvery { submitPredictionWithdrawalInteractor.prepare(any()) } returns prepared.right()
            coEvery { submitPredictionWithdrawalInteractor.confirm(prepared) } returns receipt.right()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
            advanceUntilIdle()
            model.enterAmount("5")
            advanceUntilIdle()

            // Act
            model.uiState.swapButton.onClick()
            advanceUntilIdle()

            // Assert
            coVerifyOrder {
                submitPredictionWithdrawalInteractor.prepare(any())
                submitPredictionWithdrawalInteractor.confirm(prepared)
                predictionWithdrawalTracker.track(userWalletId)
            }
            verify { swapTransferStateBuilder.createPredictionWithdrawalSentState(any(), any()) }
            verify { router.replaceAll(SwapRoute.Success) }
            verify(exactly = 0) { messageSender.send(any()) }
            assertThat(model.uiState.bottomSheetConfig).isNull()
            coVerify(exactly = 0) { swapTransferInteractor.withdrawTangemPay(any(), any(), any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the destinations are not loaded yet WHEN confirmed THEN they are fetched on demand and the withdrawal is submitted`() =
        runTest {
            // Arrange
            val destination = polygonUsdcDestination()
            stubResolvedPredictionPair(destination = destination)
            val withdrawDestination = PredictionWithdrawDestination(
                currencyId = destination.currency.id,
                chainId = POLYGON_CHAIN_ID,
                tokenAddress = USDC_CONTRACT_ADDRESS.lowercase(),
            )
            coEvery { getPredictionWithdrawDestinationsUseCase(any()) } returnsMany listOf(
                PolymarketBridgeError.Network.left(),
                mapOf(destination.currency.id to withdrawDestination).right(),
            )
            stubTransferState(sendingAmount = BigDecimal("5"))
            stubTransferStateBuilderPassthrough()
            stubStoredPolymarketAddresses()
            val params = slot<SubmitPredictionWithdrawalInteractor.Params>()
            coEvery { submitPredictionWithdrawalInteractor.prepare(capture(params)) } returns
                PredictionWithdrawalFailure.NothingToResend.left()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
            advanceUntilIdle()
            model.enterAmount("5")
            advanceUntilIdle()

            // Act
            model.uiState.swapButton.onClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 2) { getPredictionWithdrawDestinationsUseCase(any()) }
            assertThat(params.captured.toChainId).isEqualTo(POLYGON_CHAIN_ID)
            assertThat(params.captured.toTokenAddress).isEqualTo(USDC_CONTRACT_ADDRESS.lowercase())
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction withdraw WHEN confirmed THEN the recipient is the user's address on the destination network`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair(destination = polygonUsdcDestination())
            stubTransferState(sendingAmount = BigDecimal("5"))
            stubTransferStateBuilderPassthrough()
            stubStoredPolymarketAddresses()
            val params = slot<SubmitPredictionWithdrawalInteractor.Params>()
            coEvery { submitPredictionWithdrawalInteractor.prepare(capture(params)) } returns
                mockk<PreparedPredictionWithdrawal>().right()
            coEvery { submitPredictionWithdrawalInteractor.confirm(any()) } returns
                mockk<PredictionWithdrawalReceipt>().right()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
            advanceUntilIdle()
            model.enterAmount("5")
            advanceUntilIdle()

            // Act
            model.uiState.swapButton.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(params.captured).isEqualTo(
                SubmitPredictionWithdrawalInteractor.Params(
                    userWalletId = userWalletId,
                    walletId = userWalletId.stringValue,
                    ownerAddress = OWNER_ADDRESS,
                    depositWalletAddress = DEPOSIT_WALLET_ADDRESS,
                    amount = BigDecimal("5"),
                    toChainId = "137",
                    toTokenAddress = USDC_CONTRACT_ADDRESS.lowercase(),
                    recipientAddress = RECIPIENT_ADDRESS,
                ),
            )
            model.onDestroy()
        }

    @Test
    fun `GIVEN a native destination WHEN confirmed THEN the bridge's own chain and asset are sent back`() =
        runTest {
            // Arrange
            val destination = polygonCoinDestination()
            stubResolvedPredictionPair(
                destination = destination,
                withdrawDestination = PredictionWithdrawDestination(
                    currencyId = destination.currency.id,
                    chainId = POLYGON_CHAIN_ID,
                    tokenAddress = NATIVE_ASSET_ADDRESS,
                ),
            )
            stubTransferState(sendingAmount = BigDecimal("5"))
            stubTransferStateBuilderPassthrough()
            stubStoredPolymarketAddresses()
            val params = slot<SubmitPredictionWithdrawalInteractor.Params>()
            coEvery { submitPredictionWithdrawalInteractor.prepare(capture(params)) } returns
                mockk<PreparedPredictionWithdrawal>().right()
            coEvery { submitPredictionWithdrawalInteractor.confirm(any()) } returns
                mockk<PredictionWithdrawalReceipt>().right()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
            advanceUntilIdle()
            model.enterAmount("5")
            advanceUntilIdle()

            // Act
            model.uiState.swapButton.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(params.captured.toChainId).isEqualTo(POLYGON_CHAIN_ID)
            assertThat(params.captured.toTokenAddress).isEqualTo(NATIVE_ASSET_ADDRESS)
            model.onDestroy()
        }

    @Test
    fun `GIVEN a prediction withdraw WHEN the interactor fails THEN an alert is shown and nothing is tracked`() = runTest {
        // Arrange
        stubResolvedPredictionPair(destination = polygonUsdcDestination())
        stubTransferState(sendingAmount = BigDecimal("5"))
        stubTransferStateBuilderPassthrough()
        stubStoredPolymarketAddresses()
        coEvery { submitPredictionWithdrawalInteractor.prepare(any()) } returns
            PredictionWithdrawalFailure.AddressUnavailable.left()
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.enterAmount("5")
        advanceUntilIdle()

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { submitPredictionWithdrawalInteractor.confirm(any()) }
        verify(exactly = 0) { predictionWithdrawalTracker.track(any()) }
        verify(exactly = 1) { messageSender.send(any()) }
        verify(exactly = 0) { router.replaceAll(SwapRoute.Success) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN a withdrawal already in flight WHEN confirmed THEN it is tracked, an alert is shown and the screen stays`() =
        runTest {
            // Arrange
            stubResolvedPredictionPair(destination = polygonUsdcDestination())
            stubTransferState(sendingAmount = BigDecimal("5"))
            stubTransferStateBuilderPassthrough()
            stubStoredPolymarketAddresses()
            coEvery { submitPredictionWithdrawalInteractor.prepare(any()) } returns
                PredictionWithdrawalFailure.AlreadyInFlight.left()
            val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
            advanceUntilIdle()
            model.enterAmount("5")
            advanceUntilIdle()

            // Act
            model.uiState.swapButton.onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { predictionWithdrawalTracker.track(userWalletId) }
            coVerify(exactly = 0) { submitPredictionWithdrawalInteractor.confirm(any()) }
            val alert = slot<DialogMessage>()
            verify(exactly = 1) { messageSender.send(capture(alert)) }
            assertThat(alert.captured.message).isEqualTo(resourceReference(R.string.prediction_withdraw_in_progress_message))
            verify(exactly = 0) { router.replaceAll(SwapRoute.Success) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN the card tap is cancelled WHEN confirmed THEN no alert is shown`() = runTest {
        // Arrange
        stubResolvedPredictionPair(destination = polygonUsdcDestination())
        stubTransferState(sendingAmount = BigDecimal("5"))
        stubTransferStateBuilderPassthrough()
        stubStoredPolymarketAddresses()
        coEvery { submitPredictionWithdrawalInteractor.prepare(any()) } returns
            mockk<PreparedPredictionWithdrawal>().right()
        coEvery { submitPredictionWithdrawalInteractor.confirm(any()) } returns
            PredictionWithdrawalFailure.SigningFailed(PolymarketSigningError.UserCancelled).left()
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.enterAmount("5")
        advanceUntilIdle()

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { messageSender.send(any()) }
        verify(exactly = 0) { router.replaceAll(SwapRoute.Success) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN the card signing fails WHEN confirmed THEN an alert is shown`() = runTest {
        // Arrange
        stubResolvedPredictionPair(destination = polygonUsdcDestination())
        stubTransferState(sendingAmount = BigDecimal("5"))
        stubTransferStateBuilderPassthrough()
        stubStoredPolymarketAddresses()
        coEvery { submitPredictionWithdrawalInteractor.prepare(any()) } returns
            mockk<PreparedPredictionWithdrawal>().right()
        coEvery { submitPredictionWithdrawalInteractor.confirm(any()) } returns
            PredictionWithdrawalFailure.SigningFailed(PolymarketSigningError.CardError).left()
        val model = createModel(accountFlow = AccountFlow.PredictionWithdraw, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.enterAmount("5")
        advanceUntilIdle()

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { messageSender.send(any()) }
        verify(exactly = 0) { predictionWithdrawalTracker.track(any()) }
        verify(exactly = 0) { router.replaceAll(SwapRoute.Success) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN an amount above the collateral balance WHEN entered THEN the primary button is disabled`() = runTest {
        // Arrange
        val (collateral, destination) = stubResolvedPredictionPair()
        val aboveBalance = requireNotNull(collateral.status.value.amount) + BigDecimal.ONE
        coEvery {
            swapTransferInteractor.updateTransfer(
                fromSwapCurrencyStatus = collateral,
                toSwapCurrencyStatus = destination,
                fromTokenAmount = aboveBalance.toPlainString(),
                feePaidCurrencyStatus = any(),
                fee = null,
            )
        } returns transferState(from = collateral, to = destination, amount = aboveBalance, isInsufficientBalance = true)
        val model = createModel(
            accountFlow = AccountFlow.PredictionWithdraw,
            dispatchers = deferredDispatchers(),
            swapTransferStateBuilder = realSwapTransferStateBuilder(),
        )
        advanceUntilIdle()

        // Act
        model.enterAmount(aboveBalance.toPlainString())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.isInsufficientFunds).isTrue()
        model.onDestroy()
    }

    private fun realSwapTransferStateBuilder(): SwapTransferStateBuilder {
        val notificationsFactory: SwapTransferNotificationsFactory = mockk {
            every { getNotifications(any(), any(), any(), any(), any()) } returns persistentListOf()
        }
        val isFeeApproximateUseCase: IsFeeApproximateUseCase = mockk {
            every { invoke(networkId = any(), amountType = any()) } returns false
        }
        return SwapTransferStateBuilder(
            notificationsFactory = notificationsFactory,
            isFeeApproximateUseCase = isFeeApproximateUseCase,
        )
    }

    private fun transferState(
        from: SwapCurrencyStatus,
        to: SwapCurrencyStatus,
        amount: BigDecimal,
        isInsufficientBalance: Boolean,
    ): SwapState.Transfer = SwapState.Transfer(
        userWallet = from.userWallet,
        fromTokenInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(value = amount, decimals = from.currency.decimals),
            amountFiat = BigDecimal.ZERO,
            swapCurrencyStatus = from,
        ),
        toTokenInfo = TokenSwapInfo(
            tokenAmount = SwapAmount(value = amount, decimals = to.currency.decimals),
            amountFiat = BigDecimal.ZERO,
            swapCurrencyStatus = to,
        ),
        cryptoCurrencyWarning = null,
        isInsufficientBalance = isInsufficientBalance,
        appCurrency = AppCurrency.Default,
        isBalanceHidden = false,
        isAccountsMode = false,
        isFeeCoverage = false,
        sendingAmount = amount,
        tronFeeNotificationShowCount = 0,
        isAmountSubtractAvailable = false,
    )

    private fun TestScope.deferredDispatchers(): TestingCoroutineDispatcherProvider {
        val deferred = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = deferred,
            mainImmediate = deferred,
            io = deferred,
            default = deferred,
        )
    }

    private fun SwapModel.enterAmount(value: String) {
        val sendCard = uiState.sendCardData as SwapCardState.SwapCardData
        requireNotNull(sendCard.amountField).onValueChange(value)
    }

    private fun stubTransferState(sendingAmount: BigDecimal = BigDecimal.ZERO): SwapState.Transfer {
        val transferState: SwapState.Transfer = mockk(relaxed = true) {
            every { this@mockk.sendingAmount } returns sendingAmount
        }
        coEvery {
            swapTransferInteractor.updateTransfer(
                fromSwapCurrencyStatus = any(),
                toSwapCurrencyStatus = any(),
                fromTokenAmount = any(),
                feePaidCurrencyStatus = any(),
                fee = any(),
            )
        } returns transferState
        return transferState
    }

    private fun stubTransferStateBuilderPassthrough() {
        every {
            swapTransferStateBuilder.createTransferState(any(), any(), any(), any(), any(), any())
        } answers {
            arg<SwapStateHolder>(2).copy(
                swapButton = SwapButton(
                    walletInteractionIcon = null,
                    isEnabled = false,
                    mode = SwapButton.Mode.TRANSFER,
                    onClick = arg<UiActions>(0).onTransferClick,
                ),
            )
        }
        every {
            swapTransferStateBuilder.updateTransferButtonEnableState(
                any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } answers { arg<SwapStateHolder>(3) }
        every { swapTransferStateBuilder.updateTransferTitle(any()) } answers { firstArg() }
        every { swapTransferStateBuilder.createTransferInProgressState(any()) } answers { firstArg() }
    }

    private fun preparedWithdrawal(): PreparedPredictionWithdrawal = mockk()

    private fun stubStoredPolymarketAddresses() {
        val addresses: PolymarketAddresses = mockk {
            every { ownerAddress } returns OWNER_ADDRESS
            every { depositWalletAddress } returns DEPOSIT_WALLET_ADDRESS
        }
        coEvery { derivePolymarketAddressesUseCase.stored(userWalletId) } returns addresses
    }

    private fun polygonUsdcDestination(): SwapCurrencyStatus {
        val usdc: CryptoCurrency.Token = mockk(relaxed = true) {
            every { symbol } returns "USDC"
            every { decimals } returns 6
            every { contractAddress } returns USDC_CONTRACT_ADDRESS
            every { network } returns mockk(relaxed = true) {
                every { id } returns mockk(relaxed = true)
                every { name } returns "Polygon"
                every { rawId } returns "polygon-pos"
            }
        }
        val statusValue: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
            every { amount } returns BigDecimal("10")
            every { fiatRate } returns BigDecimal.ONE
            every { networkAddress } returns NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    type = NetworkAddress.Address.Type.Primary,
                    value = RECIPIENT_ADDRESS,
                ),
            )
        }
        return SwapCurrencyStatus(
            userWallet = accountFlowUserWallet(),
            status = CryptoCurrencyStatus(currency = usdc, value = statusValue),
            account = Account.Personal.createMainAccount(userWalletId),
        )
    }

    private fun polygonCoinDestination(): SwapCurrencyStatus {
        val pol: CryptoCurrency.Coin = mockk(relaxed = true) {
            every { symbol } returns "POL"
            every { decimals } returns 18
            every { network } returns mockk(relaxed = true) {
                every { id } returns mockk(relaxed = true)
                every { name } returns "Polygon"
                every { rawId } returns "polygon-pos"
            }
        }
        val statusValue: CryptoCurrencyStatus.Value = mockk(relaxed = true) {
            every { amount } returns BigDecimal("10")
            every { fiatRate } returns BigDecimal.ONE
            every { networkAddress } returns NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(
                    type = NetworkAddress.Address.Type.Primary,
                    value = RECIPIENT_ADDRESS,
                ),
            )
        }
        return SwapCurrencyStatus(
            userWallet = accountFlowUserWallet(),
            status = CryptoCurrencyStatus(currency = pol, value = statusValue),
            account = Account.Personal.createMainAccount(userWalletId),
        )
    }

    private fun accountStatusList(accountStatuses: List<AccountStatus>) = AccountStatusList(
        userWalletId = userWalletId,
        accountStatuses = accountStatuses,
        totalAccounts = accountStatuses.size,
        totalArchivedAccounts = 0,
        totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
        sortType = TokensSortType.NONE,
        groupType = TokensGroupType.NONE,
    )

    private fun personalAccountStatus(
        currencies: List<CryptoCurrencyStatus>,
        userWalletId: UserWalletId = this.userWalletId,
    ): AccountStatus.Personal {
        return AccountStatus.Personal(
            account = Account.Personal.createMainAccount(userWalletId),
            tokenList = TokenList.Ungrouped(
                totalFiatBalance = TotalFiatBalance.Loaded(amount = BigDecimal.ZERO, source = StatusSource.ACTUAL),
                sortedBy = TokensSortType.NONE,
                currencies = currencies,
            ),
            priceChangeLce = Lce.Content(PriceChange(value = BigDecimal.ZERO, source = StatusSource.ACTUAL)),
        )
    }

    private fun stubResolvedPredictionPair(
        destination: SwapCurrencyStatus = buildSwapCurrencyStatus(accountFlowUserWallet()),
        withdrawDestination: PredictionWithdrawDestination = PredictionWithdrawDestination(
            currencyId = destination.currency.id,
            chainId = POLYGON_CHAIN_ID,
            tokenAddress = USDC_CONTRACT_ADDRESS.lowercase(),
        ),
    ): Pair<SwapCurrencyStatus, SwapCurrencyStatus> {
        val wallet = accountFlowUserWallet()
        val collateral = buildSwapCurrencyStatus(wallet).copy(account = Account.Prediction(userWalletId))
        every { multiAccountStatusListSupplier() } returns
            flowOf(listOf(accountStatusList(listOf(personalAccountStatus(listOf(destination.status))))))
        coEvery { getPredictionWithdrawDestinationsUseCase(any()) } returns
            mapOf(destination.currency.id to withdrawDestination).right()
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (collateral to destination)
        return collateral to destination
    }

    private fun stubUnresolvedDestination(): SwapCurrencyStatus {
        val from = buildSwapCurrencyStatus(accountFlowUserWallet())
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (from to null)
        return from
    }

    private fun TestScope.createTopUpTransferModel(
        accountFlow: AccountFlow?,
        feeType: AnalyticsParam.FeeType = AnalyticsParam.FeeType.Normal,
    ): SwapModel {
        val from = swapCurrencyStatus()
        val collateral = swapCurrencyStatus(account = Account.Prediction(userWalletId))
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (from to collateral)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns true
        stubTransferState(sendingAmount = BigDecimal("5"))
        stubTransferStateBuilderPassthrough()
        coEvery {
            swapTransferInteractor.sendTransfer(any(), any(), any(), any(), any())
        } returns TRANSFER_TX_HASH.right()
        every { getExplorerTransactionUrlUseCase(txHash = any(), currency = any()) } returns EXPLORER_URL.right()

        val model = createModel(accountFlow = accountFlow, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.feeSelectorRepository.state.value = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee = mockk(relaxed = true))
            every { toAnalyticType() } returns feeType
        }
        return model
    }

    private fun TestScope.createWithdrawTransferModel(): SwapModel {
        val payToken = swapCurrencyStatus(account = Account.Payment(userWalletId))
        val portfolioToken = swapCurrencyStatus()
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (payToken to portfolioToken)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns true
        stubTransferState(sendingAmount = BigDecimal("5"))
        stubTransferStateBuilderPassthrough()
        coEvery {
            swapTransferInteractor.withdrawTangemPay(any(), any(), any(), any())
        } returns WithdrawalResult.Success.right()

        val model = createModel(accountFlow = AccountFlow.Withdraw, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        return model
    }

    @Test
    fun `GIVEN a payment account withdrawal WHEN the transfer succeeds THEN the pair is recorded`() = runTest {
        // Arrange
        val model = createWithdrawTransferModel()

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert — the arguments are read outside the verification block because MockK runs that block in
        // recording mode, where a mock's property yields a fresh signature value instead of the stubbed one.
        val from = requireNotNull(model.dataState.fromSwapCurrencyStatus)
        val to = requireNotNull(model.dataState.toSwapCurrencyStatus)
        val fromUserWalletId = from.userWalletId
        val fromAccount = from.account
        val fromCurrency = from.currency
        val toUserWalletId = to.userWalletId
        val toAccount = to.account
        val toCurrency = to.currency
        coVerify(exactly = 1) {
            recordTokenTransferFromSwapUseCase(
                fromUserWalletId = fromUserWalletId,
                fromAccount = fromAccount,
                fromCurrency = fromCurrency,
                toUserWalletId = toUserWalletId,
                toAccount = toAccount,
                toCurrency = toCurrency,
            )
        }
        model.onDestroy()
    }

    private fun TestScope.createTopUpToPaymentTransferModel(): SwapModel {
        val portfolioToken = swapCurrencyStatus()
        val payToken = swapCurrencyStatus(account = Account.Payment(userWalletId))
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (portfolioToken to payToken)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns true
        stubTransferState(sendingAmount = BigDecimal("5"))
        stubTransferStateBuilderPassthrough()
        coEvery {
            swapTransferInteractor.sendTransfer(any(), any(), any(), any(), any())
        } returns TRANSFER_TX_HASH.right()
        every { getExplorerTransactionUrlUseCase(txHash = any(), currency = any()) } returns EXPLORER_URL.right()

        val model = createModel(accountFlow = AccountFlow.TopUp, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        // Without a selected fee onTransferClick falls through to the "Illegal state" branch instead of
        // reaching sendTransfer; the withdrawal helper needs none, its branch is checked earlier.
        model.feeSelectorRepository.state.value = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee = mockk(relaxed = true))
        }
        return model
    }

    @Test
    fun `GIVEN a payment account top up WHEN the transfer succeeds THEN the pair is recorded`() = runTest {
        // Arrange
        val model = createTopUpToPaymentTransferModel()

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        val from = requireNotNull(model.dataState.fromSwapCurrencyStatus)
        val to = requireNotNull(model.dataState.toSwapCurrencyStatus)
        val fromUserWalletId = from.userWalletId
        val fromAccount = from.account
        val fromCurrency = from.currency
        val toUserWalletId = to.userWalletId
        val toAccount = to.account
        val toCurrency = to.currency
        coVerify(exactly = 1) {
            recordTokenTransferFromSwapUseCase(
                fromUserWalletId = fromUserWalletId,
                fromAccount = fromAccount,
                fromCurrency = fromCurrency,
                toUserWalletId = toUserWalletId,
                toAccount = toAccount,
                toCurrency = toCurrency,
            )
        }
        model.onDestroy()
    }

    private fun TestScope.createTopUpSwapModel(accountFlow: AccountFlow?): SwapModel {
        stubResolvedTopUpPair()
        val provider = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns false
        coEvery { swapInteractor.getUnfulfilledReceiveRequirement(any()) } returns null
        coEvery {
            swapInteractor.onSwap(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns SwapTransactionState.TxSent(txHash = TRANSFER_TX_HASH, timestamp = 0L)
        every { getExplorerTransactionUrlUseCase(txHash = any(), currency = any()) } returns EXPLORER_URL.right()

        val model = createModel(accountFlow = accountFlow, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.dataState = model.dataState.copy(
            amount = "5",
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(provider to quotesLoadedState(provider)),
        )
        model.uiState = model.uiState.copy(providerState = providerContentState(provider))
        model.feeSelectorRepository.state.value = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee = mockk(relaxed = true))
        }
        return model
    }

    private fun providerContentState(provider: SwapProvider) = ProviderState.Content(
        id = provider.providerId,
        name = provider.name,
        type = provider.type.providerName,
        iconUrl = "",
        subtitle = stringReference(""),
        selectionType = ProviderState.SelectionType.CLICK,
        additionalBadge = ProviderState.AdditionalBadge.Empty,
        namePrefix = ProviderState.PrefixType.NONE,
        onProviderClick = {},
    )

    private fun stubResolvedTopUpPair(): Pair<SwapCurrencyStatus, SwapCurrencyStatus> {
        val wallet = accountFlowUserWallet()
        val from = buildSwapCurrencyStatus(wallet)
        val collateral = buildSwapCurrencyStatus(wallet).copy(account = Account.Prediction(userWalletId))
        coEvery {
            initialCurrenciesResolver.invoke(
                userWalletId = any(),
                initialCryptoCurrency = any(),
                swapCurrencyPosition = any(),
                accountFlow = any(),
                initialToCryptoCurrency = any(),
            )
        } returns (from to collateral)
        return from to collateral
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

    private companion object {
        @JvmStatic
        fun provideTransferFeeTypes() = listOf(
            AnalyticsParam.FeeType.Normal,
            AnalyticsParam.FeeType.Custom,
            AnalyticsParam.FeeType.Fixed,
        )

        const val OWNER_ADDRESS = "0xOwner"
        const val DEPOSIT_WALLET_ADDRESS = "0xDepositWallet"
        const val USDC_CONTRACT_ADDRESS = "0x3c499c542cEF5E3811e1192ce70d8cC03d5c3359"
        const val POLYGON_CHAIN_ID = "137"
        const val NATIVE_ASSET_ADDRESS = "0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE"
        const val RECIPIENT_ADDRESS = "0xRecipientOnPolygon"
        const val TRANSFER_TX_HASH = "0xTransferHash"
        const val EXPLORER_URL = "https://polygonscan.com/tx/0xTransferHash"
    }
}