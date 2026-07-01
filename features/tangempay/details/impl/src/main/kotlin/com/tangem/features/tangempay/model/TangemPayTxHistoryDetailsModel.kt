package com.tangem.features.tangempay.model

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
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.features.tangempay.components.TangemPayTransactionBottomSheetComponent
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUiStates
import com.tangem.features.tangempay.model.transformers.TangemPayTxHistoryDetailsConverter
import com.tangem.features.tangempay.model.transformers.TangemPayTxHistoryDetailsConverterV2
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    private val analytics: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<TangemPayTransactionBottomSheetComponent.Params>()

    private val transaction = MutableStateFlow(params.transaction)

    val uiState: StateFlow<TangemPayTxHistoryDetailsUiStates> = combine(
        balanceHidingSettings.isBalanceHidden(),
        transaction,
    ) { isBalanceHidden, transaction ->
        buildUiStates(isBalanceHidden, transaction)
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = buildUiStates(isBalanceHidden = params.isBalanceHidden, transaction = params.transaction),
    )

    init {
        loadTransaction()
    }

    fun dismiss() {
        params.onDismiss()
    }

    private fun loadTransaction() {
        val current = params.transaction
        if (current !is TangemPayTxHistoryItem.Spend) return
        modelScope.launch {
            tangemPayTxHistoryRepository.getTransaction(
                userWalletId = params.userWalletId,
                transactionId = current.id,
            ).onRight { loaded -> if (loaded != null) transaction.value = loaded }
        }
    }

    private fun buildUiStates(
        isBalanceHidden: Boolean,
        transaction: TangemPayTxHistoryItem,
    ): TangemPayTxHistoryDetailsUiStates {
        val converterInput = TangemPayTxHistoryDetailsConverter.Input(
            item = transaction,
            isBalanceHidden = isBalanceHidden,
            onExplorerClick = ::openExplorer,
            onDisputeClick = { dispute(customerId = params.customerId) },
            onDismiss = ::dismiss,
        )
        return TangemPayTxHistoryDetailsUiStates(
            legacy = TangemPayTxHistoryDetailsConverter.convert(converterInput),
            redesign = TangemPayTxHistoryDetailsConverterV2.convert(
                value = TangemPayTxHistoryDetailsConverterV2.Input(
                    item = converterInput.item,
                    isBalanceHidden = converterInput.isBalanceHidden,
                    onExplorerClick = converterInput.onExplorerClick,
                    onDisputeClick = converterInput.onDisputeClick,
                    onDismiss = converterInput.onDismiss,
                ),
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
                FeedbackEmailType.Visa.DisputeV2(
                    item = params.transaction,
                    walletMetaInfo = walletMetaInfo,
                    customerId = customerId,
                ),
            )
        }
    }
}