package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.models.staking.PendingAction
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.CheckStakingTransactionUseCase
import com.tangem.domain.staking.GetConstructedStakingTransactionUseCase
import com.tangem.domain.staking.StakingTransactionVerdict
import com.tangem.domain.staking.GetStakingTransactionsUseCase
import com.tangem.domain.staking.SaveUnsubmittedHashUseCase
import com.tangem.domain.staking.SubmitHashUseCase
import com.tangem.domain.staking.model.StakeKitIntegration
import com.tangem.domain.staking.model.SubmitHashData
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.model.stakekit.action.StakingActionCommonType
import com.tangem.domain.staking.model.stakekit.transaction.ActionParams
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransaction
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionStatus
import com.tangem.domain.staking.model.stakekit.transaction.StakingTransactionType
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.features.staking.impl.di.StakingClock
import com.tangem.features.staking.impl.presentation.state.FeeState
import com.tangem.features.staking.impl.presentation.state.StakingStateController
import com.tangem.features.staking.impl.presentation.state.StakingStates
import com.tangem.features.staking.impl.presentation.state.StakingUiState
import com.tangem.features.staking.impl.presentation.state.utils.checkAndCalculateSubtractedAmount
import com.tangem.features.staking.impl.presentation.state.utils.isCompositePendingActions
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList", "LargeClass")
internal class StakeKitTransactionSender @AssistedInject constructor(
    private val stateController: StakingStateController,
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory,
    private val getStakingTransactionsUseCase: GetStakingTransactionsUseCase,
    private val getConstructedStakingTransactionUseCase: GetConstructedStakingTransactionUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val submitHashUseCase: SubmitHashUseCase,
    private val saveUnsubmittedHashUseCase: SaveUnsubmittedHashUseCase,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
    private val checkStakingTransactionUseCase: CheckStakingTransactionUseCase,
    @StakingClock private val clock: Clock,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val integration: StakeKitIntegration,
    @Assisted private val isAmountSubtractAvailable: Boolean,
) : StakingTransactionSender {

    private val balanceUpdater: StakingBalanceUpdater
        get() = stakingBalanceUpdater.create(cryptoCurrencyStatus, userWallet, integration)

    // The transactions validated in [validate] are cached here and reused in [send] so we don't build
    // and validate them twice. The cache is only reused when the build inputs are unchanged ([BuildKey])
    // and the build is not stale (see [ValidatedBuild.isStaleForSend]).
    private var validatedBuild: ValidatedBuild? = null

    override suspend fun validate(): StakingTransactionVerdict {
        val state = stateController.value
        val confirmationState = state.confirmationState as? StakingStates.ConfirmationState.Data
            ?: return StakingTransactionVerdict.SAFE
        val fee = (confirmationState.feeState as? FeeState.Content)?.fee ?: return StakingTransactionVerdict.SAFE
        val amountState = state.amountState as? AmountState.Data ?: return StakingTransactionVerdict.SAFE
        if (cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value == null) {
            return StakingTransactionVerdict.SAFE
        }

        var hasError = false
        val onError: (StakingError) -> Unit = { hasError = true }

        val fullTransactionsData = buildTransactions(
            state = state,
            confirmationState = confirmationState,
            fee = fee,
            amount = amountState.amountTextField.cryptoAmount.value.orZero(),
            onConstructError = onError,
        )
        if (hasError || fullTransactionsData.isNullOrEmpty()) return StakingTransactionVerdict.SAFE

        val verdict = validateConstructedTransactions(fullTransactionsData)
        validatedBuild = currentBuildKey(state)?.let { key ->
            ValidatedBuild(
                key = key,
                transactions = fullTransactionsData,
                verdict = verdict,
                builtAt = clock.now(),
            )
        }
        return verdict
    }

    override suspend fun send(callbacks: StakingTransactionSender.Callbacks) {
        constructAndSendTransactions(
            onConstructSuccess = callbacks.onConstructSuccess,
            onConstructError = callbacks.onConstructError,
            onSendSuccess = callbacks.onSendSuccess,
            onSendError = callbacks.onSendError,
            onFeeIncreased = callbacks.onFeeIncreased,
        )
    }

    private suspend fun constructAndSendTransactions(
        onConstructSuccess: (List<StakingTransaction>) -> Unit,
        onConstructError: (StakingError) -> Unit,
        onSendSuccess: (String) -> Unit,
        onSendError: (SendTransactionError) -> Unit,
        onFeeIncreased: (Fee, Boolean) -> Unit,
    ) {
        val state = stateController.value

        val confirmationState = state.confirmationState as? StakingStates.ConfirmationState.Data
            ?: error("No confirmation state")
        val fee = (confirmationState.feeState as? FeeState.Content)?.fee
            ?: error("No fee provided")
        val amountState = state.amountState as? AmountState.Data ?: error("No amount state")

        val reused = reusableBuild(state)
        val fullTransactionsData: List<FullTransactionData>
        val verdict: StakingTransactionVerdict
        if (reused != null) {
            fullTransactionsData = reused.transactions
            verdict = reused.verdict
        } else {
            val built = buildTransactions(
                state = state,
                confirmationState = confirmationState,
                fee = fee,
                amount = amountState.amountTextField.cryptoAmount.value.orZero(),
                onConstructError = onConstructError,
            )
            if (built.isNullOrEmpty()) {
                onConstructError(StakingError.DomainError("fullTransactionsData is null or empty"))
                return
            }
            fullTransactionsData = built
            verdict = validateConstructedTransactions(built)
        }

        if (verdict == StakingTransactionVerdict.UNSAFE) {
            onConstructError(StakingError.TransactionValidationFailed("Transaction blocked by security validation"))
            return
        }

        val totalFee = fullTransactionsData.sumOf { it.stakeKitTransaction.gasEstimate?.amount.orZero() }

        if (fee.amount.value.orZero() >= totalFee) {
            onConstructSuccess(fullTransactionsData.map { it.stakeKitTransaction })
            sendTransaction(
                fullTransactionsData = fullTransactionsData,
                onSendSuccess = onSendSuccess,
                onSendError = onSendError,
            )
        } else {
            val amount = fee.amount.copy(value = totalFee)
            onFeeIncreased(
                Fee.Common(amount),
                isFeeApproximateUseCase(networkId = cryptoCurrencyStatus.currency.network.id, amountType = amount.type),
            )
        }
    }

    private suspend fun getStakingTransactions(
        state: StakingUiState,
        confirmationState: StakingStates.ConfirmationState.Data,
        onConstructError: (StakingError) -> Unit,
    ) = coroutineScope {
        val isComposePendingActions = isCompositePendingActions(
            networkId = cryptoCurrencyStatus.currency.network.rawId,
            pendingActions = confirmationState.pendingActions,
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
                        networkId = cryptoCurrencyStatus.currency.network.id.rawId,
                        fee = fee,
                        amount = amount.convertToSdkAmount(cryptoCurrencyStatus),
                        transactionId = transaction.id,
                    ).fold(
                        ifRight = { (constructedTransaction, transactionData) ->
                            FullTransactionData(
                                stakeKitTransaction = constructedTransaction,
                                tangemTransaction = transactionData,
                            )
                        },
                        ifLeft = { error ->
                            onConstructError(error)
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

        val validatorAddress = validatorState.chosenTarget.address
        val amount = getAmount(amountState, fee, confirmationState.reduceAmountBy)

        return getStakingTransactionsUseCase(
            userWalletId = userWallet.walletId,
            network = cryptoCurrencyStatus.currency.network,
            params = ActionParams(
                actionCommonType = state.actionType,
                integrationId = integration.integrationId.value,
                amount = amount,
                address = defaultAddress,
                validatorAddress = validatorAddress,
                token = integration.getCurrentToken(cryptoCurrencyStatus.currency.id.rawCurrencyId),
                passthrough = action?.passthrough,
                type = action?.type,
            ),
        ).getOrElse { error ->
            onConstructError(error)
            return emptyList()
        }
    }

    private suspend fun validateConstructedTransactions(
        transactions: List<FullTransactionData>,
    ): StakingTransactionVerdict {
        val accountAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: return StakingTransactionVerdict.UNSAFE
        var worst = StakingTransactionVerdict.SAFE
        transactions.forEach { item ->
            val verdict = checkStakingTransactionUseCase(
                network = item.stakeKitTransaction.network,
                accountAddress = accountAddress,
                unsignedTransaction = item.stakeKitTransaction.unsignedTransaction,
                token = cryptoCurrencyStatus.currency.symbol,
                blockchain = cryptoCurrencyStatus.currency.network.name,
                provider = STAKEKIT_PROVIDER,
            )
            worst = maxOf(worst, verdict)
        }
        return worst
    }

    private suspend fun sendTransaction(
        fullTransactionsData: List<FullTransactionData>,
        onSendSuccess: (txUrl: String) -> Unit,
        onSendError: (SendTransactionError) -> Unit,
    ) {
        if (fullTransactionsData.isEmpty()) return

        val sortedTransactions = fullTransactionsData.sortedBy { it.stakeKitTransaction.stepIndex }

        val firstTransaction = sortedTransactions.first()
        val network = firstTransaction.stakeKitTransaction.network

        val sendMode = if (network == NetworkType.SOLANA &&
            firstTransaction.stakeKitTransaction.type == StakingTransactionType.SPLIT
        ) {
            TransactionSender.MultipleTransactionSendMode.WAIT_AFTER_FIRST
        } else {
            TransactionSender.MultipleTransactionSendMode.DEFAULT
        }

        sendTransactionUseCase(
            txsData = sortedTransactions.map { it.tangemTransaction },
            userWallet = userWallet,
            network = cryptoCurrencyStatus.currency.network,
            sendMode = sendMode,
        ).fold(
            ifLeft = { error ->
                onSendError(error)
            },
            ifRight = { transactionHashes ->
                submitHash(
                    transactions = sortedTransactions.map { it.stakeKitTransaction },
                    transactionHashes = transactionHashes,
                )

                val txUrl = getExplorerTransactionUrlUseCase(
                    txHash = transactionHashes.last(),
                    currency = cryptoCurrencyStatus.currency,
                ).getOrNull().orEmpty()

                balanceUpdater.updateAfterTransaction()
                onSendSuccess(txUrl)
            },
        )
    }

    private suspend fun submitHash(transactions: List<StakingTransaction>, transactionHashes: List<String>) {
        transactions
            .zip(transactionHashes)
            .forEach { (transaction, transactionHash) ->
                submitHashUseCase(
                    SubmitHashData(
                        transactionId = transaction.id,
                        transactionHash = transactionHash,
                    ),
                )
                    .onLeft {
                        saveUnsubmittedHashUseCase.invoke(
                            transactionId = transaction.id,
                            transactionHash = transactionHash,
                        )
                    }.onRight {
                        TangemLogger.d("Successful hash submission")
                    }
            }
    }

    private fun getAmount(amountState: AmountState.Data, fee: Fee, reduceAmountBy: BigDecimal?): BigDecimal {
        val amountValue = amountState.amountTextField.cryptoAmount.value ?: error("No amount value")
        val feeValue = fee.amount.value ?: error("No fee value")
        val isEnterAction = stateController.value.actionType is StakingActionCommonType.Enter

        return checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable && isEnterAction,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = reduceAmountBy.orZero(),
        )
    }

    private suspend fun buildTransactions(
        state: StakingUiState,
        confirmationState: StakingStates.ConfirmationState.Data,
        fee: Fee,
        amount: BigDecimal,
        onConstructError: (StakingError) -> Unit,
    ): List<FullTransactionData>? {
        val stakingTransactions = getStakingTransactions(
            state = state,
            confirmationState = confirmationState,
            onConstructError = onConstructError,
        )
        return getConstructedTransactions(
            stakingTransactions = stakingTransactions,
            fee = fee,
            amount = amount,
            onConstructError = onConstructError,
        )
    }

    // Returns the previously validated build only if it still matches the current inputs and is not
    // stale; otherwise the caller must rebuild and re-validate.
    private fun reusableBuild(state: StakingUiState): ValidatedBuild? {
        val cached = validatedBuild ?: return null
        val key = currentBuildKey(state) ?: return null
        return cached.takeIf { it.key == key && !it.isStaleForSend(clock.now()) }
    }

    // The inputs that determine the built transactions. If any of them changed between [validate] and
    // [send], the cached build is discarded and rebuilt.
    private fun currentBuildKey(state: StakingUiState): BuildKey? {
        val confirmationState = state.confirmationState as? StakingStates.ConfirmationState.Data ?: return null
        val fee = (confirmationState.feeState as? FeeState.Content)?.fee ?: return null
        val amountState = state.amountState as? AmountState.Data ?: return null
        val validatorState = state.validatorState as? StakingStates.ValidatorState.Data ?: return null
        val accountAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value ?: return null
        return BuildKey(
            actionType = state.actionType,
            accountAddress = accountAddress,
            validatorAddress = validatorState.chosenTarget.address,
            cryptoAmount = amountState.amountTextField.cryptoAmount.value.orZero(),
            feeAmount = fee.amount.value.orZero(),
            pendingAction = confirmationState.pendingAction,
            reduceAmountBy = confirmationState.reduceAmountBy.orZero(),
        )
    }

    private data class FullTransactionData(
        val stakeKitTransaction: StakingTransaction,
        val tangemTransaction: TransactionData.Compiled,
    )

    private data class ValidatedBuild(
        val key: BuildKey,
        val transactions: List<FullTransactionData>,
        val verdict: StakingTransactionVerdict,
        val builtAt: Instant,
    ) {
        // Solana transactions embed a recentBlockhash that expires. A cached Solana build may be reused
        // only while it is fresh; past [SOLANA_BUILD_TTL] it is rebuilt + re-validated so we never sign
        // an expired blockhash. Freshness is measured on the wall clock, which can jump backwards
        // (NTP/manual change) — a negative elapsed is untrustworthy and also treated as stale. Other
        // chains are deterministic and never go stale.
        fun isStaleForSend(now: Instant): Boolean {
            val isSolana = transactions.any { it.stakeKitTransaction.network == NetworkType.SOLANA }
            if (!isSolana) return false
            val elapsed = now - builtAt
            return elapsed < Duration.ZERO || elapsed >= SOLANA_BUILD_TTL
        }
    }

    // Every field here must influence the built transaction, so that an equal key guarantees identical
    // built bytes. [pendingAction] (its passthrough/type) and [reduceAmountBy] are folded into the
    // built tx by getStakingTransaction/getAmount, so they belong in the key too. BigDecimal equality is
    // scale-sensitive, but a spurious mismatch only forces a safe rebuild + re-validation, never a wrong
    // reuse, so it is acceptable here.
    private data class BuildKey(
        val actionType: StakingActionCommonType,
        val accountAddress: String,
        val validatorAddress: String,
        val cryptoAmount: BigDecimal,
        val feeAmount: BigDecimal,
        val pendingAction: PendingAction?,
        val reduceAmountBy: BigDecimal,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            userWallet: UserWallet,
            integration: StakeKitIntegration,
            isAmountSubtractAvailable: Boolean,
        ): StakeKitTransactionSender
    }
}

private const val STAKEKIT_PROVIDER = "StakeKit"

// Reuse window for a built Solana transaction before its recentBlockhash risks expiring (blockhashes
// live ~60-90s on-chain). Measured from validate() (confirmation-screen entry), so it must leave
// headroom for the post-send signing + broadcast latency that accrues after this check.
private val SOLANA_BUILD_TTL = 50.seconds