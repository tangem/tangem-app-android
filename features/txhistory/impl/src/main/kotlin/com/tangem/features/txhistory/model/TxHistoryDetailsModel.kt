package com.tangem.features.txhistory.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.domain.txhistory.model.idToCopy
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.txhistory.component.TxHistoryDetailsComponent
import com.tangem.features.txhistory.converter.TxHistoryInfoToTxHistoryDetailsUMConverter
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class TxHistoryDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val urlOpener: UrlOpener,
    private val shareManager: ShareManager,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    ownerLookupProducer: TxHistoryOwnerLookupProducer,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: TxHistoryDetailsComponent.Params = paramsContainer.require()

    val uiState: StateFlow<TxHistoryDetailsUM?> = combine(
        params.txHistoryInfo,
        ownerLookupProducer(),
    ) { txInfo, lookup ->
        // No explorer hash (e.g. an express op with no on-chain leg yet, or a blank on-chain hash) → the "Share" and
        // "Explore" rows are dropped; a blank id drops the "Transaction ID" row.
        val explorerHash = txInfo.explorerHash?.ifBlank { null }
        val idToCopy = txInfo.idToCopy.ifBlank { null }
        TxHistoryInfoToTxHistoryDetailsUMConverter(
            currency = params.currency,
            onCopyAddress = ::onCopyAddress,
            onGoToProvider = urlOpener::openUrl,
            onCopyTxId = idToCopy?.let { id -> { onCopyTxId(id) } },
            onShare = explorerHash?.let { hash -> { share(hash) } },
            onExplore = explorerHash?.let { hash -> { explore(hash) } },
            lookup = lookup,
        ).convert(txInfo)
    }
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.WhileSubscribed(), initialValue = null)

    /** Copies a counterparty address to the clipboard — wired into the detail card's copy button via the converter. */
    private fun onCopyAddress(address: String) {
        clipboardManager.setText(text = address, isSensitive = false)
    }

    /** Copies the transaction id to the clipboard — wired into the header menu's "Transaction ID" row. */
    private fun onCopyTxId(id: String) {
        clipboardManager.setText(text = id, isSensitive = false)
    }

    /** Opens the transaction in the blockchain explorer — wired into the header menu's "Explore" row. */
    private fun explore(txHash: String) {
        getExplorerTransactionUrlUseCase(txHash = txHash, currency = params.currency).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { urlOpener.openUrl(url = it) },
        )
    }

    /** Shares the transaction's explorer URL — wired into the header menu's "Share" row. */
    private fun share(txHash: String) {
        getExplorerTransactionUrlUseCase(txHash = txHash, currency = params.currency).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { shareManager.shareText(text = it) },
        )
    }
}