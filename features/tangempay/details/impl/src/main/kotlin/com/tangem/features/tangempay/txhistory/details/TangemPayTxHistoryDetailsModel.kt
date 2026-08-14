package com.tangem.features.tangempay.txhistory.details

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.components.TangemPayTransactionBottomSheetComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayTxHistoryDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val urlOpener: UrlOpener,
    private val balanceHidingSettings: GetBalanceHidingSettingsUseCase,
    private val tangemPayTxHistoryRepository: TangemPayTxHistoryRepository,
    private val cashbackRepository: CashbackRepository,
    private val featureToggles: TangemPayFeatureToggles,
    private val analytics: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<TangemPayTransactionBottomSheetComponent.Params>()

    private val transaction = MutableStateFlow(params.transaction)
    private val transactionLoadState = MutableStateFlow(TransactionLoadState.Loading)
    private var loadTransactionJob: Job? = null

    private val cashbackDetail = MutableStateFlow<TangemPayTxHistoryItem.Cashback?>(null)
    private val cashbackLoadState = MutableStateFlow(TransactionLoadState.Loading)
    private var loadCashbackJob: Job? = null

    private val cardState = combine(transaction, transactionLoadState) { tx, loadState ->
        tx to loadState
    }

    private val cashbackState = combine(cashbackDetail, cashbackLoadState) { detail, loadState ->
        detail to loadState
    }

    val uiState: StateFlow<TangemPayTxHistoryDetailsUM> = combine(
        balanceHidingSettings.isBalanceHidden(),
        cardState,
        cashbackState,
    ) { isBalanceHidden, (transaction, transactionLoadState), (cashbackDetail, cashbackLoadState) ->
        buildUiState(
            isBalanceHidden = isBalanceHidden,
            transaction = transaction,
            transactionLoadState = transactionLoadState,
            cashbackDetail = cashbackDetail,
            cashbackLoadState = cashbackLoadState,
        )
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildUiState(
            isBalanceHidden = params.isBalanceHidden,
            transaction = params.transaction,
            transactionLoadState = transactionLoadState.value,
            cashbackDetail = cashbackDetail.value,
            cashbackLoadState = cashbackLoadState.value,
        ),
    )

    init {
        loadTransaction()
        loadCashbackDetails()
    }

    fun dismiss() {
        params.onDismiss()
    }

    private fun loadTransaction() {
        val current = params.transaction
        if (current !is TangemPayTxHistoryItem.Spend) return
        if (loadTransactionJob?.isActive == true) return
        loadTransactionJob = modelScope.launch {
            transactionLoadState.value = TransactionLoadState.Loading
            tangemPayTxHistoryRepository.getTransaction(
                userWalletId = params.userWalletId,
                transactionId = current.id,
            ).onRight { loaded ->
                if (loaded != null) transaction.value = loaded
                transactionLoadState.value = TransactionLoadState.Loaded
            }.onLeft {
                transactionLoadState.value = TransactionLoadState.Error
            }
        }
    }

    private fun loadCashbackDetails() {
        if (!featureToggles.isCashbackEnabled) return
        val current = params.transaction
        if (current !is TangemPayTxHistoryItem.Spend) return
        if (loadCashbackJob?.isActive == true) return
        loadCashbackJob = modelScope.launch {
            cashbackLoadState.value = TransactionLoadState.Loading
            cashbackRepository.getCashbackDetails(
                userWalletId = params.userWalletId,
                transactionId = current.id,
            ).onRight { details ->
                cashbackDetail.value = details
                cashbackLoadState.value = TransactionLoadState.Loaded
            }.onLeft {
                cashbackLoadState.value = TransactionLoadState.Error
            }
        }
    }

    private fun buildUiState(
        isBalanceHidden: Boolean,
        transaction: TangemPayTxHistoryItem,
        transactionLoadState: TransactionLoadState,
        cashbackDetail: TangemPayTxHistoryItem.Cashback?,
        cashbackLoadState: TransactionLoadState,
    ): TangemPayTxHistoryDetailsUM {
        return TangemPayTxHistoryDetailsConverter.convert(
            value = TangemPayTxHistoryDetailsConverter.Input(
                item = transaction,
                isBalanceHidden = isBalanceHidden,
                transactionLoadState = transactionLoadState,
                cashbackDetails = cashbackDetail,
                cashbackLoadState = cashbackLoadState,
                isCashbackEnabled = featureToggles.isCashbackEnabled,
                onExplorerClick = ::openExplorer,
                onDisputeClick = { dispute(customerId = params.customerId) },
                onCardRefreshClick = ::loadTransaction,
                onCashbackRefreshClick = ::loadCashbackDetails,
                onDismiss = ::dismiss,
            ),
        )
    }

    private fun openExplorer(txHash: String?) {
        txHash?.let(urlOpener::openUrlExternalBrowser)
    }

    private fun dispute(customerId: String) {
        analytics.send(TangemPayAnalyticsEvents.SupportOnTransactionPopupClicked())
        analytics.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.TangemPay))
        modelScope.launch {
            val walletMetaInfo = getWalletMetaInfoUseCase.invoke(params.userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase.invoke(
                FeedbackEmailType.Visa.Dispute(
                    item = params.transaction,
                    walletMetaInfo = walletMetaInfo,
                    customerId = customerId,
                ),
            )
        }
    }
}