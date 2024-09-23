package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.staking.*
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.SubmitHashData
import com.tangem.domain.staking.model.stakekit.BalanceType
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.Yield
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.SendMultipleTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.analytics.StakingAnalyticsEvents
import com.tangem.features.staking.impl.presentation.state.*
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.features.staking.impl.presentation.state.utils.isSolanaWithdraw
import com.tangem.utils.extensions.orZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class StakingTransactionSender @AssistedInject constructor(
    private val stateController: StakingStateController,
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory,
    private val getStakingTransactionUseCase: GetStakingTransactionUseCase,
    private val getConstructedStakingTransactionUseCase: GetConstructedStakingTransactionUseCase,
    private val sendMultipleTransactionUseCase: SendMultipleTransactionUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val submitHashUseCase: SubmitHashUseCase,
    private val saveUnsubmittedHashUseCase: SaveUnsubmittedHashUseCase,
    private val savePendingTransactionUseCase: SavePendingTransactionUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val yield: Yield,
    @Assisted private val isAmountSubtractAvailable: Boolean,
) {

    private val balanceUpdater: StakingBalanceUpdater
        get() = stakingBalanceUpdater.create(cryptoCurrencyStatus, userWallet)

    suspend fun constructAndSendTransactions(
        onConstructSuccess: (List<StakingTransaction>) -> Unit,
        onConstructError: (StakingError) -> Unit,
        onSendSuccess: (String) -> Unit,
        onSendError: (SendTransactionError?) -> Unit,
    ) {
        val state = stateController.value

        val confirmationState = state.confirmationState as? StakingStates.ConfirmationState.Data
            ?: error("No confirmation state")
        val fee = (confirmationState.feeState as? FeeState.Content)?.fee
            ?: error("No fee provided")

        val stakingTransactions = getStakingTransactions(
            state = state,
            confirmationState = confirmationState,
            onConstructError = onConstructError,
        )

        val fullTransactionsData = getConstructedTransactions(
            stakingTransactions = stakingTransactions,
            fee = fee,
            onConstructError = onConstructError,
        )

        if (fullTransactionsData.isNullOrEmpty()) {
            onConstructError(StakingError.UnknownError)
            return
        }

        onConstructSuccess(fullTransactionsData.map { it.stakeKitTransaction })

        sendStakingTransaction(
            fullTransactionsData = fullTransactionsData,
            balanceState = confirmationState.balanceState,
            onSendSuccess = onSendSuccess,
            onSendError = onSendError,
        )
    }

    private suspend fun getStakingTransactions(
        state: StakingUiState,
        confirmationState: StakingStates.ConfirmationState.Data,
        onConstructError: (StakingError) -> Unit,
    ) = coroutineScope {
        val isAllWithdrawAction = isSolanaWithdraw(
            cryptoCurrencyStatus.currency.network.id.value,
            confirmationState.pendingActions,
        )
        if (isAllWithdrawAction) {
            confirmationState.pendingActions?.map { action ->
                async {
                    getStakingTransaction(
                        state = state,
                        action = action,
                        confirmationState = confirmationState,
                        onConstructError = onConstructError,
                    )
                }
            }?.awaitAll()?.flatten()
        } else {
            getStakingTransaction(
                state = state,
                confirmationState = confirmationState,
                onConstructError = onConstructError,
            )
        }
    }

    private suspend fun getConstructedTransactions(
        stakingTransactions: List<StakingTransaction>?,
        fee: Fee,
        onConstructError: (StakingError) -> Unit,
    ) = coroutineScope {
        stakingTransactions?.filterNot { it.type == StakingTransactionType.APPROVAL }
            ?.map { transaction ->
                async {
                    getConstructedStakingTransactionUseCase(
                        networkId = cryptoCurrencyStatus.currency.network.id.value,
                        fee = fee,
                        transactionId = transaction.id,
                    ).fold(
                        ifRight = { (constructedTransaction, transactionData) ->
                            FullTransactionData(
                                stakeKitTransaction = constructedTransaction,
                                tangemTransaction = transactionData,
                            )
                        },
                        ifLeft = {
                            onConstructError(it)
                            null
                        },
                    )
                }
            }?.awaitAll()?.filterNotNull()
    }

    private suspend fun getStakingTransaction(
        state: StakingUiState,
        confirmationState: StakingStates.ConfirmationState.Data,
        action: PendingAction? = confirmationState.pendingAction,
        onConstructError: (StakingError) -> Unit,
    ): List<StakingTransaction> {
        val validatorState = confirmationState.validatorState as? ValidatorState.Content
            ?: error("No validator provided")
        val fee = (confirmationState.feeState as? FeeState.Content)?.fee
            ?: error("No fee provided")
        val defaultAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: error("No available address")
        val amountState = state.amountState as? AmountState.Data
            ?: error("No amount provided")

        val validatorAddress = validatorState.chosenValidator.address
        val amount = getAmount(amountState, fee, confirmationState.reduceAmountBy)

        return getStakingTransactionUseCase(
            userWalletId = userWallet.walletId,
            network = cryptoCurrencyStatus.currency.network,
            params = ActionParams(
                actionCommonType = state.actionType,
                integrationId = yield.id,
                amount = amount,
                address = defaultAddress,
                validatorAddress = validatorAddress,
                token = yield.token,
                passthrough = action?.passthrough,
                type = action?.type,
            ),
        ).getOrElse {
            analyticsEventHandler.send(StakingAnalyticsEvents.StakingError(state.cryptoCurrencyName))
            onConstructError(it)
            return emptyList()
        }
    }

    private suspend fun sendStakingTransaction(
        fullTransactionsData: List<FullTransactionData>,
        balanceState: BalanceState?,
        onSendSuccess: (txUrl: String) -> Unit,
        onSendError: (SendTransactionError?) -> Unit,
    ) {
        sendMultipleTransactionUseCase(
            txsData = fullTransactionsData.map { it.tangemTransaction },
            userWallet = userWallet,
            network = cryptoCurrencyStatus.currency.network,
        ).fold(
            ifLeft = { error ->
                onSendError(error)
            },
            ifRight = { transactionHashes ->
                submitHash(
                    transactionIds = fullTransactionsData.map { it.stakeKitTransaction.id },
                    transactionHashes = transactionHashes,
                    groupId = balanceState?.groupId,
                    validator = balanceState?.validator,
                    amount = balanceState?.cryptoDecimal,
                    balanceType = balanceState?.type,
                    rawCurrencyId = balanceState?.rawCurrencyId,
                )
                val txUrl = getExplorerTransactionUrlUseCase(
                    txHash = transactionHashes.last(),
                    networkId = cryptoCurrencyStatus.currency.network.id,
                ).getOrElse { "" }

                balanceUpdater.scheduleUpdates()
                onSendSuccess(txUrl)
            },
        )
    }

    private suspend fun submitHash(
        transactionIds: List<String>,
        transactionHashes: List<String>,
        groupId: String?,
        validator: Yield.Validator?,
        amount: BigDecimal?,
        balanceType: BalanceType?,
        rawCurrencyId: String?,
    ) {
        transactionIds
            .zip(transactionHashes)
            .forEach { (transactionId, transactionHash) ->
                submitHashUseCase(
                    SubmitHashData(
                        transactionId = transactionId,
                        transactionHash = transactionHash,
                        validator = validator,
                        amount = amount,
                        balanceType = balanceType,
                        rawCurrencyId = rawCurrencyId,
                    ),
                )
                    .onLeft {
                        analyticsEventHandler.send(
                            StakingAnalyticsEvents.StakingError(stateController.value.cryptoCurrencyName),
                        )
                        saveUnsubmittedHashUseCase.invoke(
                            transactionId = transactionId,
                            transactionHash = transactionHash,
                        )
                    }.onRight {
                        Timber.d("Successful hash submission")
                        savePendingTransactionUseCase.invoke(
                            PendingTransaction(
                                groupId = groupId,
                                type = balanceType,
                                amount = amount,
                                rawCurrencyId = rawCurrencyId,
                                validator = validator,
                            ),
                        )
                    }
            }
    }

    private fun getAmount(amountState: AmountState.Data, fee: Fee, reduceAmountBy: BigDecimal?): BigDecimal {
        val amountValue = amountState.amountTextField.cryptoAmount.value ?: error("No amount value")
        val feeValue = fee.amount.value ?: error("No fee value")
        val isEnterAction = stateController.value.actionType == StakingActionCommonType.ENTER

        return checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable && isEnterAction,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy.orZero(),
        )
    }

    private data class FullTransactionData(
        val stakeKitTransaction: StakingTransaction,
        val tangemTransaction: TransactionData.Compiled,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            userWallet: UserWallet,
            yield: Yield,
            isAmountSubtractAvailable: Boolean,
        ): StakingTransactionSender
    }
}
