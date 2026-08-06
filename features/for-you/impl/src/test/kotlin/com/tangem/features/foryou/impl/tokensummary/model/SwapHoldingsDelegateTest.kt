package com.tangem.features.foryou.impl.tokensummary.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.markets.tokenselector.TokenSelectorEntry
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.features.foryou.TokenSummaryComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * The delegate condenses the summary token's holdings into a single [SwapHoldingsState], so every case is asserted
 * through that state: which holdings survive matching, whether their balances make a swap pointless, and what
 * [GetCryptoCurrencyActionsUseCase] says about the ones that remain.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapHoldingsDelegateTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = currencyFactory.createCoin(Blockchain.Ethereum)
    private val bitcoin = currencyFactory.createCoin(Blockchain.Bitcoin)

    private val loadedEthereum = CryptoCurrencyStatus(currency = ethereum, value = loadedValue(BigDecimal.ONE))
    private val emptyEthereum = CryptoCurrencyStatus(currency = ethereum, value = loadedValue(BigDecimal.ZERO))
    private val loadingEthereum = CryptoCurrencyStatus(currency = ethereum, value = CryptoCurrencyStatus.Loading)

    private val accountsFlow = MutableStateFlow<List<AccountStatusList>>(value = emptyList())

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk()
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase = mockk()

    /** Cached so that the arranged portfolio and the expected [entry] refer to the very same fixtures. */
    private val wallets = mutableMapOf<UserWalletId, UserWallet>()
    private val portfolios = mutableMapOf<UserWalletId, AccountStatus.CryptoPortfolio>()

    private val delegateScopes = mutableListOf<CoroutineScope>()

    @BeforeEach
    fun setup() {
        clearMocks(multiAccountStatusListSupplier, getCryptoCurrencyActionsUseCase)

        every { multiAccountStatusListSupplier() } returns accountsFlow

        givenWallets(WALLET_ID)
        givenPortfolios(WALLET_ID to listOf(loadedEthereum))
        givenSwapReason(ScenarioUnavailabilityReason.None)
    }

    @AfterEach
    fun tearDown() {
        delegateScopes.forEach(CoroutineScope::cancel)
        delegateScopes.clear()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Matching {

        @ParameterizedTest
        @ProvideTestModels
        fun matching(model: MatchingModel) = runTest {
            // Arrange
            val delegate = createDelegate(token = model.token)

            // Act
            advanceUntilIdle()

            // Assert — a filtered-out holding leaves the token unheld, which reads as an offer to add it
            val expected = if (model.isMatched) {
                SwapHoldingsState.Resolved(holdings = listOf(holding(WALLET_ID, loadedEthereum)))
            } else {
                SwapHoldingsState.NotHeld
            }
            assertThat(delegate.state.value).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            MatchingModel(
                name = "GIVEN market token with the same raw id WHEN resolved THEN the holding matches",
                token = marketToken(ethereum),
                isMatched = true,
            ),
            MatchingModel(
                name = "GIVEN market token with another raw id WHEN resolved THEN the holding does not match",
                token = marketToken(bitcoin),
                isMatched = false,
            ),
            MatchingModel(
                name = "GIVEN portfolio token of the same network WHEN resolved THEN the holding matches",
                token = TokenSummaryComponent.Token.Portfolio(cryptoCurrency = ethereum),
                isMatched = true,
            ),
            MatchingModel(
                name = "GIVEN portfolio token of another network WHEN resolved THEN the holding does not match",
                token = TokenSummaryComponent.Token.Portfolio(cryptoCurrency = ethereum.copy(network = bitcoin.network)),
                isMatched = false,
            ),
        )

        @Test
        fun `GIVEN token without a raw id WHEN resolved THEN swap is unavailable and no actions are requested`() =
            runTest {
                // Arrange
                val delegate = createDelegate(token = TokenSummaryComponent.Token.Portfolio(customToken()))

                // Act
                advanceUntilIdle()

                // Assert — such a token matches no holding, and cannot be topped up through Manage funds either
                assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.Unavailable)
                verify(exactly = 0) { getCryptoCurrencyActionsUseCase(any(), any(), any()) }
            }

        @Test
        fun `GIVEN a wallet is missing from the list WHEN resolved THEN its account holdings are dropped`() = runTest {
            // Arrange — the wallets list and the account statuses can disagree while one of them is being updated
            givenWallets(WALLET_ID)
            givenPortfolios(OTHER_WALLET_ID to listOf(loadedEthereum))
            val delegate = createDelegate()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.NotHeld)
            verify(exactly = 0) { getCryptoCurrencyActionsUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN the token is only held by a locked wallet WHEN resolved THEN it can only be added`() = runTest {
            // Arrange — a swap cannot start from a wallet sitting behind the lock screen. The locked wallet's swap
            // reason is stubbed on purpose: the holding must be dropped before the availability check, not stall on
            // an unstubbed one
            givenWallets(LOCKED_WALLET_ID, lockedIds = setOf(LOCKED_WALLET_ID))
            givenPortfolios(LOCKED_WALLET_ID to listOf(loadedEthereum))
            givenSwapReason(ScenarioUnavailabilityReason.None, walletId = LOCKED_WALLET_ID)
            val delegate = createDelegate()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.NotHeld)
            verify(exactly = 0) { getCryptoCurrencyActionsUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN the token is held by an unlocked and a locked wallet WHEN resolved THEN only the unlocked one is offered`() =
            runTest {
                // Arrange
                givenWallets(WALLET_ID, LOCKED_WALLET_ID, lockedIds = setOf(LOCKED_WALLET_ID))
                givenPortfolios(
                    WALLET_ID to listOf(loadedEthereum),
                    LOCKED_WALLET_ID to listOf(loadedEthereum),
                )
                // Stubbed so that a leaked locked holding would be reported as an extra offer rather than stall
                givenSwapReason(ScenarioUnavailabilityReason.None, walletId = LOCKED_WALLET_ID)
                val delegate = createDelegate()

                // Act
                advanceUntilIdle()

                // Assert — the chooser must not list the locked wallet's holding next to the reachable one
                val expected = SwapHoldingsState.Resolved(holdings = listOf(holding(WALLET_ID, loadedEthereum)))
                assertThat(delegate.state.value).isEqualTo(expected)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Balances {

        @Test
        fun `GIVEN the token is not held at all WHEN resolved THEN it can only be added to a portfolio`() = runTest {
            // Arrange — the summary can be opened from a market review for a token the user does not own
            givenPortfolios(WALLET_ID to emptyList())
            val delegate = createDelegate()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.NotHeld)
            verify(exactly = 0) { getCryptoCurrencyActionsUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN every holding is empty WHEN resolved THEN add funds is offered and no actions are requested`() =
            runTest {
                // Arrange
                givenPortfolios(WALLET_ID to listOf(emptyEthereum))
                val delegate = createDelegate()

                // Act
                advanceUntilIdle()

                // Assert — there is nothing to swap from, so the availability check is pointless
                assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.ZeroBalance)
                verify(exactly = 0) { getCryptoCurrencyActionsUseCase(any(), any(), any()) }
            }

        @Test
        fun `GIVEN the portfolio has not arrived yet WHEN resolved THEN the state stays loading`() = runTest {
            // Arrange — the only state the button shimmers in, and the supplier's first emission always ends it
            every { multiAccountStatusListSupplier() } returns emptyFlow()
            val delegate = createDelegate()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.Loading)
        }

        @Test
        fun `GIVEN every balance is unread WHEN resolved THEN add funds is offered`() = runTest {
            // Arrange — holdings of a wallet nobody fetched this session stay loading indefinitely
            givenPortfolios(WALLET_ID to listOf(loadingEthereum))
            val delegate = createDelegate()

            // Act
            advanceUntilIdle()

            // Assert — an unread balance counts as empty, so the summary offers to top the token up
            assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.ZeroBalance)
            verify(exactly = 0) { getCryptoCurrencyActionsUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN one balance is unread WHEN another one has funds THEN both go through the availability check`() =
            runTest {
                // Arrange
                givenWallets(WALLET_ID, OTHER_WALLET_ID)
                givenPortfolios(
                    WALLET_ID to listOf(loadingEthereum),
                    OTHER_WALLET_ID to listOf(loadedEthereum),
                )
                givenSwapReason(ScenarioUnavailabilityReason.None, walletId = OTHER_WALLET_ID)
                val delegate = createDelegate()

                // Act
                advanceUntilIdle()

                // Assert — a loading status reports default ACTUAL sources, so the use case calls its swap available
                val expected = SwapHoldingsState.Resolved(
                    holdings = listOf(holding(WALLET_ID, loadingEthereum), holding(OTHER_WALLET_ID, loadedEthereum)),
                )
                assertThat(delegate.state.value).isEqualTo(expected)
            }

        @Test
        fun `GIVEN an empty holding WHEN funds arrive THEN add funds turns into swap`() = runTest {
            // Arrange
            givenPortfolios(WALLET_ID to listOf(emptyEthereum))
            val delegate = createDelegate()
            advanceUntilIdle()
            assertThat(delegate.state.value).isEqualTo(SwapHoldingsState.ZeroBalance)

            // Act
            givenPortfolios(WALLET_ID to listOf(loadedEthereum))
            advanceUntilIdle()

            // Assert
            val expected = SwapHoldingsState.Resolved(holdings = listOf(holding(WALLET_ID, loadedEthereum)))
            assertThat(delegate.state.value).isEqualTo(expected)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Availability {

        @ParameterizedTest
        @ProvideTestModels
        fun availability(model: AvailabilityModel) = runTest {
            // Arrange
            givenSwapReason(model.reason)
            val delegate = createDelegate()

            // Act
            advanceUntilIdle()

            // Assert
            val expected = SwapHoldingsState.Resolved(
                holdings = listOf(holding(WALLET_ID, loadedEthereum, model.expectedReason)),
            )
            assertThat(delegate.state.value).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            AvailabilityModel(
                name = "GIVEN swap is available WHEN resolved THEN the holding can be swapped from",
                reason = ScenarioUnavailabilityReason.None,
                expectedReason = ScenarioUnavailabilityReason.None,
            ),
            AvailabilityModel(
                name = "GIVEN the status is being refreshed WHEN resolved THEN the loading reason is kept",
                reason = ScenarioUnavailabilityReason.DataLoading,
                expectedReason = ScenarioUnavailabilityReason.DataLoading,
            ),
            AvailabilityModel(
                name = "GIVEN the refresh failed WHEN resolved THEN the outdated data reason is kept",
                reason = ScenarioUnavailabilityReason.UsedOutdatedData,
                expectedReason = ScenarioUnavailabilityReason.UsedOutdatedData,
            ),
            AvailabilityModel(
                name = "GIVEN the token is custom WHEN resolved THEN the custom token reason is kept",
                reason = ScenarioUnavailabilityReason.CustomToken(cryptoCurrencyName = "Ethereum"),
                expectedReason = ScenarioUnavailabilityReason.CustomToken(cryptoCurrencyName = "Ethereum"),
            ),
            AvailabilityModel(
                name = "GIVEN the wallet is single-currency WHEN resolved THEN the single wallet reason is kept",
                reason = ScenarioUnavailabilityReason.SingleWallet,
                expectedReason = ScenarioUnavailabilityReason.SingleWallet,
            ),
            AvailabilityModel(
                name = "GIVEN no swap action is offered at all WHEN resolved THEN a generic reason stands in",
                reason = null,
                expectedReason = ScenarioUnavailabilityReason.Unreachable,
            ),
        )

        @Test
        fun `GIVEN two wallets hold the token WHEN only one can swap THEN both are offered with their own reasons`() =
            runTest {
                // Arrange
                givenWallets(WALLET_ID, OTHER_WALLET_ID)
                givenPortfolios(
                    WALLET_ID to listOf(loadedEthereum),
                    OTHER_WALLET_ID to listOf(loadedEthereum),
                )
                givenSwapReason(ScenarioUnavailabilityReason.CustomToken("Ethereum"), walletId = WALLET_ID)
                givenSwapReason(ScenarioUnavailabilityReason.None, walletId = OTHER_WALLET_ID)
                val delegate = createDelegate()

                // Act
                advanceUntilIdle()

                // Assert — an unavailable holding is still offered, and says why when it is picked
                val expected = SwapHoldingsState.Resolved(
                    holdings = listOf(
                        holding(
                            walletId = WALLET_ID,
                            status = loadedEthereum,
                            reason = ScenarioUnavailabilityReason.CustomToken("Ethereum"),
                        ),
                        holding(walletId = OTHER_WALLET_ID, status = loadedEthereum),
                    ),
                )
                assertThat(delegate.state.value).isEqualTo(expected)
            }
    }

    // region arrange helpers

    private fun wallet(id: UserWalletId): UserWallet = wallets.getOrPut(id) {
        MockUserWalletFactory.create().copy(walletId = id, isMultiCurrency = true)
    }

    private fun portfolio(walletId: UserWalletId): AccountStatus.CryptoPortfolio = portfolios.getOrPut(walletId) {
        // `account` is dereferenced while grouping the holdings, so it is a real model; only the token list is faked.
        mockk {
            every { account } returns MockAccounts.createAccount(derivationIndex = 1, userWalletId = walletId)
            every { tokenList } returns mockk<TokenList>()
        }
    }

    /**
     * Declares which wallets the repository reports. Ids listed in [lockedIds] are reported as locked:
     * [MockUserWalletFactory] builds unlocked wallets — `UserWallet.Cold.isLocked` is
     * `scanResponse.card.wallets.isEmpty()` — so their card's wallets are emptied.
     */
    private fun givenWallets(vararg walletIds: UserWalletId, lockedIds: Set<UserWalletId> = emptySet()) {
        lockedIds.forEach { id ->
            val unlocked = MockUserWalletFactory.create().copy(walletId = id, isMultiCurrency = true)
            wallets[id] = unlocked.copy(
                scanResponse = unlocked.scanResponse.copy(
                    card = unlocked.scanResponse.card.copy(wallets = emptyList()),
                ),
            )
        }
        coEvery { userWalletsListRepository.userWalletsSync() } returns walletIds.map(::wallet)
    }

    private fun givenPortfolios(vararg holdings: Pair<UserWalletId, List<CryptoCurrencyStatus>>) {
        accountsFlow.value = holdings.map { (walletId, statuses) ->
            val portfolio = portfolio(walletId)
            every { portfolio.tokenList.flattenCurrencies() } returns statuses

            mockk<AccountStatusList> {
                every { userWalletId } returns walletId
                every { accountStatuses } returns listOf(portfolio)
            }
        }
    }

    private fun givenSwapReason(reason: ScenarioUnavailabilityReason?, walletId: UserWalletId = WALLET_ID) {
        val swapAction = reason?.let { TokenActionsState.ActionState.Swap(it, shouldShowBadge = false) }
        val actions = TokenActionsState(
            walletId = walletId,
            cryptoCurrencyStatus = loadedEthereum,
            states = listOfNotNull(swapAction),
        )

        every {
            getCryptoCurrencyActionsUseCase(match { it.walletId == walletId }, any(), any())
        } returns flowOf(actions)
    }

    private fun marketToken(currency: CryptoCurrency) = TokenSummaryComponent.Token.Market(
        cryptoCurrencyRawId = currency.id.rawCurrencyId!!,
        symbol = currency.symbol,
        title = currency.name,
        tangemIconUrl = "",
    )

    private fun customToken(): CryptoCurrency = mockk<CryptoCurrency.Token> {
        every { id } returns mockk { every { rawCurrencyId } returns null }
        every { network } returns ethereum.network
    }

    private fun entry(walletId: UserWalletId, status: CryptoCurrencyStatus) = TokenSelectorEntry(
        wallet = wallet(walletId),
        account = portfolio(walletId),
        currencyStatus = status,
    )

    private fun holding(
        walletId: UserWalletId,
        status: CryptoCurrencyStatus,
        reason: ScenarioUnavailabilityReason = ScenarioUnavailabilityReason.None,
    ) = SwapHolding(entry = entry(walletId, status), unavailabilityReason = reason)

    /**
     * The delegate is given a scope of its own — the one a `Model` would hand it — rather than `backgroundScope`,
     * whose work `advanceUntilIdle` does not drive. Every scope is cancelled after the test.
     */
    private fun TestScope.createDelegate(
        token: TokenSummaryComponent.Token = marketToken(ethereum),
    ): SwapHoldingsDelegate {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )

        return SwapHoldingsDelegate(
            userWalletsListRepository = userWalletsListRepository,
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
            getCryptoCurrencyActionsUseCase = getCryptoCurrencyActionsUseCase,
            dispatchers = dispatchers,
            modelScope = CoroutineScope(testDispatcher).also(delegateScopes::add),
            token = token,
        )
    }

    // endregion

    internal data class MatchingModel(
        val name: String,
        val token: TokenSummaryComponent.Token,
        val isMatched: Boolean,
    ) {
        override fun toString(): String = name
    }

    internal data class AvailabilityModel(
        val name: String,
        val reason: ScenarioUnavailabilityReason?,
        val expectedReason: ScenarioUnavailabilityReason,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val WALLET_ID = UserWalletId("01")
        val OTHER_WALLET_ID = UserWalletId("02")
        val LOCKED_WALLET_ID = UserWalletId("03")

        fun loadedValue(amount: BigDecimal) = CryptoCurrencyStatus.Loaded(
            amount = amount,
            fiatAmount = amount,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            stakingBalance = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = mockk(relaxed = true),
            sources = CryptoCurrencyStatus.Sources(),
        )
    }
}