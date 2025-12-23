package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.EstimateGasUseCase
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.utils.isCompositePendingActions
import com.tangem.utils.extensions.orZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class StakeKitFeeLoader @AssistedInject constructor(
    private val stateController: StakingStateController,
    private val getFeeUseCase: GetFeeUseCase,
    private val estimateGasUseCase: EstimateGasUseCase,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val integration: StakeKitIntegration,
) : StakingFeeLoader {

    override suspend fun getFee(
        onStakingFee: (Fee, Boolean) -> Unit,
        onStakingFeeError: (StakingError) -> Unit,
        onApprovalFee: (TransactionFee) -> Unit,
        onFeeError: (GetFeeError) -> Unit,
    ) {
        val state = stateController.value
        val confirmationState = state.confirmationState as? StakingStates.ConfirmationState.Data
            ?: error("Illegal state")

        val targetAddress = (state.validatorState as? StakingStates.ValidatorState.Data)?.chosenTarget?.address
            ?: state.balanceState?.targetAddress
            ?: error("No target address provided")

        val amount = (state.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
            ?: error("No amount provided")

        getStakeKitFee(
            confirmationState = confirmationState,
            actionType = state.actionType,
            amount = amount,
            validatorAddress = targetAddress,
            onStakingFee = onStakingFee,
            onStakingFeeError = onStakingFeeError,
            onApprovalFee = onApprovalFee,
            onFeeError = onFeeError,
        )
    }

    private suspend fun getStakeKitFee(
        confirmationState: StakingStates.ConfirmationState.Data,
        actionType: StakingActionCommonType,
        amount: BigDecimal,
        validatorAddress: String,
        onStakingFee: (Fee, Boolean) -> Unit,
        onStakingFeeError: (StakingError) -> Unit,
        onApprovalFee: (TransactionFee) -> Unit,
        onFeeError: (GetFeeError) -> Unit,
    ) {
        val isEnter = actionType is StakingActionCommonType.Enter
        val isApprovalNeeded = confirmationState.isApprovalNeeded
        val isAllowanceNotEnough = confirmationState.allowance < amount

        if (isEnter && isApprovalNeeded && isAllowanceNotEnough) {
            getApprovalFee(
                amount = amount,
                validatorAddress = validatorAddress,
                onApprovalFee = onApprovalFee,
                onApprovalFeeError = onFeeError,
            )
        } else {
            estimateGas(
                pendingAction = confirmationState.pendingAction,
                pendingActions = confirmationState.pendingActions,
                amount = amount,
                validatorAddress = validatorAddress,
                onStakingFeeError = onStakingFeeError,
                onStakingFee = onStakingFee,
            )
        }
    }

    private suspend fun estimateGas(
        pendingAction: PendingAction?,
        pendingActions: ImmutableList<PendingAction>?,
        amount: BigDecimal,
        validatorAddress: String,
        onStakingFeeError: (StakingError) -> Unit,
        onStakingFee: (Fee, Boolean) -> Unit,
    ) {
        val sourceAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: error("No available address")

        val gasEstimate = if (isCompositePendingActions(
                networkId = cryptoCurrencyStatus.currency.network.rawId,
                pendingActions = pendingActions,
            )
        ) {
            estimateCompositeGas(
                pendingActions = pendingActions,
                amount = amount,
                sourceAddress = sourceAddress,
                validatorAddress = validatorAddress,
                onStakingFeeError = onStakingFeeError,
            ) ?: return
        } else {
            estimateSingleGas(
                amount = amount,
                sourceAddress = sourceAddress,
                validatorAddress = validatorAddress,
                action = pendingAction,
            ).getOrElse { error ->
                onStakingFeeError(error)
                return
            }
        }

        val feeAmount = Amount(
            currencySymbol = gasEstimate.token.symbol,
            value = gasEstimate.amount,
            decimals = gasEstimate.token.decimals,
        )
        onStakingFee(
            Fee.Common(feeAmount),
            isFeeApproximateUseCase(networkId = cryptoCurrencyStatus.currency.network.id, amountType = feeAmount.type),
        )
    }

    /**
     * Estimates gas for several staking transactions.
     */
    private suspend fun estimateCompositeGas(
        pendingActions: ImmutableList<PendingAction>?,
        amount: BigDecimal,
        sourceAddress: String,
        validatorAddress: String,
        onStakingFeeError: (StakingError) -> Unit,
    ): StakingGasEstimate? {
        val result = coroutineScope {
            pendingActions?.map { action ->
                async {
                    // Simultaneous or quick API calls can sometimes return ZERO fee
                    estimateGasRetry {
                        estimateSingleGas(
                            amount = amount,
                            sourceAddress = sourceAddress,
                            validatorAddress = validatorAddress,
                            action = action,
                        )
                    }.getOrElse { gasError ->
                        onStakingFeeError(gasError)
                        null
                    }
                }
            }?.awaitAll()?.filterNotNull()
        }

        if (result.isNullOrEmpty()) {
            onStakingFeeError(StakingError.DomainError("Error estimating fee"))
            return null
        }

        val totalAmount = result.sumOf { it.amount }
        val totalGasLimit = result.sumOf { it.gasLimit?.toBigDecimalOrNull().orZero() }
        return StakingGasEstimate(
            amount = totalAmount,
            token = result.first().token,
            gasLimit = totalGasLimit.toPlainString().orEmpty(),
        )
    }

    private suspend fun estimateSingleGas(
        amount: BigDecimal,
        sourceAddress: String,
        validatorAddress: String,
        action: PendingAction?,
    ) = estimateGasUseCase(
        userWalletId = userWallet.walletId,
        network = cryptoCurrencyStatus.currency.network,
        params = ActionParams(
            actionCommonType = stateController.value.actionType,
            integrationId = integration.integrationId.value,
            amount = amount,
            address = sourceAddress,
            validatorAddress = validatorAddress,
            token = integration.getCurrentToken(cryptoCurrencyStatus.currency.id.rawCurrencyId),
            passthrough = action?.passthrough,
            type = action?.type,
        ),
    )

    private suspend fun getApprovalFee(
        amount: BigDecimal,
        validatorAddress: String,
        onApprovalFee: (TransactionFee) -> Unit,
        onApprovalFeeError: (GetFeeError) -> Unit,
    ) {
        val tokenCurrency = cryptoCurrencyStatus.currency as? CryptoCurrency.Token
            ?: return onApprovalFeeError(GetFeeError.UnknownError)

        val approvalTransactionData = createApprovalTransactionUseCase(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWalletId = userWallet.walletId,
            amount = amount,
            contractAddress = tokenCurrency.contractAddress,
            spenderAddress = validatorAddress,
        ).getOrElse {
            return onApprovalFeeError(GetFeeError.DataError(it))
        }

        getFeeUseCase(
            userWallet = userWallet,
            network = tokenCurrency.network,
            transactionData = approvalTransactionData,
        ).fold(
            ifRight = onApprovalFee,
            ifLeft = onApprovalFeeError,
        )
    }

    private suspend fun estimateGasRetry(
        times: Int = RETRY_COUNT,
        delayMs: Long = RETRY_DELAY_MS,
        block: suspend () -> Either<StakingError, StakingGasEstimate>,
    ): Either<StakingError, StakingGasEstimate> {
        repeat(times - 1) {
            val feeResult = block()
            feeResult.fold(
                ifLeft = { return feeResult },
                ifRight = { estimate -> if (!estimate.amount.isZero()) return feeResult },
            )
            delay(delayMs)
        }
        return block()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            userWallet: UserWallet,
            integration: StakeKitIntegration,
        ): StakeKitFeeLoader
    }

    private companion object {
        const val RETRY_COUNT = 3
        const val RETRY_DELAY_MS = 1000L
    }
}