package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.CheckStakingTransactionUseCase
import com.tangem.domain.staking.GetConstructedStakingTransactionUseCase
import com.tangem.domain.staking.GetStakingTransactionsUseCase
import com.tangem.domain.staking.SaveUnsubmittedHashUseCase
import com.tangem.domain.staking.StakingTransactionVerdict
import com.tangem.domain.staking.SubmitHashUseCase
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Covers the build/validate reuse logic of [StakeKitTransactionSender]: a build validated in
 * [StakeKitTransactionSender.validate] is reused in [StakeKitTransactionSender.send] only when the
 * inputs are unchanged AND the build is not stale. Solana is reused only within a 50s TTL of when it
 * was built (its recentBlockhash expires) and rebuilt + re-validated afterwards. The contract under
 * test: the transaction that is sent is always the transaction that was security-validated, and UNSAFE
 * always blocks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StakeKitTransactionSenderTest {

    private val stateController: StakingStateController = mockk()
    private val stakingBalanceUpdaterFactory: StakingBalanceUpdater.Factory = mockk()
    private val balanceUpdater: StakingBalanceUpdater = mockk(relaxed = true)
    private val getStakingTransactionsUseCase: GetStakingTransactionsUseCase = mockk()
    private val getConstructedStakingTransactionUseCase: GetConstructedStakingTransactionUseCase = mockk()
    private val sendTransactionUseCase: SendTransactionUseCase = mockk()
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk()
    private val submitHashUseCase: SubmitHashUseCase = mockk()
    private val saveUnsubmittedHashUseCase: SaveUnsubmittedHashUseCase = mockk()
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase = mockk()
    private val checkStakingTransactionUseCase: CheckStakingTransactionUseCase = mockk()
    private val integration: StakeKitIntegration = mockk(relaxed = true)
    private val userWallet: UserWallet = mockk(relaxed = true)

    // Controllable wall clock: the test moves [currentTime] between validate() and send() to exercise
    // the Solana TTL deterministically.
    private var currentTime: Instant = BASE_TIME
    private val clock = object : Clock {
        override fun now(): Instant = currentTime
    }

    @BeforeEach
    fun setUp() {
        clearMocks(
            stateController,
            stakingBalanceUpdaterFactory,
            balanceUpdater,
            getStakingTransactionsUseCase,
            getConstructedStakingTransactionUseCase,
            sendTransactionUseCase,
            getExplorerTransactionUrlUseCase,
            submitHashUseCase,
            saveUnsubmittedHashUseCase,
            isFeeApproximateUseCase,
            checkStakingTransactionUseCase,
            integration,
            userWallet,
        )
        currentTime = BASE_TIME
        every { stakingBalanceUpdaterFactory.create(any(), any(), any()) } returns balanceUpdater
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns Either.Right(listOf("hash"))
        coEvery { submitHashUseCase(any()) } returns Either.Right(Unit)
        every { getExplorerTransactionUrlUseCase(txHash = any(), currency = any()) } returns Either.Right("url")
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN cache-policy inputs WHEN validate then send THEN rebuilds and re-validates as expected`(
        model: ReuseCase,
    ) = runTest {
        // Arrange
        givenBuild(network = model.network, verdict = model.verdict)
        val sender = createSender()
        var constructError: StakingError? = null

        // Act
        currentTime = BASE_TIME
        stubState(model.validateState)
        sender.validate()
        currentTime = BASE_TIME + model.elapsedBeforeSend
        stubState(model.sendState)
        sender.send(sendCallbacks(onConstructError = { constructError = it }))

        // Assert
        coVerify(exactly = model.expectedBuilds) {
            getConstructedStakingTransactionUseCase(any(), any(), any(), any())
        }
        coVerify(exactly = model.expectedScans) {
            checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any())
        }
        if (model.blocked) {
            assertThat(constructError).isInstanceOf(StakingError.TransactionValidationFailed::class.java)
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any(), any()) }
        }
    }

    // `@ProvideTestModels` lives in `:test:core`, which this module does not depend on, so we wire the
    // JUnit 5 `@MethodSource` directly — it is exactly what that annotation expands to.
    private fun provideTestModels() = listOf(
        ReuseCase(
            label = "deterministic chain, unchanged inputs → reused (built+scanned once)",
            network = NetworkType.POLYGON,
            expectedBuilds = 1,
            expectedScans = 1,
        ),
        ReuseCase(
            label = "solana within TTL → reused",
            network = NetworkType.SOLANA,
            elapsedBeforeSend = 10.seconds,
            expectedBuilds = 1,
            expectedScans = 1,
        ),
        ReuseCase(
            label = "solana past TTL → rebuilt + re-validated",
            network = NetworkType.SOLANA,
            elapsedBeforeSend = 60.seconds,
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "solana with clock jumped backward → rebuilt (non-monotonic guard)",
            network = NetworkType.SOLANA,
            elapsedBeforeSend = (-5).seconds,
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "amount changed between validate and send → rebuilt",
            network = NetworkType.POLYGON,
            sendState = StateArgs(cryptoAmount = BigDecimal("11")),
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "only pendingAction changed → rebuilt (key regression guard)",
            network = NetworkType.POLYGON,
            sendState = StateArgs(pendingAction = pendingAction(passthrough = "p1")),
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "only reduceAmountBy changed → rebuilt (key regression guard)",
            network = NetworkType.POLYGON,
            sendState = StateArgs(reduceAmountBy = BigDecimal.ONE),
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "unsafe on reused build → blocked, not sent",
            network = NetworkType.POLYGON,
            verdict = StakingTransactionVerdict.UNSAFE,
            expectedBuilds = 1,
            expectedScans = 1,
            blocked = true,
        ),
        ReuseCase(
            label = "unsafe on solana rebuilt past TTL → blocked, not sent",
            network = NetworkType.SOLANA,
            elapsedBeforeSend = 60.seconds,
            verdict = StakingTransactionVerdict.UNSAFE,
            expectedBuilds = 2,
            expectedScans = 2,
            blocked = true,
        ),
    )

    private fun createSender(): StakeKitTransactionSender = StakeKitTransactionSender(
        stateController = stateController,
        stakingBalanceUpdater = stakingBalanceUpdaterFactory,
        getStakingTransactionsUseCase = getStakingTransactionsUseCase,
        getConstructedStakingTransactionUseCase = getConstructedStakingTransactionUseCase,
        sendTransactionUseCase = sendTransactionUseCase,
        getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
        submitHashUseCase = submitHashUseCase,
        saveUnsubmittedHashUseCase = saveUnsubmittedHashUseCase,
        isFeeApproximateUseCase = isFeeApproximateUseCase,
        checkStakingTransactionUseCase = checkStakingTransactionUseCase,
        clock = clock,
        cryptoCurrencyStatus = cryptoCurrencyStatus(),
        userWallet = userWallet,
        integration = integration,
        isAmountSubtractAvailable = false,
    )

    private fun givenBuild(network: NetworkType, verdict: StakingTransactionVerdict) {
        coEvery {
            getStakingTransactionsUseCase(any(), any(), any())
        } returns Either.Right(listOf(stakingTransaction(network = network)))
        coEvery {
            getConstructedStakingTransactionUseCase(any(), any(), any(), any())
        } returns Either.Right(stakingTransaction(network = network) to mockk<TransactionData.Compiled>(relaxed = true))
        coEvery {
            checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any())
        } returns verdict
    }

    private fun stubState(args: StateArgs) {
        val feeContent = mockk<FeeState.Content>(relaxed = true) {
            every { fee } returns mockk<Fee.Common>(relaxed = true) {
                every { amount.value } returns args.feeValue
            }
        }
        val confirmation = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
            every { feeState } returns feeContent
            every { pendingActions } returns null
            every { this@mockk.pendingAction } returns args.pendingAction
            every { this@mockk.reduceAmountBy } returns args.reduceAmountBy
        }
        val validator = mockk<StakingStates.ValidatorState.Data>(relaxed = true) {
            every { chosenTarget.address } returns args.validatorAddress
        }
        val amount = mockk<AmountState.Data>(relaxed = true) {
            every { amountTextField.cryptoAmount.value } returns args.cryptoAmount
        }
        val state = mockk<StakingUiState>(relaxed = true) {
            every { confirmationState } returns confirmation
            every { amountState } returns amount
            every { validatorState } returns validator
            every { this@mockk.actionType } returns args.actionType
        }
        every { stateController.value } returns state
    }

    private fun cryptoCurrencyStatus(): CryptoCurrencyStatus {
        val statusValue = mockk<CryptoCurrencyStatus.Value>(relaxed = true) {
            every { networkAddress } returns mockk(relaxed = true) {
                every { defaultAddress } returns mockk(relaxed = true) {
                    every { value } returns ACCOUNT_ADDRESS
                }
            }
        }
        return CryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Coin>(relaxed = true),
            value = statusValue,
        )
    }

    private fun stakingTransaction(network: NetworkType) = StakingTransaction(
        id = "tx-1",
        network = network,
        status = StakingTransactionStatus.CREATED,
        type = StakingTransactionType.STAKE,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = "unsigned",
        stepIndex = 0,
        error = null,
        gasEstimate = null,
        stakeId = null,
        explorerUrl = null,
        ledgerHwAppId = null,
        isMessage = false,
    )

    private fun pendingAction(passthrough: String) = PendingAction(
        type = StakingActionType.CLAIM_REWARDS,
        passthrough = passthrough,
        args = null,
    )

    private fun sendCallbacks(
        onConstructError: (StakingError) -> Unit = {},
        onSendSuccess: (String) -> Unit = {},
    ) = StakingTransactionSender.Callbacks(
        onConstructSuccess = {},
        onConstructError = onConstructError,
        onSendSuccess = onSendSuccess,
        onSendError = {},
        onFeeIncreased = { _, _ -> },
        onTransactionExpired = {},
    )

    /** Build inputs that determine the unsigned transaction; every field defaults so a case overrides only what it tests. */
    internal data class StateArgs(
        val cryptoAmount: BigDecimal = BigDecimal.TEN,
        val feeValue: BigDecimal = BigDecimal.ONE,
        val validatorAddress: String = "VALIDATOR",
        val actionType: StakingActionCommonType = StakingActionCommonType.Enter(skipEnterAmount = false),
        val pendingAction: PendingAction? = null,
        val reduceAmountBy: BigDecimal? = null,
    )

    internal data class ReuseCase(
        val label: String,
        val network: NetworkType,
        val validateState: StateArgs = StateArgs(),
        val sendState: StateArgs = StateArgs(),
        val elapsedBeforeSend: Duration = Duration.ZERO,
        val verdict: StakingTransactionVerdict = StakingTransactionVerdict.SAFE,
        val expectedBuilds: Int,
        val expectedScans: Int,
        val blocked: Boolean = false,
    ) {
        override fun toString(): String = label
    }

    private companion object {
        const val ACCOUNT_ADDRESS = "ACCOUNT_ADDRESS"
        val BASE_TIME: Instant = Instant.fromEpochSeconds(1_700_000_000)
    }
}