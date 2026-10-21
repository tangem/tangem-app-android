package com.tangem.features.txhistory.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.domain.txhistory.model.idToCopy
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.txhistory.analytics.TxHistoryAnalyticsEvent
import com.tangem.features.txhistory.analytics.toAnalyticsType
import com.tangem.features.txhistory.converter.buildDetailsMenu
import com.tangem.features.txhistory.converter.copyIdLabels
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.ImmutableList
import javax.inject.Inject

@ModelScoped
internal class TxDetailsMenuActions @Inject constructor(
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
) {

    fun menu(
        txInfo: TxHistoryInfo,
        currency: CryptoCurrency,
        onAddContact: (() -> Unit)?,
    ): ImmutableList<TxHistoryDetailsUM.MenuItemUM> = buildDetailsMenu(
        onCopyTxId = copyTxIdAction(txInfo = txInfo, currency = currency),
        copyIdTitle = resourceReference(txInfo.copyIdLabels.title),
        shareText = null,
        onShare = {},
        onExplore = exploreAction(txInfo = txInfo, currency = currency),
        onAddContact = onAddContact,
    )

    fun copyTxIdAction(txInfo: TxHistoryInfo, currency: CryptoCurrency): (() -> Unit)? {
        val id = txInfo.idToCopy.ifBlank { null } ?: return null

        return { copyTxId(id = id, txInfo = txInfo, currency = currency) }
    }

    fun exploreAction(txInfo: TxHistoryInfo, currency: CryptoCurrency): (() -> Unit)? {
        val txHash = txInfo.explorerHash?.ifBlank { null } ?: return null

        return { explore(txHash = txHash, txInfo = txInfo, currency = currency) }
    }

    private fun copyTxId(id: String, txInfo: TxHistoryInfo, currency: CryptoCurrency) {
        analyticsEventHandler.send(
            TxHistoryAnalyticsEvent.ButtonCopyTransactionId(
                token = currency.symbol,
                blockchain = currency.network.name,
                type = txInfo.toAnalyticsType(),
            ),
        )
        clipboardManager.setText(text = id, isSensitive = false)
        uiMessageSender.send(
            SnackbarMessage(
                message = resourceReference(txInfo.copyIdLabels.copied),
                startIconId = R.drawable.ic_check_24,
            ),
        )
    }

    private fun explore(txHash: String, txInfo: TxHistoryInfo, currency: CryptoCurrency) {
        analyticsEventHandler.send(
            TokenScreenAnalyticsEvent.ButtonExplore(
                token = currency.symbol,
                source = TokenScreenAnalyticsEvent.ButtonExplore.ExploreActionSource.TransactionDetail,
                blockchain = currency.network.name,
                type = txInfo.toAnalyticsType(),
            ),
        )
        getExplorerTransactionUrlUseCase(txHash = txHash, currency = currency).fold(
            ifLeft = { TangemLogger.e(it.toString()) },
            ifRight = { urlOpener.openUrl(url = it) },
        )
    }
}