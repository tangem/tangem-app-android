package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryDetailsComponent
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUM
import com.tangem.features.tangempay.model.transformers.TangemPayTxHistoryDetailsConverter
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
    private val analytics: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<TangemPayTxHistoryDetailsComponent.Params>()
    val uiState: StateFlow<TangemPayTxHistoryDetailsUM>
        field = MutableStateFlow(
            value = TangemPayTxHistoryDetailsConverter.convert(
                value = TangemPayTxHistoryDetailsConverter.Input(
                    item = params.transaction,
                    isBalanceHidden = params.isBalanceHidden,
                    onExplorerClick = ::openExplorer,
                    onDisputeClick = ::dispute,
                    onDismiss = ::dismiss,
                ),
            ),
        )

    init {
        subscribeToBalanceHiding()
    }

    fun dismiss() {
        params.onDismiss()
    }

    private fun subscribeToBalanceHiding() {
        balanceHidingSettings.isBalanceHidden()
            .onEach { isBalanceHidden -> uiState.update { it.copy(isBalanceHidden = isBalanceHidden) } }
            .launchIn(modelScope)
    }

    private fun openExplorer(txHash: String?) {
        txHash?.let(urlOpener::openUrlExternalBrowser)
    }

    private fun dispute() {
        analytics.send(TangemPayAnalyticsEvents.SupportOnTransactionPopupClicked())
        modelScope.launch {
            val walletMetaInfo = getWalletMetaInfoUseCase.invoke(params.userWalletId).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase.invoke(
                FeedbackEmailType.Visa.DisputeV2(
                    item = params.transaction,
                    walletMetaInfo = walletMetaInfo,
                ),
            )
        }
    }
}