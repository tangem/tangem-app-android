package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.toStakingTarget
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.features.staking.impl.presentation.state.BalanceState
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.staking.MockStaking
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class P2PEthPoolTransactionCreatorTest {

    private val stateController: StakingStateController = mockk()
    private val repository: P2PEthPoolRepository = mockk()

    private val creator = P2PEthPoolTransactionCreator(
        stateController = stateController,
        p2pEthPoolRepository = repository,
    )

    private val ethStatus = status(currency = ethereumCoin, address = SOURCE_ADDRESS)

    @BeforeEach
    fun setUp() {
        clearMocks(stateController, repository)
        coEvery { repository.createDepositTransaction(network = any(), delegatorAddress = any(), vaultAddress = any(), amount = any()) } returns unsignedTx().right()
        coEvery { repository.createUnstakeTransaction(network = any(), delegatorAddress = any(), vaultAddress = any(), amount = any()) } returns unsignedTx().right()
        coEvery { repository.createWithdrawTransaction(network = any(), delegatorAddress = any(), vaultAddress = any(), amount = any()) } returns unsignedTx().right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExtractParams {

        @Test
        fun `GIVEN full state WHEN extractParams THEN params from state and status`() {
            // Arrange
            givenState(actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false))

            // Act
            val actual = creator.extractParams(ethStatus)

            // Assert
            val expected = P2PEthPoolTransactionCreator.TransactionParams(
                actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
                amount = BigDecimal("1.50"),
                vaultAddress = MockStaking.VAULT_ADDRESS,
                sourceAddress = SOURCE_ADDRESS,
            )
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN confirmation state empty WHEN extractParams THEN null`() {
            // Arrange
            givenState(confirmationState = StakingStates.ConfirmationState.Empty())

            // Act
            val actual = creator.extractParams(ethStatus)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN status without address WHEN extractParams THEN null`() {
            // Arrange
            givenState()

            // Act
            val actual = creator.extractParams(status(currency = ethereumCoin, address = null))

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN no chosen vault but balance target WHEN extractParams THEN balance target used as vault`() {
            // Arrange
            givenState(
                validatorState = StakingStates.ValidatorState.Empty(),
                balanceState = balanceState("0xbalancevault"),
            )

            // Act
            val actual = creator.extractParams(ethStatus)

            // Assert
            assertThat(actual?.vaultAddress).isEqualTo("0xbalancevault")
        }

        @Test
        fun `GIVEN no chosen vault and no balance target WHEN extractParams THEN null`() {
            // Arrange
            givenState(validatorState = StakingStates.ValidatorState.Empty(), balanceState = null)

            // Act
            val actual = creator.extractParams(ethStatus)

            // Assert
            assertThat(actual).isNull()
        }

        @Test
        fun `GIVEN amount state empty WHEN extractParams THEN null`() {
            // Arrange
            givenState(amountState = AmountState.Empty)

            // Act
            val actual = creator.extractParams(ethStatus)

            // Assert
            assertThat(actual).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateTransaction {

        @Test
        fun `GIVEN enter action WHEN createTransaction THEN deposit created on mainnet with plain amount`() = runTest {
            // Arrange
            givenState(actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false))

            // Act
            val actual = creator.createTransaction(ethStatus)

            // Assert
            assertThat(actual).isEqualTo(unsignedTx().right())
            coVerify(exactly = 1) {
                repository.createDepositTransaction(
                    network = P2PEthPoolNetwork.MAINNET,
                    delegatorAddress = SOURCE_ADDRESS,
                    vaultAddress = MockStaking.VAULT_ADDRESS,
                    amount = "1.50",
                )
            }
            coVerify(
                exactly = 0,
            ) { repository.createUnstakeTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
            coVerify(
                exactly = 0,
            ) { repository.createWithdrawTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
        }

        @Test
        fun `GIVEN exit action WHEN createTransaction THEN unstake created`() = runTest {
            // Arrange
            givenState(actionType = StakingActionCommonType.Exit(isPartiallyUnstakeDisabled = false))

            // Act
            creator.createTransaction(ethStatus)

            // Assert
            coVerify(exactly = 1) {
                repository.createUnstakeTransaction(
                    network = P2PEthPoolNetwork.MAINNET,
                    delegatorAddress = SOURCE_ADDRESS,
                    vaultAddress = MockStaking.VAULT_ADDRESS,
                    amount = "1.50",
                )
            }
            coVerify(
                exactly = 0,
            ) { repository.createDepositTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
            coVerify(
                exactly = 0,
            ) { repository.createWithdrawTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
        }

        // Pins current behaviour: every pending action type, including restake and rewards, creates a withdraw
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN any pending action WHEN createTransaction THEN withdraw created`(model: PendingModel) = runTest {
            // Arrange
            givenState(actionType = model.actionType)

            // Act
            creator.createTransaction(ethStatus)

            // Assert
            coVerify(exactly = 1) {
                repository.createWithdrawTransaction(
                    network = P2PEthPoolNetwork.MAINNET,
                    delegatorAddress = SOURCE_ADDRESS,
                    vaultAddress = MockStaking.VAULT_ADDRESS,
                    amount = "1.50",
                )
            }
            coVerify(
                exactly = 0,
            ) { repository.createDepositTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
            coVerify(
                exactly = 0,
            ) { repository.createUnstakeTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
        }

        @Test
        fun `GIVEN exponent amount WHEN createTransaction THEN amount sent as plain string`() = runTest {
            // Arrange
            givenState(
                amountState = amountData(BigDecimal("1E-7")),
                actionType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
            )

            // Act
            creator.createTransaction(ethStatus)

            // Assert
            coVerify { repository.createDepositTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = "0.0000001",
            ) }
        }

        @Test
        fun `GIVEN invalid state WHEN createTransaction THEN domain error and no repository call`() = runTest {
            // Arrange
            givenState(confirmationState = StakingStates.ConfirmationState.Empty())

            // Act
            val actual = creator.createTransaction(ethStatus)

            // Assert
            assertThat(actual)
                .isEqualTo(StakingError.DomainError("Invalid state for transaction creation").left())
            coVerify(
                exactly = 0,
            ) { repository.createDepositTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
            coVerify(
                exactly = 0,
            ) { repository.createUnstakeTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
            coVerify(
                exactly = 0,
            ) { repository.createWithdrawTransaction(
                network = any(),
                delegatorAddress = any(),
                vaultAddress = any(),
                amount = any(),
            ) }
        }

        private fun provideTestModels() = listOf(
            PendingModel(StakingActionCommonType.Pending.Stake(shouldSkipEnterAmount = false)),
            PendingModel(StakingActionCommonType.Pending.Restake),
            PendingModel(StakingActionCommonType.Pending.Rewards),
            PendingModel(StakingActionCommonType.Pending.Other),
        )
    }

    private fun givenState(
        confirmationState: StakingStates.ConfirmationState = confirmationData(),
        amountState: AmountState = amountData(BigDecimal("1.50")),
        validatorState: StakingStates.ValidatorState = validatorData(MockStaking.vault().toStakingTarget()),
        balanceState: BalanceState? = null,
        actionType: StakingActionCommonType = StakingActionCommonType.Enter(shouldSkipEnterAmount = false),
    ) {
        val state: StakingUiState = uiState(
            confirmationState = confirmationState,
            amountState = amountState,
            validatorState = validatorState,
            balanceState = balanceState,
            actionType = actionType,
        )
        every { stateController.value } returns state
    }

    internal data class PendingModel(val actionType: StakingActionCommonType)
}