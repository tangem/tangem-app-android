package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import java.math.BigDecimal
import javax.inject.Inject

@ModelScoped
internal class P2PEthPoolTransactionCreator @Inject constructor(
    private val stateController: StakingStateController,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
) {

    suspend fun createTransaction(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<StakingError, P2PEthPoolUnsignedTx> {
        val params = extractParams(cryptoCurrencyStatus)
            ?: return Either.Left(StakingError.DomainError("Invalid state for transaction creation"))

        return createTransaction(
            actionType = params.actionType,
            amount = params.amount,
            vaultAddress = params.vaultAddress,
            sourceAddress = params.sourceAddress,
        )
    }

    fun extractParams(cryptoCurrencyStatus: CryptoCurrencyStatus): TransactionParams? {
        val state = stateController.value
        val confirmationState = state.confirmationState as? StakingStates.ConfirmationState.Data
            ?: return null

        val sourceAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: return null

        val vaultAddress = (state.validatorState as? StakingStates.ValidatorState.Data)?.chosenTarget?.address
            ?: state.balanceState?.targetAddress
            ?: return null

        val amount = (state.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
            ?: return null

        return TransactionParams(
            actionType = state.actionType,
            amount = amount,
            vaultAddress = vaultAddress,
            sourceAddress = sourceAddress,
        )
    }

    private suspend fun createTransaction(
        actionType: StakingActionCommonType,
        amount: BigDecimal,
        vaultAddress: String,
        sourceAddress: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx> {
        val network = P2PEthPoolStakingConfig.activeNetwork

        return when (actionType) {
            is StakingActionCommonType.Enter -> {
                p2pEthPoolRepository.createDepositTransaction(
                    network = network,
                    delegatorAddress = sourceAddress,
                    vaultAddress = vaultAddress,
                    amount = amount.toPlainString(),
                )
            }
            is StakingActionCommonType.Exit -> {
                p2pEthPoolRepository.createWithdrawTransaction(
                    network = network,
                    stakerAddress = sourceAddress,
                )
            }
            is StakingActionCommonType.Pending -> {
                Either.Left(StakingError.DomainError("Pending actions not supported for P2PEthPool"))
            }
        }
    }

    data class TransactionParams(
        val actionType: StakingActionCommonType,
        val amount: BigDecimal,
        val vaultAddress: String,
        val sourceAddress: String,
    )
}