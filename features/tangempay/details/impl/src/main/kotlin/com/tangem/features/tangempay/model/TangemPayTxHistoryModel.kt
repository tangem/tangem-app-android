package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.features.tangempay.components.txHistory.DefaultTangemPayTxHistoryComponent
import com.tangem.features.tangempay.components.txHistory.PreviewTangemPayTxHistoryComponent
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayTxHistoryModel @Inject constructor(
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: DefaultTangemPayTxHistoryComponent.Params = paramsContainer.require()

    val uiState: StateFlow<TxHistoryUM>
        field = MutableStateFlow(getInitialState())

    init {
        handleBalanceHiding()
        subscribeToUiItemChanges()
    }

    @Suppress("MagicNumber")
    private fun subscribeToUiItemChanges() {
        modelScope.launch {
            Timber.d("subscribeToUiItemChanges: ${params.userWalletId}")
            delay(2000)
            uiState.update { PreviewTangemPayTxHistoryComponent.contentUM }
        }
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .onEach { uiState.update { state -> state.copySealed(isBalanceHidden = it.isBalanceHidden) } }
            .launchIn(modelScope)
    }

    private fun onExploreClick() {
        Timber.d("onExploreClick: open explorer")
    }

    private fun getInitialState(): TxHistoryUM {
        return TxHistoryUM.Loading(isBalanceHidden = true, onExploreClick = ::onExploreClick)
    }
}