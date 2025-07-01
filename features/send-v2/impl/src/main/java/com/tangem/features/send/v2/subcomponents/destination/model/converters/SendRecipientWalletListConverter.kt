package com.tangem.features.send.v2.subcomponents.destination.model.converters

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.WALLET_DEFAULT_COUNT
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.WALLET_KEY_TAG
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.emptyListState
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationRecipientListUM
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationWalletUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class SendRecipientWalletListConverter(
    private val senderAddress: String?,
    private val isUtxoConsolidationAvailable: Boolean,
) :
    Converter<List<DestinationWalletUM?>, PersistentList<DestinationRecipientListUM>> {
    override fun convert(value: List<DestinationWalletUM?>): PersistentList<DestinationRecipientListUM> {
        return value.filterWallets().ifEmpty {
            emptyListState(WALLET_KEY_TAG, WALLET_DEFAULT_COUNT)
        }
    }

    private fun List<DestinationWalletUM?>.filterWallets(): PersistentList<DestinationRecipientListUM> {
        var walletsCounter = 0

        return this.filterNotNull()
            .filter {
                val isCoin = it.cryptoCurrency is CryptoCurrency.Coin
                val isNotSameAddress = it.address != senderAddress
                val isNotBlankAddress = it.address.isNotBlank()

                isNotBlankAddress && isCoin && (isNotSameAddress || isUtxoConsolidationAvailable)
            }
            .groupBy { item -> item.name }
            .values.map { wallets ->
                val groupedByWallet = wallets.groupBy { it.userWalletId }
                var i = 0
                groupedByWallet
                    .flatMap { item ->
                        item.value.map { wallet ->
                            val name = if (groupedByWallet.size > 1) {
                                "${wallet.name} ${++i}"
                            } else {
                                wallet.name
                            }

                            DestinationRecipientListUM(
                                id = "${WALLET_KEY_TAG}${walletsCounter++}",
                                title = stringReference(wallet.address),
                                subtitle = stringReference(name),
                                address = wallet.address,
                                userWalletId = wallet.userWalletId,
                                network = wallet.cryptoCurrency.network,
                            )
                        }
                    }
            }
            .flatten()
            .toPersistentList()
    }
}