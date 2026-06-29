package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.features.txhistory.component.TxHistoryDetailsComponent
import com.tangem.features.txhistory.converter.TxHistoryInfoToTxHistoryDetailsUMConverter
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Stable
@ModelScoped
internal class TxHistoryDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val urlOpener: UrlOpener,
    multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: TxHistoryDetailsComponent.Params = paramsContainer.require()

    /** Own deposit addresses for the viewed currency's network — drives the own-vs-external transfer title. */
    private val ownAddressesFlow: Flow<Set<String>> = multiAccountStatusListSupplier()
        .map { lists -> buildOwnAccountAddressMap(lists, params.currency.network.id.rawId).keys }
        .distinctUntilChanged()

    val uiState: StateFlow<TxHistoryDetailsUM?> = combine(
        params.txHistoryInfo,
        ownAddressesFlow,
    ) { txInfo, ownAddresses ->
        TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = params.currency,
            onCopyAddress = ::onCopyAddress,
            onGoToProvider = urlOpener::openUrl,
            ownAddresses = ownAddresses,
        ).convert(txInfo)
    }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = null)

    /** Copies a counterparty address to the clipboard — wired into the detail card's copy button via the converter. */
    private fun onCopyAddress(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
    }
}