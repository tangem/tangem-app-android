package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.subcomponents.destination.model.converters.SendRecipientHistoryListConverter
import com.tangem.features.send.v2.subcomponents.destination.model.converters.SendRecipientWalletListConverter
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.utils.transformer.Transformer

@Suppress("LongParameterList")
internal class SendDestinationRecentListTransformer(
    private val senderAddress: String?,
    private val cryptoCurrency: CryptoCurrency,
    private val isSelfSendAvailable: Boolean,
    private val destinationWalletList: List<DestinationWalletUM>,
    private val txHistoryList: List<TxInfo>,
    private val isAccountsMode: Boolean,
) : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        return state.copy(
            isAccountsMode = isAccountsMode,
            wallets = SendRecipientWalletListConverter(
                senderAddress = senderAddress,
                isSelfSendAvailable = isSelfSendAvailable,
                isAccountsMode = isAccountsMode,
            ).convert(destinationWalletList),
            recent = SendRecipientHistoryListConverter(
                cryptoCurrency = cryptoCurrency,
            ).convert(txHistoryList),
        )
    }
}