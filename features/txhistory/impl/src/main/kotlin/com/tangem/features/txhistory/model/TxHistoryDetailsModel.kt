package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.features.txhistory.component.TxHistoryDetailsComponent
import com.tangem.features.txhistory.converter.TxHistoryInfoToTxHistoryDetailsUMConverter
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Stable
@ModelScoped
internal class TxHistoryDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val urlOpener: UrlOpener,
    ownerLookupProducer: TxHistoryOwnerLookupProducer,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: TxHistoryDetailsComponent.Params = paramsContainer.require()

    val uiState: StateFlow<TxHistoryDetailsUM?> = combine(
        params.txHistoryInfo,
        ownerLookupProducer(),
    ) { txInfo, lookup ->
        TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = params.currency,
            onCopyAddress = ::onCopyAddress,
            onGoToProvider = urlOpener::openUrl,
            lookup = lookup,
        ).convert(txInfo)
    }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = null)

    /** Copies a counterparty address to the clipboard — wired into the detail card's copy button via the converter. */
    private fun onCopyAddress(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
    }
}