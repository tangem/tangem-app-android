package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.subcomponents.destination.model.converters.SendRecipientHistoryListConverter
import com.tangem.features.send.v2.subcomponents.destination.model.converters.SendRecipientWalletListConverter
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.transformer.Transformer

@Suppress("LongParameterList")
internal class SendDestinationRecentListTransformer(
    private val senderAddress: String?,
    private val cryptoCurrency: CryptoCurrency,
    private val isSelfSendAvailable: Boolean,
    private val destinationWalletList: List<DestinationWalletUM>,
    private val txHistoryList: List<TxInfo>,
    private val account: Account.CryptoPortfolio?,
    private val isAccountsMode: Boolean,
) : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        return state.copy(
            accountTitleUM = if (account != null && isAccountsMode) {
                AccountTitleUM.Account(
                    name = account.accountName.toUM().value,
                    icon = account.icon.toUM(),
                    prefixText = stringReference(StringsSigns.DOT),
                )
            } else {
                AccountTitleUM.Text(TextReference.EMPTY)
            },
            wallets = SendRecipientWalletListConverter(
                senderAddress = senderAddress,
                isSelfSendAvailable = isSelfSendAvailable,
            ).convert(destinationWalletList),
            recent = SendRecipientHistoryListConverter(
                cryptoCurrency = cryptoCurrency,
            ).convert(txHistoryList),
        )
    }
}