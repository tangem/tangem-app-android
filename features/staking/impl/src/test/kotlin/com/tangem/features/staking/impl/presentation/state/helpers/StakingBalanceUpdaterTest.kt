package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.right
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.FetchActionsUseCase
import com.tangem.domain.staking.FetchStakingYieldBalanceUseCase
import com.tangem.domain.staking.GetActionsUseCase
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakingIntegration
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionStatus
import com.tangem.domain.tokens.FetchPendingTransactionsUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.utils.coroutines.AppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    private val userWallet: UserWallet = mockk(relaxed = true)
    private val integration: StakingIntegration = mockk(relaxed = true)
    private val cryptoCurrencyStatus = CryptoCurrencyStatus(
        currency = mockk<CryptoCurrency.Coin>(relaxed = true),
        value = mockk(relaxed = true),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(getTxHistoryItemsCountUseCase, fetchActionsUseCase, getActionsUseCase)
        coEvery { getTxHistoryItemsCountUseCase(any(), any()) } returns 0.right()
        coEvery { fetchActionsUseCase(any(), any(), any(), any()) } returns Unit.right()
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

    private fun createUpdater(testScope: TestScope) = StakingBalanceUpdater(
        fetchPendingTransactionsUseCase = fetchPendingTransactionsUseCase,
        getTxHistoryItemsCountUseCase = getTxHistoryItemsCountUseCase,
        fetchActionsUseCase = fetchActionsUseCase,
        getActionsUseCase = getActionsUseCase,
        txHistoryContentUpdateEmitter = txHistoryContentUpdateEmitter,
        cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
        fetchStakingYieldBalanceUseCase = fetchStakingYieldBalanceUseCase,
        coroutineScope = object : AppCoroutineScope {
            override val coroutineContext: CoroutineContext = testScope.coroutineContext
        },
        userWallet = userWallet,
        cryptoCurrencyStatus = cryptoCurrencyStatus,
        integration = integration,
    )

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