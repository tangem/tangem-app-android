package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.EstimateGasUseCase
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Covers the source-address handling of [StakeKitFeeLoader.getFee] (CRASHAND-53): the loader reads the
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
    private val integration: StakeKitIntegration = mockk(relaxed = true)
    private val userWallet: UserWallet = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(
            stateController,
            getFeeUseCase,
            estimateGasUseCase,
            isFeeApproximateUseCase,
            createApprovalTransactionUseCase,
            integration,
            userWallet,
        )
        every { isFeeApproximateUseCase(any(), any()) } returns false
        stubState()
    }

    @Test
    fun `GIVEN status without address WHEN getFee THEN fee error and no estimation`() = runTest {
        // Arrange
        val loader = createLoader(statusProvider = Provider { cryptoCurrencyStatus(address = null) })
        var stakingError: StakingError? = null

        // Act
        loader.getFee(
            onStakingFee = { _, _ -> },
            onStakingFeeError = { stakingError = it },
            onApprovalFee = {},
            onFeeError = {},
        )

        // Assert
        assertThat(stakingError).isInstanceOf(StakingError.DomainError::class.java)
        coVerify(exactly = 0) { estimateGasUseCase(any(), any(), any()) }
    }

    @Test
    fun `GIVEN status recovers address after failed fee WHEN getFee again THEN fee estimated with fresh address`() =
        runTest {
            // Arrange
            val paramsSlot = slot<ActionParams>()
            coEvery { estimateGasUseCase(any(), any(), capture(paramsSlot)) } returns Either.Right(gasEstimate())
            var currentStatus = cryptoCurrencyStatus(address = null)
            val loader = createLoader(statusProvider = Provider { currentStatus })
            var fee: Fee? = null

            // Act
            loader.getFee(
                onStakingFee = { _, _ -> },
                onStakingFeeError = {},
                onApprovalFee = {},
                onFeeError = {},
            )
            currentStatus = cryptoCurrencyStatus(address = FRESH_ADDRESS)
            loader.getFee(
                onStakingFee = { stakingFee, _ -> fee = stakingFee },
                onStakingFeeError = {},
                onApprovalFee = {},
                onFeeError = {},
            )

            // Assert
            assertThat(paramsSlot.captured.address).isEqualTo(FRESH_ADDRESS)
            assertThat(fee).isNotNull()
        }

    @Test
    fun `GIVEN status with address WHEN getFee THEN fee estimated with that address`() = runTest {
        // Arrange
        val paramsSlot = slot<ActionParams>()
        coEvery { estimateGasUseCase(any(), any(), capture(paramsSlot)) } returns Either.Right(gasEstimate())
        val loader = createLoader(statusProvider = Provider { cryptoCurrencyStatus(address = SOURCE_ADDRESS) })
        var fee: Fee? = null

        // Act
        loader.getFee(
            onStakingFee = { stakingFee, _ -> fee = stakingFee },
            onStakingFeeError = {},
            onApprovalFee = {},
            onFeeError = {},
        )

        // Assert
        assertThat(paramsSlot.captured.address).isEqualTo(SOURCE_ADDRESS)
        assertThat(fee).isNotNull()
    }

    private fun createLoader(statusProvider: Provider<CryptoCurrencyStatus>): StakeKitFeeLoader = StakeKitFeeLoader(
        stateController = stateController,
        getFeeUseCase = getFeeUseCase,
        estimateGasUseCase = estimateGasUseCase,
        isFeeApproximateUseCase = isFeeApproximateUseCase,
        createApprovalTransactionUseCase = createApprovalTransactionUseCase,
        cryptoCurrencyStatusProvider = statusProvider,
        userWallet = userWallet,
        integration = integration,
    )

    private fun stubState() {
        val feeContent = mockk<FeeState.Content>(relaxed = true)
        val confirmation = mockk<StakingStates.ConfirmationState.Data>(relaxed = true) {
            every { feeState } returns feeContent
            every { isApprovalNeeded } returns false
            every { allowance } returns BigDecimal.ZERO
            every { pendingAction } returns null
            every { pendingActions } returns null
        }
        val validator = mockk<StakingStates.ValidatorState.Data>(relaxed = true) {
            every { chosenTarget.address } returns VALIDATOR_ADDRESS
        }
        val amount = mockk<AmountState.Data>(relaxed = true) {
            every { amountTextField.cryptoAmount.value } returns BigDecimal.TEN
        }
        val state = mockk<StakingUiState>(relaxed = true) {
            every { confirmationState } returns confirmation
            every { amountState } returns amount
            every { validatorState } returns validator
            every { this@mockk.actionType } returns StakingActionCommonType.Enter(skipEnterAmount = false)
        }
        every { stateController.value } returns state
    }

    private fun cryptoCurrencyStatus(address: String?): CryptoCurrencyStatus {
        val statusValue = mockk<CryptoCurrencyStatus.Value>(relaxed = true) {
            if (address == null) {
                every { networkAddress } returns null
            } else {
                every { networkAddress } returns mockk(relaxed = true) {
                    every { defaultAddress } returns mockk(relaxed = true) {
                        every { value } returns address
                    }
                }
            }
        }
        return CryptoCurrencyStatus(
            currency = mockk<CryptoCurrency.Coin>(relaxed = true),
            value = statusValue,
        )
    }

    private fun gasEstimate() = StakingGasEstimate(
        amount = BigDecimal.ONE,
        token = mockk(relaxed = true),
        gasLimit = null,
    )

    private companion object {
        const val SOURCE_ADDRESS = "SOURCE_ADDRESS"
        const val FRESH_ADDRESS = "FRESH_ADDRESS"
        const val VALIDATOR_ADDRESS = "VALIDATOR"
    }
}