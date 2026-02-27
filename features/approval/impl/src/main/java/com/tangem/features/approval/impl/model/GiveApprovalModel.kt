package com.tangem.features.approval.impl.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateApprovalTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.utils.TangemBlogUrlBuilder.RESOURCE_TO_LEARN_ABOUT_APPROVING_IN_SWAP
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class GiveApprovalModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val createApprovalTransactionUseCase: CreateApprovalTransactionUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
    private val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
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
            ),
        )

    private var feeSelectorUM: FeeSelectorUM = FeeSelectorUM.Loading

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
        uiState.update { it.copy(approveType = approveType) }
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

    suspend fun prepareApprovalTransaction(): Either<Throwable, TransactionData> {
        val cryptoCurrencyStatus = params.cryptoCurrencyStatus
        val tokenCurrency = cryptoCurrencyStatus.currency as? CryptoCurrency.Token
            ?: return Either.Left(IllegalStateException("Currency is not a token"))

        return createApprovalTransactionUseCase(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWalletId = params.userWalletId,
            amount = getApprovalAmount(),
            contractAddress = tokenCurrency.contractAddress,
            spenderAddress = params.spenderAddress,
        )
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val approvalTransaction = prepareApprovalTransaction()
            .getOrElse { return GetFeeError.DataError(it).left() }

        return getFeeUseCase(
            transactionData = approvalTransaction,
            userWallet = userWallet,
            network = params.cryptoCurrencyStatus.currency.network,
        )
    }

    suspend fun loadFeeExtended(maybeToken: CryptoCurrencyStatus?): Either<GetFeeError, TransactionFeeExtended> {
        val approvalTransaction = prepareApprovalTransaction()
            .getOrElse { return GetFeeError.DataError(it).left() }

        return if (maybeToken == null) {
            getFeeForGaslessUseCase(
                transactionData = approvalTransaction,
                userWallet = userWallet,
                network = params.cryptoCurrencyStatus.currency.network,
            )
        } else {
            getFeeForTokenUseCase(
                transactionData = approvalTransaction,
                userWallet = userWallet,
                token = maybeToken.currency,
            )
        }
    }

    private suspend fun sendApprovalTransaction(): Boolean {
        val cryptoCurrencyStatus = params.cryptoCurrencyStatus
        val tokenCurrency = cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: return false

        val feeContent = feeSelectorUM as? FeeSelectorUM.Content ?: return false
        val selectedFee = feeContent.selectedFeeItem.fee
        val feeExtended = feeContent.feeExtraInfo.transactionFeeExtended

        val isFeeInTokenCurrency = feeExtended?.transactionFee?.normal is Fee.Ethereum.TokenCurrency

        val transactionData = createApprovalTransactionUseCase(
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            userWalletId = params.userWalletId,
            amount = getApprovalAmount(),
            fee = selectedFee,
            contractAddress = tokenCurrency.contractAddress,
            spenderAddress = params.spenderAddress,
        ).getOrElse { error ->
            Timber.e(error, "Failed to create approval transaction")
            return false
        }

        return if (isFeeInTokenCurrency) {
            createAndSendGaslessTransactionUseCase(
                userWallet = userWallet,
                transactionData = transactionData,
                fee = feeExtended,
            )
        } else {
            sendTransactionUseCase(
                txData = transactionData,
                userWallet = userWallet,
                network = tokenCurrency.network,
            )
        }.fold(
            ifLeft = { error ->
                Timber.e("Failed to send approval transaction: $error")
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
}