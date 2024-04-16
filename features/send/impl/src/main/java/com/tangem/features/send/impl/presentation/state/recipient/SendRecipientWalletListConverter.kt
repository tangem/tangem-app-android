package com.tangem.features.send.impl.presentation.state.recipient

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.recipient.utils.WALLET_DEFAULT_COUNT
import com.tangem.features.send.impl.presentation.state.recipient.utils.WALLET_KEY_TAG
import com.tangem.features.send.impl.presentation.state.recipient.utils.emptyListState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class SendRecipientWalletListConverter :
    Converter<List<AvailableWallet?>, PersistentList<SendRecipientListContent>> {
    override fun convert(value: List<AvailableWallet?>): PersistentList<SendRecipientListContent> {
        return value.filterWallets().ifEmpty {
            emptyListState(WALLET_KEY_TAG, WALLET_DEFAULT_COUNT)
        }
    }

    private fun List<AvailableWallet?>.filterWallets() = this.filterNotNull()
        .groupBy { item -> item.name }
        .values.map {
            it.mapIndexed { index, item ->
                val name = if (it.size > 1) {
                    "${item.name} ${index.inc()}"
                } else {
                    item.name
                }
                SendRecipientListContent(
                    id = "${WALLET_KEY_TAG}$index",
                    title = TextReference.Str(item.address),
                    subtitle = TextReference.Str(name),
                )
            }
        }
        .flatten()
        .toPersistentList()
}