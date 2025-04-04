package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.destination.model.converters.SendRecipientHistoryListConverter
import com.tangem.features.send.v2.subcomponents.destination.model.converters.SendRecipientWalletListConverter
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.utils.transformer.Transformer

internal class SendDestinationRecentListTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val isUtxoConsolidationAvailable: Boolean,
    private val destinationWalletList: List<DestinationWalletUM>,
    private val txHistoryList: List<TxHistoryItem>,
) : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        return state.copy(
            wallets = SendRecipientWalletListConverter(
                cryptoCurrencyStatus,
                isUtxoConsolidationAvailable,
            ).convert(destinationWalletList),
            recent = SendRecipientHistoryListConverter(
                cryptoCurrency = cryptoCurrencyStatus.currency,
            ).convert(txHistoryList),
        )
    }
}
