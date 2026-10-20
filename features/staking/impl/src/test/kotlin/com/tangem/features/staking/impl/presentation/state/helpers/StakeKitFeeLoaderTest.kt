package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.EstimateGasUseCase
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.staking.action.StakingActionType
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.Provider
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.math.BigDecimal

/**
 * Covers [StakeKitFeeLoader.getFee]: state preconditions, single and Solana composite gas estimation, the
 * approval-fee branch, and the source-address handling (CRASHAND-53) — the loader reads the
 * [CryptoCurrencyStatus] through a [Provider] on every call, so a transiently address-less status only
 * fails the current fee request instead of freezing the failure for the whole screen lifetime.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class StakeKitFeeLoaderTest {

    private val stateController: StakingStateController = mockk()
    private val getFeeUseCase: GetFeeUseCase = mockk()
    private val estimateGasUseCase: EstimateGasUseCase = mockk()
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase = mockk()
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(
            stateController,
            getFeeUseCase,
            estimateGasUseCase,
            isFeeApproximateUseCase,
            createApprovalTransactionUseCase,
        )
        every { isFeeApproximateUseCase(any(), any()) } returns false
        every { stateController.value } returns uiState()
    }

    @Test
    fun `GIVEN status without address WHEN getFee THEN no available address error and no estimation`() = runTest {
        // Arrange
        val loader = createLoader(statusProvider = Provider { status(address = null) })
        val captured = Captured()

        // Act
        loader.load(captured)

        // Assert
        assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("No available address"))
        assertThat(captured.fees).isEmpty()
        coVerify(exactly = 0) { estimateGasUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN status recovers address after failed fee WHEN getFee again THEN fee estimated with fresh address`() =
        runTest {
            // Arrange
            val paramsSlot = stubEstimate()
            var currentStatus = status(address = null)
            val loader = createLoader(statusProvider = Provider { currentStatus })
            val captured = Captured()

            // Act
            loader.load(captured)
            currentStatus = status(address = FRESH_ADDRESS)
            loader.load(captured)

            // Assert
            assertThat(paramsSlot.captured.address).isEqualTo(FRESH_ADDRESS)
            assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("No available address"))
            assertThat(captured.fees).containsExactly(fee(BigDecimal.ONE, "SOL", 9) to false)
        }

    @Test
    fun `GIVEN status with address WHEN getFee THEN fee estimated with that address`() = runTest {
        // Arrange
        val paramsSlot = stubEstimate()
        val loader = createLoader(statusProvider = Provider { status(address = SOURCE_ADDRESS) })
        val captured = Captured()

        // Act
        loader.load(captured)

        // Assert
        assertThat(paramsSlot.captured.address).isEqualTo(SOURCE_ADDRESS)
        assertThat(captured.fees).containsExactly(fee(BigDecimal.ONE, "SOL", 9) to false)
        assertThat(captured.stakingErrors).isEmpty()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Preconditions {

        @Test
        fun `GIVEN confirmation state empty WHEN getFee THEN illegal state error`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(confirmationState = StakingStates.ConfirmationState.Empty())
            stubEstimate()
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("Illegal state"))
            assertThat(captured.fees).isEmpty()
            coVerify(exactly = 0) { estimateGasUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN no chosen validator and no balance target WHEN getFee THEN no target address error`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                validatorState = StakingStates.ValidatorState.Empty(),
                balanceState = null,
            )
            stubEstimate()
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("No target address provided"))
        }

        @Test
        fun `GIVEN no chosen validator but balance target WHEN getFee THEN balance target used as validator address`() =
            runTest {
                // Arrange
                every { stateController.value } returns uiState(
                    validatorState = StakingStates.ValidatorState.Empty(),
                    balanceState = balanceState("balance-target"),
                )
                val paramsSlot = stubEstimate()
                val captured = Captured()

                // Act
                solanaLoader().load(captured)

                // Assert
                assertThat(paramsSlot.captured.validatorAddress).isEqualTo("balance-target")
                assertThat(captured.fees).hasSize(1)
            }

        @Test
        fun `GIVEN amount state empty WHEN getFee THEN no amount error`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(amountState = AmountState.Empty)
            stubEstimate()
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("No amount provided"))
        }

        @Test
        fun `GIVEN amount value null WHEN getFee THEN no amount error`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(amountState = amountData(null))
            stubEstimate()
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("No amount provided"))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SingleEstimate {

        @Test
        fun `GIVEN solana enter state WHEN getFee THEN estimate requested with full action params`() = runTest {
            // Arrange
            every { stateController.value } returns uiState()
            val paramsSlot = stubEstimate()

            // Act
            solanaLoader().load(Captured())

            // Assert
            val expected = ActionParams(
                actionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
                integrationId = MockStaking.SOLANA_ID,
                amount = BigDecimal.TEN,
                address = SOURCE_ADDRESS,
                validatorAddress = MockStaking.VALIDATOR_ADDRESS,
                token = MockStaking.solToken,
                passthrough = null,
                type = null,
            )
            assertThat(paramsSlot.captured).isEqualTo(expected)
            coVerify {
                estimateGasUseCase(
                    userWalletId = realUserWallet.walletId,
                    network = solanaCoin.network,
                    params = any(),
                )
            }
        }

        @Test
        fun `GIVEN fee approximate for network WHEN getFee THEN approximate flag true`() = runTest {
            // Arrange
            every { stateController.value } returns uiState()
            every { isFeeApproximateUseCase(any(), any()) } returns true
            stubEstimate()
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.fees.single().second).isTrue()
            verify { isFeeApproximateUseCase(networkId = solanaCoin.network.id, amountType = AmountType.Coin) }
        }

        @Test
        fun `GIVEN pending action WHEN getFee THEN passthrough and type forwarded`() = runTest {
            // Arrange
            val pendingAction = MockStaking.pendingAction(
                type = StakingActionType.CLAIM_REWARDS,
                passthrough = "passthrough-1",
            )
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(pendingAction = pendingAction),
                actionType = StakingActionCommonType.Pending.Rewards,
            )
            val paramsSlot = stubEstimate()

            // Act
            solanaLoader().load(Captured())

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

        @Test
        fun `GIVEN estimate left WHEN getFee THEN staking fee error forwarded and no fee`() = runTest {
            // Arrange
            every { stateController.value } returns uiState()
            coEvery {
                estimateGasUseCase(userWalletId = any(), network = any(), params = any())
            } returns Either.Left(StakingError.DomainError("estimate failed"))
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("estimate failed"))
            assertThat(captured.fees).isEmpty()
            coVerify(exactly = 1) { estimateGasUseCase(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Approval {

        private val uncompiledTx: TransactionData.Uncompiled = mockk()

        @BeforeEach
        fun stubApproval() {
            every { stateController.value } returns uiState(confirmationState = approvalNeededData())
            coEvery {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = any(),
                    contractAddress = any(),
                    spenderAddress = any(),)
            } returns Either.Right(uncompiledTx)
            coEvery {
                getFeeUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    spenderAddress = any(),
                    isSimulateEstimation = any(),)
            } returns Either.Right(TransactionFee.Single(fee(BigDecimal("0.0005"), "ETH", 18)))
        }

        @Test
        fun `GIVEN approval needed for coin currency WHEN getFee THEN unknown fee error and no approval created`() =
            runTest {
                // Arrange
                val loader = polygonLoader(currency = ethereumCoin)
                val captured = Captured()

                // Act
                loader.load(captured)

                // Assert
                assertThat(captured.feeErrors).containsExactly(GetFeeError.UnknownError)
                coVerify(
                    exactly = 0,
                ) { createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = any(),
                    contractAddress = any(),
                    spenderAddress = any(),
                ) }
                coVerify(exactly = 0) { estimateGasUseCase(any(), any(), any()) }
            }

        @Test
        fun `GIVEN create approval left WHEN getFee THEN data error with cause`() = runTest {
            // Arrange
            val cause = IllegalStateException("boom")
            coEvery {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = any(),
                    userWalletId = any(),
                    amount = any(),
                    contractAddress = any(),
                    spenderAddress = any(),)
            } returns Either.Left(cause)
            val captured = Captured()

            // Act
            polygonLoader().load(captured)

            // Assert
            assertThat(captured.feeErrors).containsExactly(GetFeeError.DataError(cause))
            coVerify(
                exactly = 0,
            ) { getFeeUseCase(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                spenderAddress = any(),
                isSimulateEstimation = any(),
            ) }
        }

        @Test
        fun `GIVEN approval fee left WHEN getFee THEN fee error forwarded`() = runTest {
            // Arrange
            coEvery {
                getFeeUseCase(
                    userWallet = any(),
                    network = any(),
                    transactionData = any(),
                    spenderAddress = any(),
                    isSimulateEstimation = any(),)
            } returns Either.Left(GetFeeError.UnknownError)
            val captured = Captured()

            // Act
            polygonLoader().load(captured)

            // Assert
            assertThat(captured.feeErrors).containsExactly(GetFeeError.UnknownError)
            assertThat(captured.approvalFees).isEmpty()
        }

        @Test
        fun `GIVEN approval fee right WHEN getFee THEN approval fee forwarded with token spender data`() = runTest {
            // Arrange
            val captured = Captured()

            // Act
            polygonLoader().load(captured)

            // Assert
            assertThat(captured.approvalFees)
                .containsExactly(TransactionFee.Single(fee(BigDecimal("0.0005"), "ETH", 18)))
            coVerify {
                createApprovalTransactionUseCase(
                    cryptoCurrencyStatus = status(currency = polygonToken),
                    userWalletId = realUserWallet.walletId,
                    amount = BigDecimal.TEN,
                    contractAddress = polygonToken.contractAddress,
                    spenderAddress = MockStaking.VALIDATOR_ADDRESS,
                )
            }
            coVerify {
                getFeeUseCase(
                    userWallet = realUserWallet,
                    network = polygonToken.network,
                    transactionData = uncompiledTx,
                    spenderAddress = null,
                    isSimulateEstimation = false,
                )
            }
            coVerify(exactly = 0) { estimateGasUseCase(any(), any(), any()) }
        }

        @Test
        fun `GIVEN allowance covers amount WHEN getFee THEN gas estimated instead of approval`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                confirmationState = approvalNeededData(allowance = BigDecimal.TEN),
            )
            stubEstimate(StakingGasEstimate(BigDecimal("0.002"), MockStaking.polToken, null))
            val captured = Captured()

            // Act
            polygonLoader().load(captured)

            // Assert
            coVerify(
                exactly = 0,
            ) { createApprovalTransactionUseCase(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            ) }
            assertThat(captured.fees).containsExactly(fee(BigDecimal("0.002"), "POL", 18) to false)
        }

        @Test
        fun `GIVEN approval needed but exit action WHEN getFee THEN gas estimated instead of approval`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                confirmationState = approvalNeededData(),
                actionType = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false),
            )
            stubEstimate(StakingGasEstimate(BigDecimal("0.002"), MockStaking.polToken, null))

            // Act
            polygonLoader().load(Captured())

            // Assert
            coVerify(
                exactly = 0,
            ) { createApprovalTransactionUseCase(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            ) }
            coVerify(exactly = 1) { estimateGasUseCase(any(), any(), any()) }
        }

        private fun approvalNeededData(allowance: BigDecimal = BigDecimal.ONE) =
            confirmationData(isApprovalNeeded = true, allowance = allowance)

        private fun polygonLoader(currency: CryptoCurrency = polygonToken) = createLoader(
            statusProvider = Provider { status(currency = currency) },
            integration = stakeKitIntegration(StakingIntegrationID.StakeKit.EthereumToken.Polygon),
            userWallet = realUserWallet,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SolanaComposite {

        private val withdraw1 = MockStaking.pendingAction(type = StakingActionType.WITHDRAW, passthrough = "pt-1")
        private val withdraw2 = MockStaking.pendingAction(type = StakingActionType.WITHDRAW, passthrough = "pt-2")

        @Test
        fun `GIVEN two withdraw actions WHEN getFee THEN fee is sum of both estimates`() = runTest {
            // Arrange
            givenCompositeState(withdraw1, withdraw2)
            stubEstimate("pt-1", estimate("0.001"))
            stubEstimate("pt-2", estimate("0.002"))
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.fees).containsExactly(fee(BigDecimal("0.003"), "SOL", 9) to false)
            assertThat(captured.stakingErrors).isEmpty()
            coVerify(exactly = 2) {
                estimateGasUseCase(any(), any(), match { it.type == StakingActionType.WITHDRAW })
            }
        }

        @Test
        fun `GIVEN estimate without gas limit WHEN getFee for two withdraw actions THEN fee is still the sum`() =
            runTest {
                // Arrange
                givenCompositeState(withdraw1, withdraw2)
                stubEstimate("pt-1", estimate("0.001"))
                stubEstimate(
                    "pt-2",
                    Either.Right(StakingGasEstimate(BigDecimal("0.002"), MockStaking.solToken, gasLimit = null)),
                )
                val captured = Captured()

                // Act
                solanaLoader().load(captured)

                // Assert
                assertThat(captured.fees).containsExactly(fee(BigDecimal("0.003"), "SOL", 9) to false)
                assertThat(captured.stakingErrors).isEmpty()
            }

        @Test
        fun `GIVEN zero estimate twice WHEN getFee THEN retried until non zero after two delays`() = runTest {
            // Arrange
            givenCompositeState(withdraw1)
            stubEstimate("pt-1", estimate("0"), estimate("0"), estimate("0.001"))
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            coVerify(exactly = 3) { estimateGasUseCase(any(), any(), any()) }
            assertThat(testScheduler.currentTime).isEqualTo(2000L)
            assertThat(captured.fees.single().first).isEqualTo(fee(BigDecimal("0.001"), "SOL", 9))
        }

        // Pins current behaviour: a fee that is still zero after the last retry is accepted as is
        @Test
        fun `GIVEN zero estimate three times WHEN getFee THEN zero fee accepted after three tries`() = runTest {
            // Arrange
            givenCompositeState(withdraw1)
            stubEstimate("pt-1", estimate("0"), estimate("0"), estimate("0"))
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            coVerify(exactly = 3) { estimateGasUseCase(any(), any(), any()) }
            assertThat(testScheduler.currentTime).isEqualTo(2000L)
            assertThat(captured.fees).containsExactly(fee(BigDecimal("0"), "SOL", 9) to false)
            assertThat(captured.stakingErrors).isEmpty()
        }

        @Test
        fun `GIVEN left on first try WHEN getFee THEN no retry and error estimating fee`() = runTest {
            // Arrange
            givenCompositeState(withdraw1)
            stubEstimate("pt-1", Either.Left(StakingError.DomainError("boom")))
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            coVerify(exactly = 1) { estimateGasUseCase(any(), any(), any()) }
            assertThat(testScheduler.currentTime).isEqualTo(0L)
            assertThat(captured.stakingErrors)
                .containsExactly(StakingError.DomainError("boom"), StakingError.DomainError("Error estimating fee"))
                .inOrder()
            assertThat(captured.fees).isEmpty()
        }

        // Pins current behaviour: a failed action is reported but the fee of the remaining actions is still emitted
        @Test
        fun `GIVEN one of two actions fails WHEN getFee THEN error reported and fee of remaining action emitted`() =
            runTest {
                // Arrange
                givenCompositeState(withdraw1, withdraw2)
                stubEstimate("pt-1", Either.Left(StakingError.DomainError("boom")))
                stubEstimate("pt-2", estimate("0.002"))
                val captured = Captured()

                // Act
                solanaLoader().load(captured)

                // Assert
                assertThat(captured.stakingErrors).containsExactly(StakingError.DomainError("boom"))
                assertThat(captured.fees).containsExactly(fee(BigDecimal("0.002"), "SOL", 9) to false)
            }

        @Test
        fun `GIVEN all actions fail WHEN getFee THEN each error plus error estimating fee`() = runTest {
            // Arrange
            givenCompositeState(withdraw1, withdraw2)
            stubEstimate("pt-1", Either.Left(StakingError.DomainError("e1")))
            stubEstimate("pt-2", Either.Left(StakingError.DomainError("e2")))
            val captured = Captured()

            // Act
            solanaLoader().load(captured)

            // Assert
            assertThat(captured.stakingErrors).containsExactly(
                StakingError.DomainError("e1"),
                StakingError.DomainError("e2"),
                StakingError.DomainError("Error estimating fee"),
            )
            assertThat(captured.fees).isEmpty()
        }

        @Test
        fun `GIVEN solana pending actions without withdraw WHEN getFee THEN single estimate with confirmation pending action`() =
            runTest {
                // Arrange
                every { stateController.value } returns uiState(
                    confirmationState = confirmationData(
                        pendingAction = null,
                        pendingActions = persistentListOf(
                            MockStaking.pendingAction(StakingActionType.CLAIM_REWARDS, "pt-c"),
                        ),
                    ),
                    actionType = StakingActionCommonType.Pending.Other,
                )
                val paramsSlot = stubEstimate()

                // Act
                solanaLoader().load(Captured())

                // Assert
                coVerify(exactly = 1) { estimateGasUseCase(any(), any(), any()) }
                assertThat(paramsSlot.captured.passthrough).isNull()
            }

        @Test
        fun `GIVEN tron with withdraw actions WHEN getFee THEN not composite and single estimate`() = runTest {
            // Arrange
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(
                    pendingAction = withdraw1,
                    pendingActions = persistentListOf(withdraw1, withdraw2),
                ),
                actionType = StakingActionCommonType.Pending.Other,
            )
            val paramsSlot = stubEstimate(StakingGasEstimate(BigDecimal("1"), MockStaking.trxToken, null))
            val loader = createLoader(
                statusProvider = Provider { status(currency = tronCoin) },
                integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Tron),
                userWallet = realUserWallet,
            )
            val captured = Captured()

            // Act
            loader.load(captured)

            // Assert
            coVerify(exactly = 1) { estimateGasUseCase(any(), any(), any()) }
            assertThat(paramsSlot.captured.passthrough).isEqualTo("pt-1")
            assertThat(captured.fees.single().first).isEqualTo(fee(BigDecimal("1"), "TRX", 6))
        }

        private fun givenCompositeState(vararg actions: PendingAction) {
            every { stateController.value } returns uiState(
                confirmationState = confirmationData(pendingActions = persistentListOf(*actions)),
                actionType = StakingActionCommonType.Pending.Other,
            )
        }

        private fun estimate(amount: String) = Either.Right(
            StakingGasEstimate(BigDecimal(amount), MockStaking.solToken, gasLimit = "5000"),
        )

        private fun stubEstimate(passthrough: String, vararg results: Either<StakingError, StakingGasEstimate>) {
            coEvery {
                estimateGasUseCase(any(), any(), match { it.passthrough == passthrough })
            } returnsMany results.toList()
        }
    }

    private class Captured {
        val fees = mutableListOf<Pair<Fee, Boolean>>()
        val stakingErrors = mutableListOf<StakingError>()
        val approvalFees = mutableListOf<TransactionFee>()
        val feeErrors = mutableListOf<GetFeeError>()
    }

    private suspend fun StakeKitFeeLoader.load(captured: Captured) {
        getFee(
            onStakingFee = { fee, isApproximate -> captured.fees += fee to isApproximate },
            onStakingFeeError = { captured.stakingErrors += it },
            onApprovalFee = { captured.approvalFees += it },
            onFeeError = { captured.feeErrors += it },
        )
    }

    private fun solanaLoader() = createLoader(
        statusProvider = Provider { status() },
        integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
        userWallet = realUserWallet,
    )

    private fun stubEstimate(
        estimate: StakingGasEstimate = StakingGasEstimate(BigDecimal.ONE, MockStaking.solToken, gasLimit = null),
    ): CapturingSlot<ActionParams> {
        val paramsSlot = slot<ActionParams>()
        coEvery {
            estimateGasUseCase(userWalletId = any(), network = any(), params = capture(paramsSlot))
        } returns Either.Right(estimate)
        return paramsSlot
    }

    private fun createLoader(
        statusProvider: Provider<CryptoCurrencyStatus>,
        integration: StakeKitIntegration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana),
        userWallet: UserWallet = realUserWallet,
    ): StakeKitFeeLoader = StakeKitFeeLoader(
        stateController = stateController,
        getFeeUseCase = getFeeUseCase,
        estimateGasUseCase = estimateGasUseCase,
        isFeeApproximateUseCase = isFeeApproximateUseCase,
        createApprovalTransactionUseCase = createApprovalTransactionUseCase,
        cryptoCurrencyStatusProvider = statusProvider,
        userWallet = userWallet,
        integration = integration,
    )

    private companion object {
        const val FRESH_ADDRESS = "FRESH_ADDRESS"
    }
}