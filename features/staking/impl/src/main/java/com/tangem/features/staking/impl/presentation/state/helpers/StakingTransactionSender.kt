package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.staking.*
import com.tangem.domain.staking.model.PendingTransaction
import com.tangem.domain.staking.model.SubmitHashData
import com.tangem.domain.staking.model.stakekit.*
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.staking.impl.analytics.StakingAnalyticsEvents
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.features.staking.impl.presentation.state.utils.isComposePendingActions
import com.tangem.utils.extensions.orZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.math.BigDecimal
import java.util.UUID

@Suppress("LongParameterList")
internal class StakingTransactionSender @AssistedInject constructor(
    private val stateController: StakingStateController,
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory,
    private val getStakingTransactionUseCase: GetStakingTransactionUseCase,
    private val getConstructedStakingTransactionUseCase: GetConstructedStakingTransactionUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
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
        val amountState = state.amountState as? AmountState.Data ?: error("No amount state")

        val stakingTransactions = getStakingTransactions(
            state = state,
            confirmationState = confirmationState,
            onConstructError = onConstructError,
        )

        val fullTransactionsData = getConstructedTransactions(
            stakingTransactions = stakingTransactions,
            fee = fee,
            amount = amountState.amountTextField.cryptoAmount.value.orZero(),
            onConstructError = onConstructError,
        )

        if (fullTransactionsData.isNullOrEmpty()) {
            onConstructError(StakingError.UnknownError())
            return
        }

        onConstructSuccess(fullTransactionsData.map { it.stakeKitTransaction })

        sendStakingTransaction(
            fullTransactionsData = fullTransactionsData,
            possiblePendingTransaction = composePendingTransaction(confirmationState),
            onSendSuccess = onSendSuccess,
            onSendError = onSendError,
        )
    }

    private fun composePendingTransaction(confirmationState: StakingStates.ConfirmationState.Data): PendingTransaction {
        return confirmationState.possiblePendingTransaction ?: composeStakeTransaction()
    }

    private suspend fun getStakingTransactions(
        state: StakingUiState,
        confirmationState: StakingStates.ConfirmationState.Data,
        onConstructError: (StakingError) -> Unit,
    ) = coroutineScope {
        val isComposePendingActions = isComposePendingActions(
            cryptoCurrencyStatus.currency.network.id.value,
            confirmationState.pendingActions,
        )
        if (isComposePendingActions) {
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
        amount: BigDecimal,
        onConstructError: (StakingError) -> Unit,
    ) = coroutineScope {
        stakingTransactions
            ?.filterNot {
                it.type == StakingTransactionType.APPROVAL || it.status == StakingTransactionStatus.SKIPPED
            }
            ?.map { transaction ->
                async {
                    getConstructedStakingTransactionUseCase(
                        networkId = cryptoCurrencyStatus.currency.network.id.value,
                        fee = fee,
                        amount = amount.convertToSdkAmount(cryptoCurrencyStatus.currency),
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
            }
            ?.awaitAll()
            ?.filterNotNull()
    }

    private suspend fun getStakingTransaction(
        state: StakingUiState,
        confirmationState: StakingStates.ConfirmationState.Data,
        action: PendingAction? = confirmationState.pendingAction,
        onConstructError: (StakingError) -> Unit,
    ): List<StakingTransaction> {
        val validatorState = state.validatorState as? StakingStates.ValidatorState.Data
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
                token = yield.getCurrentToken(cryptoCurrencyStatus.currency.id.rawCurrencyId),
                passthrough = action?.passthrough,
                type = action?.type,
            ),
        ).getOrElse {
            analyticsEventHandler.send(
                StakingAnalyticsEvents.StakekitError(
                    token = state.cryptoCurrencySymbol,
                    stakeKitError = it,
                ),
            )
            onConstructError(it)
            return emptyList()
        }
    }

    private suspend fun sendStakingTransaction(
        fullTransactionsData: List<FullTransactionData>,
        possiblePendingTransaction: PendingTransaction,
        onSendSuccess: (txUrl: String) -> Unit,
        onSendError: (SendTransactionError?) -> Unit,
    ) {
        sendTransactionUseCase(
            txsData = fullTransactionsData.map { it.tangemTransaction },
            userWallet = userWallet,
            network = cryptoCurrencyStatus.currency.network,
        ).fold(
            ifLeft = { error ->
                onSendError(error)
            },
            ifRight = { transactionHashes ->
                submitHash(
                    transactions = fullTransactionsData.map { it.stakeKitTransaction },
                    transactionHashes = transactionHashes,
                    pendingTransaction = possiblePendingTransaction,
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
        transactions: List<StakingTransaction>,
        transactionHashes: List<String>,
        pendingTransaction: PendingTransaction,
    ) {
        transactions
            .zip(transactionHashes)
            .forEach { (transaction, transactionHash) ->
                submitHashUseCase(
                    SubmitHashData(
                        transactionId = transaction.id,
                        transactionHash = transactionHash,
                        pendingTransaction = pendingTransaction,
                    ),
                )
                    .onLeft { error ->
                        analyticsEventHandler.send(
                            StakingAnalyticsEvents.StakekitError(
                                token = stateController.value.cryptoCurrencySymbol,
                                stakeKitError = error,
                            ),
                        )
                        saveUnsubmittedHashUseCase.invoke(
                            transactionId = transaction.id,
                            transactionHash = transactionHash,
                        )
                    }.onRight {
                        Timber.d("Successful hash submission")
                        if (transaction.type != StakingTransactionType.FREEZE_ENERGY) {
                            savePendingTransactionUseCase.invoke(userWallet.walletId, pendingTransaction)
                        }
                    }
            }
    }

    private fun getAmount(amountState: AmountState.Data, fee: Fee, reduceAmountBy: BigDecimal?): BigDecimal {
        val amountValue = amountState.amountTextField.cryptoAmount.value ?: error("No amount value")
        val feeValue = fee.amount.value ?: error("No fee value")
        val isEnterAction = stateController.value.actionType == StakingActionCommonType.Enter

        return checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable && isEnterAction,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy.orZero(),
        )
    }

    private fun composeStakeTransaction(): PendingTransaction {
        val state = stateController.value

        val yieldBalance = cryptoCurrencyStatus.value.yieldBalance as? YieldBalance.Data
        val token = yield.getCurrentToken(cryptoCurrencyStatus.currency.id.rawCurrencyId)

        return PendingTransaction(
            groupId = UUID.randomUUID().toString(),
            token = token,
            type = BalanceType.STAKED,
            amount = (state.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value ?: BigDecimal.ZERO,
            rawCurrencyId = cryptoCurrencyStatus.currency.id.rawCurrencyId,
            validator = (state.validatorState as? StakingStates.ValidatorState.Data)?.chosenValidator,
            balancesId = yieldBalance?.getBalancesUniqueId() ?: 0,
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
