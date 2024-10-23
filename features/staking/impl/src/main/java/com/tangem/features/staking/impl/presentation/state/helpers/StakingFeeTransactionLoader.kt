package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.staking.EstimateGasUseCase
import com.tangem.domain.staking.model.StakingApproval
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingGasEstimate
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.GetAllowanceUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.ValidatorState
import com.tangem.features.staking.impl.presentation.state.utils.isSolanaWithdraw
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
internal class StakingFeeTransactionLoader @AssistedInject constructor(
    private val stateController: StakingStateController,
    private val getAllowanceUseCase: GetAllowanceUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val estimateGasUseCase: EstimateGasUseCase,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val yield: Yield,
    @Assisted private val stakingApproval: StakingApproval,
) {

    suspend fun getFee(
        pendingAction: PendingAction?,
        pendingActions: ImmutableList<PendingAction>?,
        onStakingFee: (Fee) -> Unit,
        onStakingFeeError: (StakingError) -> Unit,
        onApprovalFee: (TransactionFee) -> Unit,
        onFeeError: (GetFeeError) -> Unit,
    ) {
        val state = stateController.value
        val confirmationState = state.confirmationState as? StakingStates.ConfirmationState.Data
            ?: error("No confirmation state")
        val validatorState = confirmationState.validatorState as? ValidatorState.Content
            ?: error("No validator provided")

        val amount = (state.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
            ?: error("No amount provided")

        val validatorAddress = validatorState.chosenValidator.address

        val approval = stakingApproval as? StakingApproval.Needed
        if (approval != null && state.actionType == StakingActionCommonType.ENTER) {
            val allowance = getAllowanceUseCase(
                userWalletId = userWallet.walletId,
                cryptoCurrency = cryptoCurrencyStatus.currency,
                spenderAddress = approval.spenderAddress,
            ).getOrElse { BigDecimal.ZERO }

            if (allowance < amount) {
                getApproveFee(
                    amount = amount,
                    validatorAddress = validatorAddress,
                    onApprovalFee = onApprovalFee,
                    onApprovalFeeError = onFeeError,
                )
            } else {
                estimateGas(
                    pendingAction = pendingAction,
                    pendingActions = pendingActions,
                    amount = amount,
                    validatorAddress = validatorAddress,
                    onStakingFeeError = onStakingFeeError,
                    onStakingFee = onStakingFee,
                )
            }
        } else {
            estimateGas(
                pendingAction = pendingAction,
                pendingActions = pendingActions,
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
        onStakingFee: (Fee) -> Unit,
    ) {
        val sourceAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: error("No available address")

        val gasEstimate = if (isSolanaWithdraw(cryptoCurrencyStatus.currency.network.id.value, pendingActions)) {
            val result = coroutineScope {
                pendingActions?.map { action ->
                    async {
                        // Simultaneous or quick api calls can sometimes return ZERO fee
                        estimateFeeRetry {
                            estimateFee(
                                amount = amount,
                                sourceAddress = sourceAddress,
                                validatorAddress = validatorAddress,
                                action = action,
                            )
                        }.getOrElse {
                            onStakingFeeError(it)
                            null
                        }
                    }
                }?.awaitAll()?.filterNotNull()
            }

            if (result.isNullOrEmpty()) {
                onStakingFeeError(StakingError.UnknownError())
                return
            }

            val totalAmount = result.sumOf { it.amount }
            val totalGasLimit = result.sumOf { it.gasLimit?.toBigDecimalOrNull().orZero() }
            StakingGasEstimate(
                amount = totalAmount,
                token = result.first().token,
                gasLimit = totalGasLimit.toPlainString().orEmpty(),
            )
        } else {
            estimateFee(
                amount = amount,
                sourceAddress = sourceAddress,
                validatorAddress = validatorAddress,
                action = pendingAction,
            ).getOrElse {
                onStakingFeeError(it)
                return
            }
        }

        onStakingFee(
            Fee.Common(
                Amount(
                    currencySymbol = gasEstimate.token.symbol,
                    value = gasEstimate.amount,
                    decimals = gasEstimate.token.decimals,
                ),
            ),
        )
    }

    private suspend fun estimateFee(
        amount: BigDecimal,
        sourceAddress: String,
        validatorAddress: String,
        action: PendingAction?,
    ) = estimateGasUseCase(
        userWalletId = userWallet.walletId,
        network = cryptoCurrencyStatus.currency.network,
        params = ActionParams(
            actionCommonType = stateController.value.actionType,
            integrationId = yield.id,
            amount = amount,
            address = sourceAddress,
            validatorAddress = validatorAddress,
            token = yield.getCurrentToken(cryptoCurrencyStatus.currency.id.rawCurrencyId),
            passthrough = action?.passthrough,
            type = action?.type,
        ),
    )

    private suspend fun getApproveFee(
        amount: BigDecimal,
        validatorAddress: String,
        onApprovalFee: (TransactionFee) -> Unit,
        onApprovalFeeError: (GetFeeError) -> Unit,
    ) {
        getFeeUseCase(
            amount = amount,
            destination = validatorAddress,
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrencyStatus.currency,
        ).fold(
            ifRight = { fee ->
                onApprovalFee(fee)
            },
            ifLeft = { error ->
                onApprovalFeeError(error)
            },
        )
    }

    private suspend fun estimateFeeRetry(
        times: Int = 3,
        delay: Long = 1000,
        block: suspend () -> Either<StakingError, StakingGasEstimate>,
    ): Either<StakingError, StakingGasEstimate> {
        repeat(times - 1) {
            val feeResult = block()
            feeResult.fold(
                ifLeft = { return feeResult },
                ifRight = {
                    if (!it.amount.isZero()) return feeResult
                },
            )
            delay(delay)
        }
        return block()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            userWallet: UserWallet,
            yield: Yield,
            stakingApproval: StakingApproval,
        ): StakingFeeTransactionLoader
    }
}
