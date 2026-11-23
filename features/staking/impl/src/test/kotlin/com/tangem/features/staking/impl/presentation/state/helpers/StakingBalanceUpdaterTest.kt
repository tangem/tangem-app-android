package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.NetworkType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.FetchActionsUseCase
import com.tangem.domain.staking.FetchStakingYieldBalanceUseCase
import com.tangem.domain.staking.GetActionsUseCase
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.model.StakingBalance
import com.tangem.domain.staking.model.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolExitRequest
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.staking.single.SingleStakingBalanceSupplier
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

/**
 * Covers the post-transaction refresh of staking actions in [StakingBalanceUpdater]. The "pending"
 * overlay on the staking screen is driven by StakeKit actions with status PROCESSING, so once a stake
 * is submitted the updater must keep re-fetching actions until the backend stops reporting them as
 * PROCESSING — otherwise the pending badge stays forever ([REDACTED_TASK_KEY] / [REDACTED_TASK_KEY]). The poll is bounded
 * so it cannot loop indefinitely if the backend never settles.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakingBalanceUpdaterTest {

    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase = mockk(relaxed = true)
    private val getTxHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase = mockk()
    private val fetchActionsUseCase: FetchActionsUseCase = mockk()
    private val getActionsUseCase: GetActionsUseCase = mockk()
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter = mockk(relaxed = true)
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk(relaxed = true)
    private val fetchStakingYieldBalanceUseCase: FetchStakingYieldBalanceUseCase = mockk(relaxed = true)
    private val singleStakingBalanceSupplier: SingleStakingBalanceSupplier = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val solanaStatus = status(currency = solanaCoin)
    private val ethStatus = status(currency = ethereumCoin)
    private var fetchActionsCalls = 0

    @BeforeEach
    fun setUp() {
        clearMocks(
            getTxHistoryItemsCountUseCase,
            fetchActionsUseCase,
            getActionsUseCase,
            cryptoCurrencyBalanceFetcher,
            fetchPendingTransactionsUseCase,
            fetchStakingYieldBalanceUseCase,
            txHistoryContentUpdateEmitter,
            singleStakingBalanceSupplier,
            stakingIdFactory,
        )
        coEvery { getTxHistoryItemsCountUseCase(any(), any()) } returns 0.right()
        fetchActionsCalls = 0
        coEvery { fetchActionsUseCase(any(), any(), any(), any()) } coAnswers {
            fetchActionsCalls++
            Unit.right()
        }
    }

    @Test
    fun `GIVEN backend stops reporting PROCESSING WHEN updateAfterTransaction THEN polls until settled`() = runTest {
        // Arrange
        every { getActionsUseCase(any(), any()) } returnsMany listOf(
            flowOf(listOf(processingAction()).right()),
            flowOf(listOf(processingAction()).right()),
            flowOf(emptyList<StakingAction>().right()),
        )
        val updater = createUpdater(testScope = this)

        // Act
        updater.updateAfterTransaction()
        advanceUntilIdle()

        // Assert
        // 1 immediate fetch (show pending) + 2 polling fetches until the backend clears PROCESSING.
        coVerify(exactly = 3) { fetchActionsUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN backend keeps reporting PROCESSING WHEN updateAfterTransaction THEN polling is bounded`() = runTest {
        // Arrange
        every { getActionsUseCase(any(), any()) } returns flowOf(listOf(processingAction()).right())
        val updater = createUpdater(testScope = this)

        // Act
        updater.updateAfterTransaction()
        advanceUntilIdle()

        // Assert
        // 1 immediate fetch + ACTIONS_POLL_MAX_ATTEMPTS (6) polling fetches, then the loop stops.
        coVerify(exactly = 7) { fetchActionsUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN backend keeps reporting PROCESSING WHEN poll interval elapses THEN next fetch happens exactly at ten seconds`() =
        runTest {
            // Arrange
            every { getActionsUseCase(any(), any()) } returns flowOf(listOf(processingAction()).right())
            val updater = createUpdater(testScope = this)

            // Act
            updater.updateAfterTransaction()
            runCurrent()
            advanceTimeBy(9_999)
            runCurrent()
            val fetchesBeforeInterval = fetchActionsCalls
            advanceTimeBy(1)
            runCurrent()
            val fetchesAtInterval = fetchActionsCalls

            // Assert
            assertThat(fetchesBeforeInterval).isEqualTo(1)
            assertThat(fetchesAtInterval).isEqualTo(2)
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN actions without PROCESSING WHEN updateAfterTransaction THEN polling stops after the first fetch`(
        model: StopPollingModel,
    ) = runTest {
        // Arrange
        every { getActionsUseCase(any(), any()) } returns model.actions
        val updater = createUpdater(testScope = this)

        // Act
        updater.updateAfterTransaction()
        advanceUntilIdle()

        // Assert
        assertThat(fetchActionsCalls).isEqualTo(1)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PartialUpdate {

        @Test
        fun `GIVEN stakeKit solana integration WHEN partialUpdate THEN balance and processing actions fetched without delay`() =
            runTest {
                // Arrange
                val updater =
                    createUpdater(
                        testScope = this,
                        integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
                        status = solanaStatus,
                        userWallet = realUserWallet,
                    )

                // Act
                updater.partialUpdate()

                // Assert
                coVerify(exactly = 1) {
                    cryptoCurrencyBalanceFetcher.invokeAndAwait(
                        userWalletId = realUserWallet.walletId,
                        currency = solanaCoin,
                    )
                }
                coVerify(exactly = 1) {
                    fetchActionsUseCase(
                        userWalletId = realUserWallet.walletId,
                        cryptoCurrency = solanaCoin,
                        networkType = NetworkType.SOLANA,
                        stakingActionStatus = StakingActionStatus.PROCESSING,
                    )
                }
                assertThat(testScheduler.currentTime).isEqualTo(0L)
                coVerify(exactly = 0) { fetchPendingTransactionsUseCase(any(), any()) }
                coVerify(exactly = 0) { fetchStakingYieldBalanceUseCase(any(), any()) }
                coVerify(exactly = 0) { getTxHistoryItemsCountUseCase(any(), any()) }
            }

        // Pins current behaviour: StakeKit actions are fetched even for the P2P integration
        @Test
        fun `GIVEN p2p integration WHEN partialUpdate THEN stakeKit actions still fetched for ethereum`() = runTest {
            // Arrange
            val updater = createUpdater(
                testScope = this,
                integration = p2pIntegration(),
                status = ethStatus,
                userWallet = realUserWallet,
            )

            // Act
            updater.partialUpdate()

            // Assert
            coVerify(exactly = 1) {
                fetchActionsUseCase(
                    userWalletId = realUserWallet.walletId,
                    cryptoCurrency = ethereumCoin,
                    networkType = NetworkType.ETHEREUM,
                    stakingActionStatus = StakingActionStatus.PROCESSING,
                )
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PartialUpdateCancellation {

        @Test
        fun `GIVEN partialUpdate cancelled mid balance fetch WHEN scheduler drains THEN balance fetch still completes`() =
            runTest {
                // Arrange
                var isBalanceFetchCompleted = false
                coEvery { cryptoCurrencyBalanceFetcher.invokeAndAwait(userWalletId = any(), currency = any<CryptoCurrency>()) } coAnswers {
                    delay(1_000)
                    isBalanceFetchCompleted = true
                }
                val updater = createUpdater(testScope = this)
                val job = launch { updater.partialUpdate() }
                runCurrent()

                // Act
                job.cancel()
                advanceUntilIdle()

                // Assert
                assertThat(job.isCancelled).isTrue()
                assertThat(isBalanceFetchCompleted).isTrue()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PartialUpdateWithDelay {

        @Test
        fun `GIVEN delayed partial update WHEN less than eleven seconds pass THEN nothing fetched`() = runTest {
            // Arrange
            val updater =
                createUpdater(
                    testScope = this,
                    integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
                    status = solanaStatus,
                    userWallet = realUserWallet,
                )
            val job = launch { updater.partialUpdateWithDelay() }

            // Act
            advanceTimeBy(10_999)
            runCurrent()

            // Assert
            coVerify(
                exactly = 0,
            ) { cryptoCurrencyBalanceFetcher.invokeAndAwait(userWalletId = any(), currency = any()) }
            coVerify(
                exactly = 0,
            ) { fetchActionsUseCase(
                userWalletId = any(),
                cryptoCurrency = any(),
                networkType = any(),
                stakingActionStatus = any(),
            ) }
            job.cancel()
        }

        @Test
        fun `GIVEN delayed partial update WHEN eleven seconds pass THEN balance and actions fetched`() = runTest {
            // Arrange
            val updater =
                createUpdater(
                    testScope = this,
                    integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
                    status = solanaStatus,
                    userWallet = realUserWallet,
                )
            launch { updater.partialUpdateWithDelay() }

            // Act
            advanceTimeBy(11_000)
            runCurrent()

            // Assert
            coVerify(
                exactly = 1,
            ) { cryptoCurrencyBalanceFetcher.invokeAndAwait(userWalletId = any(), currency = any()) }
            coVerify(
                exactly = 1,
            ) { fetchActionsUseCase(
                userWalletId = any(),
                cryptoCurrency = any(),
                networkType = any(),
                stakingActionStatus = any(),
            ) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UpdateAfterTransaction {

        @BeforeEach
        fun setUpUpdateAfterTransaction() {
            every { getActionsUseCase(any(), any()) } returns flowOf(emptyList<StakingAction>().right())
        }

        @Test
        fun `GIVEN transaction sent WHEN updateAfterTransaction THEN pending txs and yield balance fetched immediately and balance after delay`() =
            runTest {
                // Arrange
                val updater =
                    createUpdater(
                        testScope = this,
                        integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
                        status = solanaStatus,
                        userWallet = realUserWallet,
                    )

                // Act
                updater.updateAfterTransaction()
                runCurrent()

                // Assert
                coVerify(exactly = 1) { fetchPendingTransactionsUseCase(realUserWallet.walletId, solanaCoin.network) }
                coVerify(exactly = 1) { fetchStakingYieldBalanceUseCase(realUserWallet.walletId, solanaCoin) }
                coVerify(
                    exactly = 0,
                ) { cryptoCurrencyBalanceFetcher.invokeAndAwait(userWalletId = any(), currency = any()) }

                advanceTimeBy(11_000)
                runCurrent()
                coVerify(
                    exactly = 1,
                ) { cryptoCurrencyBalanceFetcher.invokeAndAwait(userWalletId = any(), currency = any()) }
            }

        @Test
        fun `GIVEN tx history count right WHEN updateAfterTransaction THEN tx history update triggered`() = runTest {
            // Arrange
            coEvery { getTxHistoryItemsCountUseCase(any(), any()) } returns 3.right()
            val updater =
                createUpdater(
                    testScope = this,
                    integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
                    status = solanaStatus,
                    userWallet = realUserWallet,
                )

            // Act
            updater.updateAfterTransaction()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { txHistoryContentUpdateEmitter.triggerUpdate() }
            coVerify { getTxHistoryItemsCountUseCase(realUserWallet.walletId, solanaCoin) }
        }

        @Test
        fun `GIVEN tx history count left WHEN updateAfterTransaction THEN tx history update not triggered`() = runTest {
            // Arrange
            coEvery {
                getTxHistoryItemsCountUseCase(any(), any())
            } returns TxHistoryStateError.EmptyTxHistories().left()
            val updater =
                createUpdater(
                    testScope = this,
                    integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
                    status = solanaStatus,
                    userWallet = realUserWallet,
                )

            // Act
            updater.updateAfterTransaction()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { txHistoryContentUpdateEmitter.triggerUpdate() }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class P2PEthPoolBalancePolling {

        private val p2pStakingId = MockStaking.stakingId(MockStaking.P2P_ID, SOURCE_ADDRESS)
        private var fetchBalanceCalls = 0

        @BeforeEach
        fun setUpP2PEthPoolBalancePolling() {
            every { getActionsUseCase(any(), any()) } returns flowOf(emptyList<StakingAction>().right())
            coEvery {
                stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
            } returns p2pStakingId.right()
            fetchBalanceCalls = 0
            coEvery { fetchStakingYieldBalanceUseCase(any(), any()) } coAnswers {
                fetchBalanceCalls++
                Unit.right()
            }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN p2p balances after transaction WHEN updateAfterTransaction THEN polls until position changes`(
            model: PollingModel,
        ) = runTest {
            // Arrange
            coEvery {
                singleStakingBalanceSupplier.getSyncOrNull(any(), any())
            } returnsMany listOf(model.balanceBeforeIndexing) + model.polledBalances
            val updater = createP2PUpdater(testScope = this)

            // Act
            updater.updateAfterTransaction()
            advanceUntilIdle()

            // Assert
            assertThat(fetchBalanceCalls).isEqualTo(model.expectedFetches)
        }

        @Test
        fun `GIVEN p2p position never changes and slow fetches WHEN five minutes pass THEN polling stops`() = runTest {
            // Arrange
            coEvery { singleStakingBalanceSupplier.getSyncOrNull(any(), any()) } returns emptyBalance()
            val fetchDuration = 10_000L
            coEvery { fetchStakingYieldBalanceUseCase(any(), any()) } coAnswers {
                delay(fetchDuration)
                fetchBalanceCalls++
                Unit.right()
            }
            val updater = createP2PUpdater(testScope = this)

            // Act
            updater.updateAfterTransaction()
            advanceTimeBy(fetchDuration + 300_001)
            runCurrent()
            val fetchesAtDeadline = fetchBalanceCalls
            advanceUntilIdle()

            // Assert
            assertThat(fetchesAtDeadline).isGreaterThan(1)
            assertThat(fetchBalanceCalls).isEqualTo(fetchesAtDeadline)
        }

        @Test
        fun `GIVEN p2p position not changed WHEN fifteen seconds pass THEN next fetch happens`() = runTest {
            // Arrange
            coEvery { singleStakingBalanceSupplier.getSyncOrNull(any(), any()) } returns emptyBalance()
            val updater = createP2PUpdater(testScope = this)

            // Act
            updater.updateAfterTransaction()
            runCurrent()
            advanceTimeBy(14_999)
            runCurrent()
            val fetchesBeforeInterval = fetchBalanceCalls
            advanceTimeBy(1)
            runCurrent()
            val fetchesAtInterval = fetchBalanceCalls

            // Assert
            assertThat(fetchesBeforeInterval).isEqualTo(1)
            assertThat(fetchesAtInterval).isEqualTo(2)
        }

        @Test
        fun `GIVEN staking id unavailable WHEN updateAfterTransaction THEN balance fetched once without polling`() =
            runTest {
                // Arrange
                coEvery {
                    stakingIdFactory.create(userWalletId = any(), cryptoCurrency = any())
                } returns StakingIdFactory.Error.UnsupportedCurrency.left()
                val updater = createP2PUpdater(testScope = this)

                // Act
                updater.updateAfterTransaction()
                advanceUntilIdle()

                // Assert
                assertThat(fetchBalanceCalls).isEqualTo(1)
                coVerify(exactly = 0) { singleStakingBalanceSupplier.getSyncOrNull(any(), any()) }
            }

        @Test
        fun `GIVEN stakeKit integration WHEN updateAfterTransaction THEN p2p balance is not polled`() = runTest {
            // Arrange
            val updater = createUpdater(
                testScope = this,
                integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
                status = solanaStatus,
            )

            // Act
            updater.updateAfterTransaction()
            advanceUntilIdle()

            // Assert
            assertThat(fetchBalanceCalls).isEqualTo(1)
            coVerify(exactly = 0) { singleStakingBalanceSupplier.getSyncOrNull(any(), any()) }
        }

        private fun provideTestModels() = listOf(
            PollingModel(
                label = "deposit indexed on third poll",
                balanceBeforeIndexing = emptyBalance(),
                polledBalances = listOf(emptyBalance(), emptyBalance(), p2pBalance(staked = BigDecimal("0.06"))),
                expectedFetches = 4,
            ),
            PollingModel(
                label = "error and no emission keep polling",
                balanceBeforeIndexing = emptyBalance(),
                polledBalances = listOf(StakingBalance.Error(p2pStakingId), null, p2pBalance(staked = BigDecimal.ONE)),
                expectedFetches = 4,
            ),
            PollingModel(
                label = "same position in another scale keeps polling",
                balanceBeforeIndexing = p2pBalance(staked = BigDecimal.ONE),
                polledBalances = listOf(p2pBalance(staked = BigDecimal("1.0")), p2pBalance(staked = BigDecimal.TEN)),
                expectedFetches = 3,
            ),
            PollingModel(
                label = "reward-only update keeps polling",
                balanceBeforeIndexing = p2pBalance(staked = BigDecimal.ONE),
                polledBalances = listOf(
                    p2pBalance(staked = BigDecimal("1.01"), earned = BigDecimal("0.01")),
                    p2pBalance(staked = BigDecimal("2.01"), earned = BigDecimal("0.01")),
                ),
                expectedFetches = 3,
            ),
            PollingModel(
                label = "unstake moves stake to exit queue",
                balanceBeforeIndexing = p2pBalance(staked = BigDecimal.ONE),
                polledBalances = listOf(
                    p2pBalance(staked = BigDecimal.ZERO, exitRequests = listOf(exitRequest(isClaimable = false))),
                ),
                expectedFetches = 2,
            ),
            PollingModel(
                label = "exit request maturing keeps polling",
                balanceBeforeIndexing = p2pBalance(
                    staked = BigDecimal.ONE,
                    exitRequests = listOf(exitRequest(isClaimable = false)),
                ),
                polledBalances = listOf(
                    p2pBalance(staked = BigDecimal.ONE, exitRequests = listOf(exitRequest(isClaimable = true))),
                    p2pBalance(staked = BigDecimal.TEN, exitRequests = listOf(exitRequest(isClaimable = true))),
                ),
                expectedFetches = 3,
            ),
            PollingModel(
                label = "withdraw all empties position",
                balanceBeforeIndexing = p2pBalance(
                    staked = BigDecimal.ZERO,
                    exitRequests = listOf(exitRequest(isClaimable = true)),
                ),
                polledBalances = listOf(emptyBalance()),
                expectedFetches = 2,
            ),
            PollingModel(
                label = "balance before indexing unavailable falls back to status",
                balanceBeforeIndexing = null,
                polledBalances = listOf(p2pBalance(staked = BigDecimal.ONE)),
                expectedFetches = 2,
            ),
        )

        private fun createP2PUpdater(testScope: TestScope) = createUpdater(
            testScope = testScope,
            integration = p2pIntegration(),
            status = ethStatus,
        )

        private fun emptyBalance() = StakingBalance.Empty(stakingId = p2pStakingId, source = StatusSource.ACTUAL)

        private fun p2pBalance(
            staked: BigDecimal,
            earned: BigDecimal = BigDecimal.ZERO,
            exitRequests: List<P2PEthPoolExitRequest> = emptyList(),
        ) = MockStaking.p2pBalance(
            accounts = listOf(MockStaking.p2pAccount(staked = staked, earned = earned, exitRequests = exitRequests)),
            address = SOURCE_ADDRESS,
        )

        private fun exitRequest(isClaimable: Boolean) = P2PEthPoolExitRequest(
            ticket = "ticket",
            totalAssets = BigDecimal.ONE,
            timestamp = Instant.fromEpochMilliseconds(0),
            withdrawalTimestamp = null,
            isClaimable = isClaimable,
        )
    }

    internal data class PollingModel(
        val label: String,
        val balanceBeforeIndexing: StakingBalance?,
        val polledBalances: List<StakingBalance?>,
        val expectedFetches: Int,
    ) {
        override fun toString(): String = label
    }

    private fun createUpdater(
        testScope: TestScope,
        integration: StakingIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
        status: CryptoCurrencyStatus = solanaStatus,
        userWallet: UserWallet = realUserWallet,
    ) = StakingBalanceUpdater(
        fetchPendingTransactionsUseCase = fetchPendingTransactionsUseCase,
        getTxHistoryItemsCountUseCase = getTxHistoryItemsCountUseCase,
        fetchActionsUseCase = fetchActionsUseCase,
        getActionsUseCase = getActionsUseCase,
        txHistoryContentUpdateEmitter = txHistoryContentUpdateEmitter,
        cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
        fetchStakingYieldBalanceUseCase = fetchStakingYieldBalanceUseCase,
        singleStakingBalanceSupplier = singleStakingBalanceSupplier,
        stakingIdFactory = stakingIdFactory,
        coroutineScope = object : AppCoroutineScope {
            override val coroutineContext: CoroutineContext = testScope.coroutineContext
        },
        userWallet = userWallet,
        cryptoCurrencyStatus = status,
        integration = integration,
    )

    private fun provideTestModels() = listOf(
        StopPollingModel(label = "actions left", actions = flowOf(StakingError.DomainError("x").left())),
        StopPollingModel(label = "empty actions", actions = flowOf(emptyList<StakingAction>().right())),
        StopPollingModel(label = "no emission", actions = emptyFlow()),
        StopPollingModel(
            label = "only settled actions",
            actions = flowOf(listOf(processingAction().copy(status = StakingActionStatus.SUCCESS)).right()),
        ),
    )

    internal data class StopPollingModel(
        val label: String,
        val actions: Flow<Either<StakingError, List<StakingAction>>>,
    ) {
        override fun toString(): String = label
    }

    private fun processingAction() = StakingAction(
        id = "action-id",
        integrationId = "integration-id",
        status = StakingActionStatus.PROCESSING,
        type = StakingActionType.STAKE,
        currentStepIndex = 0,
        amount = BigDecimal.ONE,
        validatorAddress = null,
        validatorAddresses = null,
        transactions = null,
        createdAt = DateTime(0L),
    )
}