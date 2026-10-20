package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.CheckStakingTransactionUseCase
import com.tangem.domain.staking.GetConstructedStakingTransactionUseCase
import com.tangem.domain.staking.GetStakingTransactionsUseCase
import com.tangem.domain.staking.SaveUnsubmittedHashUseCase
import com.tangem.domain.staking.StakingTransactionVerdict
import com.tangem.domain.staking.SubmitHashUseCase
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.SubmitHashData
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.staking.model.toStakingTarget
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.models.TxStatusError
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.runSuspendCatching
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/**
 * Covers [StakeKitTransactionSender]: validation, construction and sending, plus the build reuse between
 * [StakeKitTransactionSender.validate] and [StakeKitTransactionSender.send]. A validated build is reused
 * only when every build input is unchanged. The top-level reuse tests prove that the build that is sent
 * is the build that was security-checked last, and that UNSAFE always blocks.
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

    private var currentStatus: CryptoCurrencyStatus = status()

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
        )
        currentStatus = status()
        every { stakingBalanceUpdaterFactory.create(any(), any(), any()) } returns balanceUpdater
        coEvery {
            sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
        } returns Either.Right(listOf("hash"))
        coEvery { submitHashUseCase(any()) } returns Either.Right(Unit)
        every { getExplorerTransactionUrlUseCase(txHash = any(), currency = any()) } returns Either.Right("url")
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN cache-policy inputs WHEN validate then send THEN sends exactly the build that was scanned last`(
        model: ReuseCase,
    ) = runTest {
        // Arrange
        givenBuild(network = model.network, verdict = model.verdict)
        val sender = createSender(model.network)
        val recorder = Recorder()
        val expectedBuild = "build-${model.expectedBuilds}"

        // Act
        stubState(model.validateState, model.network)
        sender.validate()
        stubState(model.sendState, model.network)
        sender.send(recorder.callbacks)

        // Assert
        coVerify(exactly = model.expectedBuilds) {
            getConstructedStakingTransactionUseCase(any(), any(), any(), any())
        }
        coVerify(exactly = model.expectedScans) {
            checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any())
        }
        coVerify(exactly = 1) {
            checkStakingTransactionUseCase(
                network = any(),
                accountAddress = any(),
                unsignedTransaction = "unsigned-$expectedBuild",
                token = any(),
                blockchain = any(),
                provider = any(),
            )
        }
        if (model.blocked) {
            assertThat(recorder.constructErrors).containsExactly(
                StakingError.TransactionValidationFailed("Transaction blocked by security validation"),
            )
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any(), any()) }
        } else {
            assertThat(recorder.constructErrors).isEmpty()
            coVerify(exactly = 1) {
                sendTransactionUseCase(
                    txsData = listOf(compiled(expectedBuild)),
                    userWallet = realUserWallet,
                    network = currencyFor(model.network).network,
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            }
        }
    }

    // Pins current behaviour: a re-validate that fails to build returns early without dropping the earlier build, so send reuses it with no fresh security check
    @Test
    fun `GIVEN validated build and re-validate fails to build WHEN send with same inputs THEN earlier build reused and sent`() =
        runTest {
            // Arrange
            givenBuild(network = NetworkType.POLYGON, verdict = StakingTransactionVerdict.SAFE)
            val sender = createSender(NetworkType.POLYGON)
            stubState(StateArgs(), NetworkType.POLYGON)
            sender.validate()
            coEvery {
                getStakingTransactionsUseCase(any(), any(), any())
            } returns Either.Left(StakingError.DomainError("re-validate failed"))
            val recorder = Recorder()

            // Act
            val revalidateVerdict = sender.validate()
            sender.send(recorder.callbacks)

            // Assert
            assertThat(revalidateVerdict).isEqualTo(StakingTransactionVerdict.SAFE)
            assertThat(recorder.constructErrors).isEmpty()
            coVerify(exactly = 1) { getConstructedStakingTransactionUseCase(any(), any(), any(), any()) }
            coVerify(exactly = 1) { checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                sendTransactionUseCase(
                    txsData = listOf(compiled("build-1")),
                    userWallet = realUserWallet,
                    network = polygonToken.network,
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            }
        }

    // Pins current behaviour: a re-validate on a state without fee returns early without dropping the earlier build, so send reuses it with no fresh security check
    @Test
    fun `GIVEN validated build and re-validate without fee WHEN send with original inputs THEN earlier build reused and sent`() =
        runTest {
            // Arrange
            givenBuild(network = NetworkType.POLYGON, verdict = StakingTransactionVerdict.SAFE)
            val sender = createSender(NetworkType.POLYGON)
            stubState(StateArgs(), NetworkType.POLYGON)
            sender.validate()
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(feeState = FeeState.Loading),
            )
            val revalidateVerdict = sender.validate()
            stubState(StateArgs(), NetworkType.POLYGON)
            val recorder = Recorder()

            // Act
            sender.send(recorder.callbacks)

            // Assert
            assertThat(revalidateVerdict).isEqualTo(StakingTransactionVerdict.SAFE)
            assertThat(recorder.constructErrors).isEmpty()
            coVerify(exactly = 1) { getConstructedStakingTransactionUseCase(any(), any(), any(), any()) }
            coVerify(exactly = 1) { checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                sendTransactionUseCase(
                    txsData = listOf(compiled("build-1")),
                    userWallet = realUserWallet,
                    network = polygonToken.network,
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            }
        }

    @Test
    fun `GIVEN status without address WHEN send THEN no address error then empty build error and nothing sent`() =
        runTest {
            // Arrange
            givenBuild(network = NetworkType.POLYGON, verdict = StakingTransactionVerdict.SAFE)
            val sender = createSender(NetworkType.POLYGON)
            stubState(StateArgs(), NetworkType.POLYGON)
            currentStatus = status(currency = polygonToken, address = null)
            val recorder = Recorder()

            // Act
            sender.send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).containsExactly(
                StakingError.DomainError("No available address"),
                StakingError.DomainError("fullTransactionsData is null or empty"),
            ).inOrder()
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any(), any()) }
        }

    @Test
    fun `GIVEN status recovers address after failed send WHEN send again THEN sent with fresh address`() = runTest {
        // Arrange
        givenBuild(network = NetworkType.POLYGON, verdict = StakingTransactionVerdict.SAFE)
        val paramsSlot = slot<ActionParams>()
        coEvery {
            getStakingTransactionsUseCase(any(), any(), capture(paramsSlot))
        } returns Either.Right(listOf(tx("tx-1", network = NetworkType.POLYGON)))
        val sender = createSender(NetworkType.POLYGON)
        stubState(StateArgs(), NetworkType.POLYGON)
        currentStatus = status(currency = polygonToken, address = null)

        // Act
        sender.send(Recorder().callbacks)
        currentStatus = status(currency = polygonToken, address = FRESH_ADDRESS)
        sender.send(Recorder().callbacks)

        // Assert
        assertThat(paramsSlot.captured.address).isEqualTo(FRESH_ADDRESS)
        coVerify(exactly = 1) { sendTransactionUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN status loses address after build WHEN validate THEN UNSAFE without caching the build`() = runTest {
        // Arrange
        givenBuild(network = NetworkType.POLYGON, verdict = StakingTransactionVerdict.SAFE)
        val sender = createSender(NetworkType.POLYGON)
        stubState(StateArgs(), NetworkType.POLYGON)
        coEvery { getConstructedStakingTransactionUseCase(any(), any(), any(), any()) } coAnswers {
            currentStatus = status(currency = polygonToken, address = null)
            val build = tx("build-1", network = NetworkType.POLYGON)
            Either.Right(build to compiled(build.id))
        }

        // Act
        val actual = sender.validate()

        // Assert
        assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
        coVerify(exactly = 0) { checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any()) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Validate {

        @BeforeEach
        fun setUpValidate() = stubDefaults()

        @Test
        fun `GIVEN confirmation state empty WHEN validate THEN SAFE and nothing built`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(confirmationState = StakingStates.ConfirmationState.Empty())

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertSafeAndNothingBuilt(actual)
        }

        @Test
        fun `GIVEN fee state loading WHEN validate THEN SAFE and nothing built`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(feeState = FeeState.Loading),
            )

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertSafeAndNothingBuilt(actual)
        }

        @Test
        fun `GIVEN fee content without fee WHEN validate THEN SAFE and nothing built`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(confirmationState = confirmationData(fee = null))

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertSafeAndNothingBuilt(actual)
        }

        @Test
        fun `GIVEN amount state empty WHEN validate THEN SAFE and nothing built`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(amountState = AmountState.Empty)

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertSafeAndNothingBuilt(actual)
        }

        @Test
        fun `GIVEN status without address WHEN validate THEN SAFE and nothing built`() = runTest {
            // Arrange
            val sender = createRealSender(status = status(address = null))

            // Act
            val actual = sender.validate()

            // Assert
            assertSafeAndNothingBuilt(actual)
        }

        @Test
        fun `GIVEN staking transactions left WHEN validate THEN SAFE and no security check`() = runTest {
            // Arrange
            coEvery {
                getStakingTransactionsUseCase(any(), any(), any())
            } returns Either.Left(StakingError.DomainError("x"))

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(
                exactly = 0,
            ) { checkStakingTransactionUseCase(
                network = any(),
                accountAddress = any(),
                unsignedTransaction = any(),
                token = any(),
                blockchain = any(),
                provider = any(),
            ) }
        }

        // Pins current behaviour: a partially failed build skips the security check, so validate reports SAFE
        @Test
        fun `GIVEN one of two constructions fails WHEN validate THEN SAFE and no security check`() = runTest {
            // Arrange
            givenTransactions(tx("tx-1"), tx("tx-2"))
            coEvery {
                getConstructedStakingTransactionUseCase(
                    networkId = any(),
                    fee = any(),
                    amount = any(),
                    transactionId = "tx-1",
                )
            } returns Either.Left(StakingError.DomainError("construct failed"))

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(exactly = 0) { checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN constructed transactions with SAFE and WARNING WHEN validate THEN WARNING`() = runTest {
            // Arrange
            givenTransactions(tx("tx-1"), tx("tx-2"))
            givenVerdictForUnsigned("unsigned-tx-2", StakingTransactionVerdict.WARNING)

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.WARNING)
        }

        @Test
        fun `GIVEN constructed transactions with SAFE and UNSAFE WHEN validate THEN UNSAFE`() = runTest {
            // Arrange
            givenTransactions(tx("tx-1"), tx("tx-2"))
            givenVerdictForUnsigned("unsigned-tx-2", StakingTransactionVerdict.UNSAFE)

            // Act
            val actual = createRealSender().validate()

            // Assert
            assertThat(actual).isEqualTo(StakingTransactionVerdict.UNSAFE)
        }

        @Test
        fun `GIVEN solana build WHEN validate THEN security check gets stakeKit provider and currency data`() =
            runTest {
                // Act
                createRealSender().validate()

                // Assert
                coVerify(exactly = 1) {
                    checkStakingTransactionUseCase(
                        network = NetworkType.SOLANA,
                        accountAddress = SOURCE_ADDRESS,
                        unsignedTransaction = "unsigned-tx-1",
                        token = "SOL",
                        blockchain = "Solana",
                        provider = "StakeKit",
                    )
                }
            }

        private fun assertSafeAndNothingBuilt(actual: StakingTransactionVerdict) {
            assertThat(actual).isEqualTo(StakingTransactionVerdict.SAFE)
            coVerify(exactly = 0) { getStakingTransactionsUseCase(any(), any(), any()) }
            coVerify(
                exactly = 0,
            ) { checkStakingTransactionUseCase(
                network = any(),
                accountAddress = any(),
                unsignedTransaction = any(),
                token = any(),
                blockchain = any(),
                provider = any(),
            ) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Construct {

        @BeforeEach
        fun setUpConstruct() = stubDefaults()

        @Test
        fun `GIVEN fee state not content WHEN send THEN no fee provided error and nothing built`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(feeState = FeeState.Error),
            )
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).containsExactly(StakingError.DomainError("No fee provided"))
            coVerify(exactly = 0) { getStakingTransactionsUseCase(any(), any(), any()) }
        }

        // Pins current behaviour: a missing confirmation state throws instead of reaching onConstructError.
        @Test
        fun `GIVEN confirmation state empty WHEN send THEN throws illegal state`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(confirmationState = StakingStates.ConfirmationState.Empty())
            val recorder = Recorder()

            // Act
            val error = runSuspendCatching { createRealSender().send(recorder.callbacks) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("No confirmation state")
            assertThat(recorder.constructErrors).isEmpty()
        }

        // Pins current behaviour: a missing amount state throws instead of reaching onConstructError.
        @Test
        fun `GIVEN amount state empty WHEN send THEN throws illegal state`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(amountState = AmountState.Empty)

            // Act
            val error = runSuspendCatching { createRealSender().send(Recorder().callbacks) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("No amount state")
        }

        // Pins current behaviour: a balance target does not replace the validator, and an empty build is reported twice
        @Test
        fun `GIVEN no chosen validator even with balance target WHEN send THEN no validator error then empty build error`() =
            runTest {
                // Arrange
                every { stateController.value } returns uiState(
                    validatorState = StakingStates.ValidatorState.Empty(),
                    balanceState = balanceState("balance-target"),
                )
                val recorder = Recorder()

                // Act
                createRealSender().send(recorder.callbacks)

                // Assert
                assertThat(recorder.constructErrors).containsExactly(
                    StakingError.DomainError("No validator provided"),
                    StakingError.DomainError("fullTransactionsData is null or empty"),
                ).inOrder()
                coVerify(exactly = 0) { getStakingTransactionsUseCase(any(), any(), any()) }
                coVerify(
                    exactly = 0,
                ) { sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any()) }
            }

        // Pins current behaviour: a failed transactions request is followed by a second, generic build error
        @Test
        fun `GIVEN staking transactions left WHEN send THEN error forwarded then empty build error`() = runTest {
            // Arrange
            coEvery {
                getStakingTransactionsUseCase(any(), any(), any())
            } returns Either.Left(StakingError.DomainError("create failed"))
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).containsExactly(
                StakingError.DomainError("create failed"),
                StakingError.DomainError("fullTransactionsData is null or empty"),
            ).inOrder()
        }

        @Test
        fun `GIVEN approval and skipped transactions WHEN send THEN only remaining transaction constructed`() =
            runTest {
                // Arrange
                givenTransactions(
                    tx("approval", type = StakingTransactionType.APPROVAL),
                    tx("skipped", status = StakingTransactionStatus.SKIPPED),
                    tx("stake"),
                )

                // Act
                createRealSender().send(Recorder().callbacks)

                // Assert
                coVerify(
                    exactly = 1,
                ) { getConstructedStakingTransactionUseCase(
                    networkId = any(),
                    fee = any(),
                    amount = any(),
                    transactionId = "stake",
                ) }
                coVerify(
                    exactly = 0,
                ) { getConstructedStakingTransactionUseCase(
                    networkId = any(),
                    fee = any(),
                    amount = any(),
                    transactionId = "approval",
                ) }
                coVerify(
                    exactly = 0,
                ) { getConstructedStakingTransactionUseCase(
                    networkId = any(),
                    fee = any(),
                    amount = any(),
                    transactionId = "skipped",
                ) }
                coVerify(exactly = 1) {
                    sendTransactionUseCase(
                        txsData = listOf(compiled("stake")),
                        userWallet = any(),
                        network = any(),
                        sendMode = any(),
                    )
                }
            }

        // Pins current behaviour: a failed construction is reported, the remaining transactions are still sent
        @Test
        fun `GIVEN one of two constructions fails WHEN send THEN error reported and remaining transaction still sent`() =
            runTest {
                // Arrange
                givenTransactions(tx("tx-1"), tx("tx-2"))
                coEvery {
                    getConstructedStakingTransactionUseCase(networkId = any(), fee = any(), amount = any(), transactionId = "tx-1")
                } returns Either.Left(StakingError.DomainError("construct failed"))
                val recorder = Recorder()

                // Act
                createRealSender().send(recorder.callbacks)

                // Assert
                assertThat(recorder.constructErrors).containsExactly(StakingError.DomainError("construct failed"))
                coVerify(exactly = 1) {
                    sendTransactionUseCase(
                        txsData = listOf(compiled("tx-2")),
                        userWallet = any(),
                        network = any(),
                        sendMode = any(),
                    )
                }
            }

        @Test
        fun `GIVEN built transaction WHEN send THEN constructed with network raw id fee and transaction id`() =
            runTest {
                // Act
                createRealSender().send(Recorder().callbacks)

                // Assert
                coVerify {
                    getConstructedStakingTransactionUseCase(
                        networkId = solanaCoin.network.id.rawId,
                        fee = fee(),
                        amount = any(),
                        transactionId = "tx-1",
                    )
                }
            }

        @Test
        fun `GIVEN pending action WHEN send THEN passthrough and type forwarded`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(
                    pendingAction = MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS, "passthrough-1"),
                ),
                actionType = StakingActionCommonType.Pending.Rewards,
            )
            val paramsSlot = slot<ActionParams>()
            coEvery {
                getStakingTransactionsUseCase(any(), any(), capture(paramsSlot))
            } returns Either.Right(listOf(tx("tx-1")))

            // Act
            createRealSender().send(Recorder().callbacks)

            // Assert
            assertThat(paramsSlot.captured).isEqualTo(
                ActionParams(
                    actionCommonType = StakingActionCommonType.Pending.Rewards,
                    integrationId = MockStaking.SOLANA_ID,
                    amount = BigDecimal.TEN,
                    address = SOURCE_ADDRESS,
                    validatorAddress = MockStaking.VALIDATOR_ADDRESS,
                    token = MockStaking.solToken,
                    passthrough = "passthrough-1",
                    type = StakingActionType.CLAIM_REWARDS,
                ),
            )
        }

        // Pins current behaviour: a null entered amount throws from getAmount instead of reaching onConstructError
        @Test
        fun `GIVEN crypto amount null WHEN send THEN throws No amount value and nothing sent`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(amountState = amountData(null))
            val recorder = Recorder()

            // Act
            val error = runSuspendCatching { createRealSender().send(recorder.callbacks) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("No amount value")
            assertThat(recorder.constructErrors).isEmpty()
            coVerify(exactly = 0) { getStakingTransactionsUseCase(any(), any(), any()) }
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN fee value null WHEN send THEN throws No fee value and nothing sent`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(
                    fee = Fee.Common(Amount(currencySymbol = "SOL", value = null, decimals = 9)),
                ),
            )
            val recorder = Recorder()

            // Act
            val error = runSuspendCatching { createRealSender().send(recorder.callbacks) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalStateException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("No fee value")
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN solana withdraw pending actions WHEN send THEN staking transactions requested per action`() =
            runTest {
                // Arrange
                every { stateController.value } returns uiState(
                    confirmationState = confirmationData(
                        pendingActions = persistentListOf(withdraw("pt-1"), withdraw("pt-2")),
                    ),
                    actionType = StakingActionCommonType.Pending.Other,
                )
                val requested = mutableListOf<ActionParams>()
                coEvery {
                    getStakingTransactionsUseCase(any(), any(), capture(requested))
                } answers {
                    val passthrough = requested.last().passthrough
                    Either.Right(listOf(tx(if (passthrough == "pt-1") "tx-1" else "tx-2")))
                }
                givenConstructed(tx("tx-1"), tx("tx-2"))
                val sentSlot = slot<List<TransactionData>>()
                coEvery {
                    sendTransactionUseCase(txsData = capture(sentSlot), userWallet = any(), network = any(), sendMode = any())
                } returns Either.Right(listOf("h-1", "h-2"))

                // Act
                createRealSender().send(Recorder().callbacks)

                // Assert
                assertThat(requested.map { it.passthrough }).containsExactly("pt-1", "pt-2")
                assertThat(sentSlot.captured).containsExactly(compiled("tx-1"), compiled("tx-2"))
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN subtract flag and action type WHEN send THEN amount reduced only for enter`(model: SubtractModel) =
            runTest {
                // Arrange
                every { stateController.value } returns uiState(
                    amountState = amountData(BigDecimal.TEN),
                    actionType = model.actionType,
                )
                val paramsSlot = slot<ActionParams>()
                coEvery {
                    getStakingTransactionsUseCase(any(), any(), capture(paramsSlot))
                } returns Either.Right(listOf(tx("tx-1")))
                val sender = createRealSender(
                    status = status(balance = BigDecimal.TEN),
                    isAmountSubtractAvailable = model.isSubtractAvailable,
                )

                // Act
                sender.send(Recorder().callbacks)

                // Assert
                assertThat(paramsSlot.captured.amount).isEquivalentAccordingToCompareTo(model.expected)
            }

        private fun withdraw(passthrough: String) =
            MockStaking.pendingAction(type = StakingActionType.WITHDRAW, passthrough = passthrough)

        private fun provideTestModels() = listOf(
            SubtractModel(StakingActionCommonType.Enter(shouldSkipEnterAmount = false), true, BigDecimal("9")),
            SubtractModel(StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false), true, BigDecimal("10")),
            SubtractModel(StakingActionCommonType.Enter(shouldSkipEnterAmount = false), false, BigDecimal("10")),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Send {

        @BeforeEach
        fun setUpSend() = stubDefaults()

        @Test
        fun `GIVEN fee lower than total gas WHEN send THEN fee increased to total and nothing sent`() = runTest {
            // Arrange
            givenTransactions(tx("tx-1", gas = BigDecimal("0.7")), tx("tx-2", gas = BigDecimal("0.6")))
            every { isFeeApproximateUseCase(any(), any()) } returns true
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.feeIncreased).containsExactly(fee(BigDecimal("1.3"), "SOL", 9) to true)
            assertThat(recorder.constructed).isEmpty()
            coVerify(exactly = 0) {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            }
            verify { isFeeApproximateUseCase(networkId = solanaCoin.network.id, amountType = AmountType.Coin) }
        }

        @Test
        fun `GIVEN fee equal to total gas WHEN send THEN construct success and sent`() = runTest {
            // Arrange
            givenTransactions(tx("tx-1", gas = BigDecimal("0.4")), tx("tx-2", gas = BigDecimal("0.6")))
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.feeIncreased).isEmpty()
            assertThat(recorder.constructed.single().map { it.id }).containsExactly("tx-1", "tx-2")
            coVerify(
                exactly = 1,
            ) { sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any()) }
        }

        @Test
        fun `GIVEN WARNING verdict without prior validate WHEN send THEN built transaction still sent`() = runTest {
            // Arrange
            givenVerdictForUnsigned("unsigned-tx-1", StakingTransactionVerdict.WARNING)
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.constructErrors).isEmpty()
            assertThat(recorder.successUrls).containsExactly("url")
            coVerify(exactly = 1) {
                sendTransactionUseCase(
                    txsData = listOf(compiled("tx-1")),
                    userWallet = realUserWallet,
                    network = solanaCoin.network,
                    sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
                )
            }
        }

        // Pins current behaviour: a batch Left after the first tx was already broadcast leaves no submit_hash trace.
        @Test
        fun `GIVEN send left WHEN send THEN send error and no hash submit or balance update`() = runTest {
            // Arrange
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns Either.Left(SendTransactionError.UserCancelledError)
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.sendErrors).containsExactly(SendTransactionError.UserCancelledError)
            coVerify(exactly = 0) { submitHashUseCase(any()) }
            verify(exactly = 0) { stakingBalanceUpdaterFactory.create(any(), any(), any()) }
            assertThat(recorder.successUrls).isEmpty()
        }

        @Test
        fun `GIVEN submit hash left WHEN send THEN unsubmitted hash saved and success still reported`() = runTest {
            // Arrange
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns Either.Right(listOf("hash-1"))
            coEvery { submitHashUseCase(any()) } returns Either.Left(StakingError.DomainError("x"))
            coEvery { saveUnsubmittedHashUseCase(any(), any()) } returns Either.Right(Unit)
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            coVerify(exactly = 1) { saveUnsubmittedHashUseCase(transactionId = "tx-1", transactionHash = "hash-1") }
            assertThat(recorder.successUrls).containsExactly("url")
        }

        @Test
        fun `GIVEN submit hash right WHEN send THEN unsubmitted hash not saved`() = runTest {
            // Arrange
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns Either.Right(listOf("hash-1"))

            // Act
            createRealSender().send(Recorder().callbacks)

            // Assert
            coVerify {
                submitHashUseCase(SubmitHashData(transactionHash = "hash-1", transactionId = "tx-1"))
            }
            coVerify(exactly = 0) { saveUnsubmittedHashUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN transactions out of step order WHEN send THEN sent and submitted sorted by step index`() = runTest {
            // Arrange
            givenTransactions(tx("tx-a", stepIndex = 1), tx("tx-b", stepIndex = 0))
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns Either.Right(listOf("h0", "h1"))

            // Act
            createRealSender().send(Recorder().callbacks)

            // Assert
            coVerify {
                sendTransactionUseCase(
                    txsData = listOf(compiled("tx-b"), compiled("tx-a")),
                    userWallet = realUserWallet,
                    network = solanaCoin.network,
                    sendMode = any(),
                )
            }
            coVerifyOrder {
                submitHashUseCase(SubmitHashData(transactionHash = "h0", transactionId = "tx-b"))
                submitHashUseCase(SubmitHashData(transactionHash = "h1", transactionId = "tx-a"))
            }
            verify { getExplorerTransactionUrlUseCase(txHash = "h1", currency = solanaCoin) }
        }

        @Test
        fun `GIVEN explorer url left WHEN send THEN success with empty url`() = runTest {
            // Arrange
            every {
                getExplorerTransactionUrlUseCase(txHash = any(), currency = any())
            } returns Either.Left(TxStatusError.EmptyUrlError)
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            assertThat(recorder.successUrls).containsExactly("")
        }

        @Test
        fun `GIVEN successful send WHEN send THEN balance updater refreshes after transaction`() = runTest {
            // Arrange
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            verify(exactly = 1) { stakingBalanceUpdaterFactory.create(status(), realUserWallet, any()) }
            verify(exactly = 1) { balanceUpdater.updateAfterTransaction() }
            assertThat(recorder.successUrls).containsExactly("url")
        }

        // Pins current behaviour: send throws NoSuchElementException on `transactionHashes.last()` when no hashes come back
        @Test
        fun `GIVEN send returns empty hashes WHEN send THEN throws NoSuchElementException and nothing submitted`() =
            runTest {
                // Arrange
                coEvery {
                    sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
                } returns Either.Right(emptyList())
                val recorder = Recorder()

                // Act
                val error = runSuspendCatching { createRealSender().send(recorder.callbacks) }.exceptionOrNull()

                // Assert
                assertThat(error).isInstanceOf(NoSuchElementException::class.java)
                coVerify(exactly = 0) { submitHashUseCase(any()) }
                coVerify(exactly = 0) { saveUnsubmittedHashUseCase(any(), any()) }
                assertThat(recorder.successUrls).isEmpty()
            }

        // Pins current behaviour: hashes are paired with transactions by position, unmatched transactions are skipped
        @Test
        fun `GIVEN fewer hashes than transactions WHEN send THEN only matched pairs submitted`() = runTest {
            // Arrange
            givenTransactions(tx("tx-1"), tx("tx-2"))
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns Either.Right(listOf("h1"))

            // Act
            createRealSender().send(Recorder().callbacks)

            // Assert
            coVerify(exactly = 1) { submitHashUseCase(any()) }
            coVerify(exactly = 1) {
                submitHashUseCase(SubmitHashData(transactionHash = "h1", transactionId = "tx-1"))
            }
            verify { getExplorerTransactionUrlUseCase(txHash = "h1", currency = any()) }
            verify(exactly = 1) { balanceUpdater.updateAfterTransaction() }
            coVerify(exactly = 0) { saveUnsubmittedHashUseCase(any(), any()) }
        }

        // Pins current behaviour: hashes beyond the number of transactions are ignored, the explorer link uses the last one
        @Test
        fun `GIVEN more hashes than transactions WHEN send THEN extra hashes ignored`() = runTest {
            // Arrange
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns Either.Right(listOf("h1", "h2"))
            val recorder = Recorder()

            // Act
            createRealSender().send(recorder.callbacks)

            // Assert
            coVerify(exactly = 1) { submitHashUseCase(any()) }
            coVerify(exactly = 1) {
                submitHashUseCase(SubmitHashData(transactionHash = "h1", transactionId = "tx-1"))
            }
            verify { getExplorerTransactionUrlUseCase(txHash = "h2", currency = any()) }
            verify(exactly = 1) { balanceUpdater.updateAfterTransaction() }
            assertThat(recorder.successUrls).containsExactly("url")
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN first sorted transaction WHEN send THEN send mode chosen by network and type`(
            model: SendModeModel,
        ) = runTest {
            // Arrange
            givenTransactions(*model.txs.toTypedArray())
            coEvery {
                sendTransactionUseCase(txsData = any(), userWallet = any(), network = any(), sendMode = any())
            } returns Either.Right(model.txs.map { "h-${it.id}" })

            // Act
            createRealSender().send(Recorder().callbacks)

            // Assert
            coVerify { sendTransactionUseCase(
                txsData = any(),
                userWallet = any(),
                network = any(),
                sendMode = model.expected,
            ) }
        }

        private fun provideTestModels() = listOf(
            SendModeModel(
                txs = listOf(
                    tx(id = "s", network = NetworkType.SOLANA, type = StakingTransactionType.SPLIT, stepIndex = 0),
                    tx(id = "t", network = NetworkType.SOLANA, type = StakingTransactionType.STAKE, stepIndex = 1),
                ),
                expected = TransactionSender.MultipleTransactionSendMode.WAIT_AFTER_FIRST,
            ),
            SendModeModel(
                txs = listOf(tx("t", NetworkType.SOLANA, StakingTransactionType.STAKE)),
                expected = TransactionSender.MultipleTransactionSendMode.DEFAULT,
            ),
            SendModeModel(
                txs = listOf(
                    tx(id = "t", network = NetworkType.SOLANA, type = StakingTransactionType.STAKE, stepIndex = 0),
                    tx(id = "s", network = NetworkType.SOLANA, type = StakingTransactionType.SPLIT, stepIndex = 1),
                ),
                expected = TransactionSender.MultipleTransactionSendMode.DEFAULT,
            ),
            SendModeModel(
                txs = listOf(tx("s", NetworkType.TRON, StakingTransactionType.SPLIT)),
                expected = TransactionSender.MultipleTransactionSendMode.DEFAULT,
            ),
        )
    }

    private fun createRealSender(status: CryptoCurrencyStatus = status(), isAmountSubtractAvailable: Boolean = false) =
        StakeKitTransactionSender(
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
            cryptoCurrencyStatusProvider = Provider { status },
            userWallet = realUserWallet,
            integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
            isAmountSubtractAvailable = isAmountSubtractAvailable,
        )

    private fun stubDefaults() {
        coEvery {
            checkStakingTransactionUseCase(
                network = any(),
                accountAddress = any(),
                unsignedTransaction = any(),
                token = any(),
                blockchain = any(),
                provider = any(),
            )
        } returns StakingTransactionVerdict.SAFE
        every { isFeeApproximateUseCase(any(), any()) } returns false
        givenTransactions(tx("tx-1"))
        every { stateController.value } returns uiState()
    }

    private fun tx(
        id: String,
        network: NetworkType = NetworkType.SOLANA,
        type: StakingTransactionType = StakingTransactionType.STAKE,
        status: StakingTransactionStatus = StakingTransactionStatus.CREATED,
        stepIndex: Int = 0,
        gas: BigDecimal? = null,
    ) = StakingTransaction(
        id = id,
        network = network,
        status = status,
        type = type,
        hash = null,
        signedTransaction = null,
        unsignedTransaction = "unsigned-$id",
        stepIndex = stepIndex,
        error = null,
        gasEstimate = gas?.let { StakingGasEstimate(it, MockStaking.solToken, null) },
        stakeId = null,
        explorerUrl = null,
        ledgerHwAppId = null,
        isMessage = false,
    )

    private fun compiled(id: String) = TransactionData.Compiled(
        value = TransactionData.Compiled.Data.RawString("compiled-$id"),
    )

    private fun givenTransactions(vararg txs: StakingTransaction) {
        coEvery { getStakingTransactionsUseCase(any(), any(), any()) } returns Either.Right(txs.toList())
        givenConstructed(*txs)
    }

    private fun givenConstructed(vararg txs: StakingTransaction) {
        txs.forEach { tx ->
            coEvery {
                getConstructedStakingTransactionUseCase(networkId = any(), fee = any(), amount = any(), transactionId = tx.id)
            } returns Either.Right(tx to compiled(tx.id))
        }
    }

    private fun givenVerdictForUnsigned(unsignedTransaction: String, verdict: StakingTransactionVerdict) {
        coEvery {
            checkStakingTransactionUseCase(network = any(), accountAddress = any(), unsignedTransaction = unsignedTransaction, token = any(), blockchain = any(), provider = any())
        } returns verdict
    }

    private class Recorder {
        val constructErrors = mutableListOf<StakingError>()
        val sendErrors = mutableListOf<SendTransactionError>()
        val successUrls = mutableListOf<String>()
        val feeIncreased = mutableListOf<Pair<Fee, Boolean>>()
        val constructed = mutableListOf<List<StakingTransaction>>()

        val callbacks = StakingTransactionSender.Callbacks(
            onConstructSuccess = { constructed += it },
            onConstructError = { constructErrors += it },
            onSendSuccess = { successUrls += it },
            onSendError = { sendErrors += it },
            onFeeIncreased = { fee, isApproximate -> feeIncreased += fee to isApproximate },
            onTransactionExpired = {},
        )
    }

    internal data class SubtractModel(
        val actionType: StakingActionCommonType,
        val isSubtractAvailable: Boolean,
        val expected: BigDecimal,
    )

    internal data class SendModeModel(
        val txs: List<StakingTransaction>,
        val expected: TransactionSender.MultipleTransactionSendMode,
    )

    private fun provideTestModels() = listOf(
        ReuseCase(
            label = "deterministic chain, unchanged inputs → reused (built+scanned once)",
            network = NetworkType.POLYGON,
            expectedBuilds = 1,
            expectedScans = 1,
        ),
        ReuseCase(
            label = "solana, unchanged inputs → reused (blockhash is refreshed by the SDK before signing)",
            network = NetworkType.SOLANA,
            expectedBuilds = 1,
            expectedScans = 1,
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
            sendState = StateArgs(pendingAction = MockStaking.pendingAction(passthrough = "p1")),
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
            label = "only feeAmount changed → rebuilt (key regression guard)",
            network = NetworkType.POLYGON,
            sendState = StateArgs(feeValue = BigDecimal("2")),
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "only validatorAddress changed → rebuilt (key regression guard)",
            network = NetworkType.POLYGON,
            sendState = StateArgs(validatorAddress = MockStaking.SECOND_VALIDATOR_ADDRESS),
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "only actionType changed → rebuilt (key regression guard)",
            network = NetworkType.POLYGON,
            sendState = StateArgs(actionType = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false)),
            expectedBuilds = 2,
            expectedScans = 2,
        ),
        ReuseCase(
            label = "only accountAddress changed → rebuilt (key regression guard)",
            network = NetworkType.POLYGON,
            sendState = StateArgs(accountAddress = FRESH_ADDRESS),
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
            label = "unsafe on solana rebuilt after amount change → blocked, not sent",
            network = NetworkType.SOLANA,
            sendState = StateArgs(cryptoAmount = BigDecimal("11")),
            verdict = StakingTransactionVerdict.UNSAFE,
            expectedBuilds = 2,
            expectedScans = 2,
            blocked = true,
        ),
    )

    private fun currencyFor(network: NetworkType): CryptoCurrency = when (network) {
        NetworkType.SOLANA -> solanaCoin
        NetworkType.POLYGON -> polygonToken
        else -> error("No currency fixture for $network")
    }

    private fun integrationFor(network: NetworkType): StakingIntegrationID.StakeKit = when (network) {
        NetworkType.SOLANA -> StakingIntegrationID.StakeKit.Coin.Solana
        NetworkType.POLYGON -> StakingIntegrationID.StakeKit.EthereumToken.Polygon
        else -> error("No integration fixture for $network")
    }

    private fun createSender(network: NetworkType): StakeKitTransactionSender {
        currentStatus = status(currency = currencyFor(network), address = ACCOUNT_ADDRESS)
        return StakeKitTransactionSender(
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
            cryptoCurrencyStatusProvider = Provider { currentStatus },
            userWallet = realUserWallet,
            integration = stakeKitIntegration(integrationFor(network)),
            isAmountSubtractAvailable = false,
        )
    }

    /**
     * Every build returns a distinct pair — `unsigned-build-N` is what the security check sees and
     * `compiled-build-N` is what gets signed — so a test can prove that the sent build is the scanned one.
     */
    private fun givenBuild(network: NetworkType, verdict: StakingTransactionVerdict) {
        coEvery {
            getStakingTransactionsUseCase(any(), any(), any())
        } returns Either.Right(listOf(tx("tx-1", network = network)))
        coEvery {
            getConstructedStakingTransactionUseCase(any(), any(), any(), any())
        } returnsMany (1..MAX_BUILDS).map { n ->
            val build = tx("build-$n", network = network)
            Either.Right(build to compiled(build.id))
        }
        coEvery {
            checkStakingTransactionUseCase(any(), any(), any(), any(), any(), any())
        } returns verdict
    }

    private fun stubState(args: StateArgs, network: NetworkType) {
        currentStatus = status(currency = currencyFor(network), address = args.accountAddress)
        every { stateController.value } returns uiState(
            confirmationState = confirmationData(
                fee = fee(args.feeValue),
                pendingAction = args.pendingAction,
                reduceAmountBy = args.reduceAmountBy,
            ),
            amountState = amountData(args.cryptoAmount),
            validatorState = validatorData(MockStaking.validator(address = args.validatorAddress).toStakingTarget()),
            actionType = args.actionType,
        )
    }

    /** Build inputs that determine the unsigned transaction; every field defaults so a case overrides only what it tests. */
    internal data class StateArgs(
        val cryptoAmount: BigDecimal = BigDecimal.TEN,
        val feeValue: BigDecimal = BigDecimal.ONE,
        val validatorAddress: String = MockStaking.VALIDATOR_ADDRESS,
        val actionType: StakingActionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
        val pendingAction: PendingAction? = null,
        val reduceAmountBy: BigDecimal? = null,
        val accountAddress: String = ACCOUNT_ADDRESS,
    )

    internal data class ReuseCase(
        val label: String,
        val network: NetworkType,
        val validateState: StateArgs = StateArgs(),
        val sendState: StateArgs = StateArgs(),
        val verdict: StakingTransactionVerdict = StakingTransactionVerdict.SAFE,
        val expectedBuilds: Int,
        val expectedScans: Int,
        val blocked: Boolean = false,
    ) {
        override fun toString(): String = label
    }

    private companion object {
        const val ACCOUNT_ADDRESS = "ACCOUNT_ADDRESS"
        const val FRESH_ADDRESS = "FRESH_ADDRESS"
        const val MAX_BUILDS = 3
    }
}