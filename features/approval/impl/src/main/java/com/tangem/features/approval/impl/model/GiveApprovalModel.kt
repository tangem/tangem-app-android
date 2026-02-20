package com.tangem.features.approval.impl.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
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
) : Model(), FeeSelectorModelCallback {

    private val params: GiveApprovalComponent.Params = paramsContainer.require()

    val uiState: StateFlow<GiveApprovalUM>
        field = MutableStateFlow(
            GiveApprovalUM(
                approveType = ApproveType.LIMITED,
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
            userWalletId = params.userWallet.walletId,
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
            userWallet = params.userWallet,
            network = params.cryptoCurrencyStatus.currency.network,
        )
    }

    suspend fun loadFeeExtended(maybeToken: CryptoCurrencyStatus?): Either<GetFeeError, TransactionFeeExtended> {
        val approvalTransaction = prepareApprovalTransaction()
            .getOrElse { return GetFeeError.DataError(it).left() }

        return if (maybeToken == null) {
            getFeeForGaslessUseCase(
                transactionData = approvalTransaction,
                userWallet = params.userWallet,
                network = params.cryptoCurrencyStatus.currency.network,
            )
        } else {
            getFeeForTokenUseCase(
                transactionData = approvalTransaction,
                userWallet = params.userWallet,
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
            userWalletId = params.userWallet.walletId,
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
                userWallet = params.userWallet,
                transactionData = transactionData,
                fee = feeExtended,
            )
        } else {
            sendTransactionUseCase(
                txData = transactionData,
                userWallet = params.userWallet,
                network = tokenCurrency.network,
            )
        }.fold(
            ifLeft = { error ->
                Timber.e("Failed to send approval transaction: $error")
                false
            },
            ifRight = { true },
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