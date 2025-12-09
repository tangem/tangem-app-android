package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.tangempay.components.txHistory.TangemPayTxHistoryDetailsComponent
import com.tangem.features.tangempay.entity.TangemPayTxHistoryDetailsUM
import com.tangem.features.tangempay.model.transformers.TangemPayTxHistoryDetailsConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class TangemPayTxHistoryDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletsUseCase: GetWalletsUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val urlOpener: UrlOpener,
    private val balanceHidingSettings: GetBalanceHidingSettingsUseCase,
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
        modelScope.launch {
            val userWalletId = params.userWalletId
            val userWallet = getUserWalletsUseCase.invokeSync()
                .firstOrNull { it.walletId == userWalletId } ?: return@launch
            val walletMetaInfo = getWalletMetaInfoUseCase.invoke(
                userWallet.requireColdWallet().scanResponse,
            ).getOrNull() ?: return@launch

            sendFeedbackEmailUseCase.invoke(
                FeedbackEmailType.Visa.DisputeV2(
                    item = params.transaction,
                    walletMetaInfo = walletMetaInfo,
                ),
            )
        }
    }
}