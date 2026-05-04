package com.tangem.features.approval.impl.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender.MultipleTransactionSendMode
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetAllowanceInfoUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.utils.TangemBlogUrlBuilder.RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class GiveApprovalModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val getAllowanceInfoUseCase: GetAllowanceInfoUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger,
) : Model(), FeeSelectorModelCallback {

    private val params: GiveApprovalComponent.Params = paramsContainer.require()

    private val userWallet by lazy {
        requireNotNull(
            getUserWalletUseCase(params.userWalletId).getOrNull(),
        ) { "No wallet found for id: $params.userWalletId" }
    }

    val uiState: StateFlow<GiveApprovalUM>
        field = MutableStateFlow(
            GiveApprovalUM(
                approveType = ApproveType.LIMITED,
                walletInteractionIcon = walletInterationIcon(userWallet),
                isApproveButtonEnabled = false,
                isApproveLoading = false,
                isHoldToConfirm = params.isHoldToConfirm,
                isResetApproval = params.isResetApproval,
            ),
        )

    private var feeSelectorUM: FeeSelectorUM = FeeSelectorUM.Loading

    private var approvalTxList: Map<TransactionData.Uncompiled, TransactionFee> = emptyMap()

    override fun onFeeResult(feeSelectorUM: FeeSelectorUM) {
        this.feeSelectorUM = feeSelectorUM
        uiState.update { it.copy(isApproveButtonEnabled = feeSelectorUM.isPrimaryButtonEnabled) }
    }

    fun onApproveClick() {
        params.callback.onApproveClick()
        uiState.update { it.copy(isApproveLoading = true) }
        modelScope.launch(dispatchers.main) {
            val isSuccess = sendApprovalTransaction()
            uiState.update { it.copy(isApproveLoading = false) }
            if (isSuccess) {
                params.callback.onApproveDone()
            } else {
                params.callback.onApproveFailed()
            }
        }
    }

    fun onCancelClick() {
        params.callback.onCancelClick()
    }

    fun onChangeApproveType(approveType: ApproveType) {
        if (uiState.value.approveType == approveType) return
        uiState.update { it.copy(approveType = approveType, isApproveButtonEnabled = false) }
        modelScope.launch {
            feeSelectorReloadTrigger.triggerLoadingState()
            feeSelectorReloadTrigger.triggerUpdate()
        }
    }

    fun onOpenLearnMoreAboutApproveClick() {
        urlOpener.openUrl(RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP)
    }

    fun showPermissionInfoDialog() {
        uiMessageSender.send(
            DialogMessage(
                message = resourceReference(com.tangem.common.ui.R.string.give_permission_staking_footer),
                title = resourceReference(com.tangem.common.ui.R.string.common_approve),
            ),
        )
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        return onApprovalTx(
            onApprove = { approve ->
                getFeeUseCase(
                    transactionData = approve,
                    userWallet = userWallet,
                    network = params.cryptoCurrencyStatus.currency.network,
                ).onRight { fee ->
                    approvalTxList = mapOf(approve to fee)
                }
            },
            onResetApprove = { (revokeApproval, approve) ->
                getFeeUseCase(
                    transactionData = revokeApproval,
                    userWallet = userWallet,
                    network = params.cryptoCurrencyStatus.currency.network,
                ).map { revokeFee ->
                    estimateFeeForResetApproval(
                        revokeTransactionFee = revokeFee,
                        revokeApprovalTransaction = revokeApproval,
                        approvalTransaction = approve,
                    )
                }
            },
        )
    }

    fun shouldDisableCustomFee(): Boolean {
        return approvalTxList.size > 1
    }

    suspend fun loadFeeExtended(maybeToken: CryptoCurrencyStatus?): Either<GetFeeError, TransactionFeeExtended> {
        val approve = createApprovalTransactionUseCase(
            userWalletId = params.userWalletId,
            cryptoCurrencyStatus = params.cryptoCurrencyStatus,
            amount = getApprovalAmount(),
            contractAddress = (params.cryptoCurrencyStatus.currency as CryptoCurrency.Token).contractAddress,
            spenderAddress = params.spenderAddress,
        ).getOrElse { error ->
            TangemLogger.e("Failed to create approveTransaction", error)
            return GetFeeError.DataError(error).left()
        }

        return if (maybeToken == null) {
            getFeeForGaslessUseCase(
                transactionData = approve,
                userWallet = userWallet,
                network = params.cryptoCurrencyStatus.currency.network,
            )
        } else {
            getFeeForTokenUseCase(
                transactionData = approve,
                userWallet = userWallet,
                token = maybeToken.currency,
            )
        }.onRight { feeExtended ->
            approvalTxList = mapOf(approve to feeExtended.transactionFee)
        }
    }

    private fun estimateFeeForResetApproval(
        revokeTransactionFee: TransactionFee,
        revokeApprovalTransaction: TransactionData.Uncompiled,
        approvalTransaction: TransactionData.Uncompiled,
    ) = when (revokeTransactionFee) {
        is TransactionFee.Choosable -> {
            val approveFee = revokeTransactionFee.copy(
                minimum = revokeTransactionFee.minimum.increaseEthereumGasLimitBy(2.toBigDecimal()),
                normal = revokeTransactionFee.normal.increaseEthereumGasLimitBy(2.toBigDecimal()),
                priority = revokeTransactionFee.priority.increaseEthereumGasLimitBy(2.toBigDecimal()),
            )
            approvalTxList = mapOf(
                revokeApprovalTransaction to revokeTransactionFee,
                approvalTransaction to approveFee,
            )
            revokeTransactionFee.copy(
                minimum = approveFee.minimum + revokeTransactionFee.minimum,
                normal = approveFee.normal + revokeTransactionFee.normal,
                priority = approveFee.priority + revokeTransactionFee.priority,
            )
        }
        is TransactionFee.Single -> {
            val approveFee = revokeTransactionFee.copy(normal = revokeTransactionFee.normal)
            approvalTxList = mapOf(
                revokeApprovalTransaction to revokeTransactionFee,
                approvalTransaction to approveFee,
            )
            revokeTransactionFee.copy(normal = approveFee.normal + revokeTransactionFee.normal)
        }
    }

    private suspend fun sendApprovalTransaction(): Boolean {
        val cryptoCurrencyStatus = params.cryptoCurrencyStatus
        val tokenCurrency = cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: return false

        val feeContent = feeSelectorUM as? FeeSelectorUM.Content ?: return false
        val feeExtended = feeContent.feeExtraInfo.transactionFeeExtended

        val isFeeInTokenCurrency = feeExtended?.transactionFee?.normal is Fee.Ethereum.TokenCurrency

        val transactions = approvalTxList.map { (tx, fee) ->
            tx.copy(
                fee = when (feeContent.selectedFeeItem) {
                    is FeeItem.Fast -> (fee as? TransactionFee.Choosable)?.priority ?: fee.normal
                    is FeeItem.Market -> fee.normal
                    is FeeItem.Slow -> (fee as? TransactionFee.Choosable)?.minimum ?: fee.normal
                    else -> feeContent.selectedFeeItem.fee
                },
            )
        }

        return if (isFeeInTokenCurrency) {
            createAndSendGaslessTransactionUseCase(
                userWallet = userWallet,
                transactionData = transactions.first(),
                fee = feeExtended,
            )
        } else {
            sendTransactionUseCase(
                txsData = transactions,
                userWallet = userWallet,
                network = tokenCurrency.network,
                sendMode = MultipleTransactionSendMode.DEFAULT,
            )
        }.fold(
            ifLeft = { error ->
                TangemLogger.e("Failed to send approval transaction: $error")
                false
            },
            ifRight = {
                sendApproveSuccessAnalytics(feeContent)
                true
            },
        )
    }

    private fun sendApproveSuccessAnalytics(feeContent: FeeSelectorUM.Content) {
        val currency = params.cryptoCurrencyStatus.currency
        val feeToken = feeContent.feeExtraInfo.feeCryptoCurrencyStatus.currency.symbol
        val permissionType = when (uiState.value.approveType) {
            ApproveType.LIMITED -> "Current transaction"
            ApproveType.UNLIMITED -> "Unlimited"
        }
        val event = AnalyticsParam.TxSentFrom.Approve(
            blockchain = currency.network.name,
            token = currency.symbol,
            feeType = feeContent.toAnalyticType(),
            feeToken = feeToken,
            permissionType = permissionType,
        )
        analyticsEventHandler.send(
            Basic.TransactionSent(
                sentFrom = event,
                memoType = Basic.TransactionSent.MemoType.Null,
            ),
        )
    }

    private fun getApprovalAmount(): BigDecimal? {
        return if (uiState.value.approveType == ApproveType.LIMITED) {
            params.amount.toBigDecimalOrNull()
        } else {
            null
        }
    }

    private suspend fun <T> onApprovalTx(
        onApprove: suspend (TransactionData.Uncompiled) -> Either<GetFeeError, T>,
        onResetApprove:
        suspend (Pair<TransactionData.Uncompiled, TransactionData.Uncompiled>) -> Either<GetFeeError, T>,
    ): Either<GetFeeError, T> {
        val cryptoCurrencyStatus = params.cryptoCurrencyStatus
        val tokenCurrency = cryptoCurrencyStatus.currency as? CryptoCurrency.Token
            ?: return GetFeeError.DataError(IllegalStateException("Currency is not a token")).left()

        val amount = params.amount.toBigDecimalOrNull()
            ?: return GetFeeError.DataError(IllegalArgumentException("Invalid amount format")).left()

        val allowance = getAllowanceInfoUseCase(
            userWalletId = params.userWalletId,
            cryptoCurrency = cryptoCurrencyStatus.currency,
            spenderAddress = params.spenderAddress,
            requiredAmount = amount,
        ).getOrElse { error ->
            TangemLogger.e("Failed to get allowance info", error)
            return GetFeeError.DataError(error).left()
        }

        val approve = createApprovalTransactionUseCase(
            userWalletId = params.userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amount = getApprovalAmount(),
            contractAddress = tokenCurrency.contractAddress,
            spenderAddress = params.spenderAddress,
        ).getOrElse { error ->
            TangemLogger.e("Failed to create approveTransaction", error)
            return GetFeeError.DataError(error).left()
        }

        return if (allowance is AllowanceInfo.ResetNeeded) {
            val revokeApproval = createApprovalTransactionUseCase(
                userWalletId = params.userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                amount = BigDecimal.ZERO,
                contractAddress = tokenCurrency.contractAddress,
                spenderAddress = params.spenderAddress,
            ).getOrElse { error ->
                TangemLogger.e("Failed to create revoke approveTransaction", error)
                return GetFeeError.DataError(error).left()
            }
            onResetApprove(revokeApproval to approve)
        } else {
            onApprove(approve)
        }
    }

    private fun Fee.increaseEthereumGasLimitBy(multiplier: BigDecimal): Fee {
        if (this !is Fee.Ethereum) return this
        val increasedGasPrice = amount.value?.movePointRight(amount.decimals)
            ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
        val increasedGasLimit = gasLimit
            .multiply(multiplier.toBigInteger())
        val increasedAmount = amount.copy(
            value = increasedGasPrice?.multiply(
                increasedGasLimit.toBigDecimal().movePointLeft(amount.decimals),
            ),
        )
        return when (this) {
            is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.TokenCurrency -> error("handle in [REDACTED_TASK_KEY]")
        }
    }

    private operator fun Fee.plus(otherFee: Fee): Fee {
        if (this !is Fee.Ethereum || otherFee !is Fee.Ethereum) return this
        val gasLimit = this.gasLimit
        val increasedGasPrice = this.amount.value?.movePointRight(this.amount.decimals)
            ?.divide(gasLimit.toBigDecimal(), RoundingMode.HALF_UP)
        val increasedGasLimit = gasLimit + otherFee.gasLimit
        val increasedAmount = this.amount.copy(
            value = increasedGasLimit.toBigDecimal().multiply(increasedGasPrice).movePointLeft(this.amount.decimals),
        )
        return when (this) {
            is Fee.Ethereum.EIP1559 -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.Legacy -> copy(amount = increasedAmount, gasLimit = increasedGasLimit)
            is Fee.Ethereum.TokenCurrency -> this
        }
    }
}