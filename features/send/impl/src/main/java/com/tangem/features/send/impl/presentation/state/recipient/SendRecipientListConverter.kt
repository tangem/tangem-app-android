package com.tangem.features.send.impl.presentation.state.recipient

import androidx.paging.*
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toDateFormat
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.toFormattedCurrencyString
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.update

internal class SendRecipientListConverter(
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) {

    fun convert(wallets: List<AvailableWallet?>, txHistory: PagingData<TxHistoryItem>, txHistoryCount: Int) {
        val filteredWallets = wallets.filterNotNull()
            .groupBy { item -> item.name }
            .values.flatten()
            .mapIndexed { index, item ->
                item.copy(
                    name = "${item.name} ${index.inc()}",
                )
            }

        val walletsItem = getWalletItems(filteredWallets, txHistoryCount)

        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        currentStateProvider().recipientList.update {
            if (txHistoryCount == 0) {
                PagingData.from(listOf(walletsItem))
            } else {
                txHistory.filter { item ->
                    val isTransfer = item.type == TxHistoryItem.TransactionType.Transfer
                    val isNotContract = item.interactionAddressType is TxHistoryItem.InteractionAddressType.User
                    val isSingleAddress = if (item.isOutgoing) {
                        item.destinationType is TxHistoryItem.DestinationType.Single
                    } else {
                        item.sourceType is TxHistoryItem.SourceType.Single
                    }
                    isTransfer && isSingleAddress && isNotContract
                }.map<TxHistoryItem, SendRecipientListContent> { tx ->
                    SendRecipientListContent.Item(
                        id = tx.txHash,
                        title = tx.extractAddress(),
                        subtitle = stringReference(tx.getAmount(cryptoCurrency).trim()),
                        timestamp = tx.extractTimestamp(),
                        subtitleEndOffset = cryptoCurrency.symbol.length,
                        subtitleIconRes = tx.extractIconRes(),
                    )
                }.insertWallets(walletsItem)
            }
        }
    }

    private fun PagingData<SendRecipientListContent>.insertWallets(
        wallets: SendRecipientListContent.Wallets,
    ): PagingData<SendRecipientListContent> {
        return insertSeparators(terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE) { before, after ->
            return@insertSeparators when {
                before == null && after is SendRecipientListContent.Item -> wallets
                else -> null
            }
        }
    }

    private fun getWalletItems(wallets: List<AvailableWallet>, txHistoryCount: Int): SendRecipientListContent.Wallets {
        return SendRecipientListContent.Wallets(
            wallets.map {
                SendRecipientListContent.Item(
                    id = it.address,
                    title = TextReference.Str(it.address),
                    subtitle = TextReference.Str(it.name),
                )
            }.toPersistentList(),
            isWalletsOnly = txHistoryCount == 0,
        )
    }

    private fun TxHistoryItem.extractAddress(): TextReference = if (isOutgoing) {
        when (val destination = destinationType) {
            is TxHistoryItem.DestinationType.Multiple -> TextReference.Res(
                R.string.transaction_history_multiple_addresses,
            )
            is TxHistoryItem.DestinationType.Single -> TextReference.Str(destination.addressType.address)
        }
    } else {
        when (val source = sourceType) {
            is TxHistoryItem.SourceType.Multiple -> TextReference.Res(R.string.transaction_history_multiple_addresses)
            is TxHistoryItem.SourceType.Single -> TextReference.Str(source.address)
        }
    }

    private fun TxHistoryItem.extractIconRes() = if (isOutgoing) {
        R.drawable.ic_arrow_up_24
    } else {
        R.drawable.ic_arrow_down_24
    }

    private fun TxHistoryItem.getAmount(cryptoCurrency: CryptoCurrency): String {
        return amount.toFormattedCurrencyString(
            currency = cryptoCurrency.symbol,
            decimals = cryptoCurrency.decimals,
        )
    }

    private fun TxHistoryItem.extractTimestamp(): TextReference {
        val date = timestampInMillis.toDateFormat(
            formatter = DateTimeFormatters.dateDDMMYYYY,
        )
        val time = timestampInMillis.toTimeFormat()
        return TextReference.Res(R.string.send_date_format, wrappedList(date, time))
    }
}