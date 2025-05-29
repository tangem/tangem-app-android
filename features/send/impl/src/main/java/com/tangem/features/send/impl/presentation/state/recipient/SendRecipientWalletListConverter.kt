package com.tangem.features.send.impl.presentation.state.recipient

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.domain.SendRecipientListContent
import com.tangem.features.send.impl.presentation.state.recipient.utils.WALLET_DEFAULT_COUNT
import com.tangem.features.send.impl.presentation.state.recipient.utils.WALLET_KEY_TAG
import com.tangem.features.send.impl.presentation.state.recipient.utils.emptyListState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal class SendRecipientWalletListConverter(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val isUtxoConsolidationAvailableProvider: Provider<Boolean>,
) :
    Converter<List<AvailableWallet?>, PersistentList<SendRecipientListContent>> {
    override fun convert(value: List<AvailableWallet?>): PersistentList<SendRecipientListContent> {
        return value.filterWallets().ifEmpty {
            emptyListState(WALLET_KEY_TAG, WALLET_DEFAULT_COUNT)
        }
    }

    private fun List<AvailableWallet?>.filterWallets(): PersistentList<SendRecipientListContent> {
        var walletsCounter = 0
        val currentAddress: String = runCatching {
            cryptoCurrencyStatusProvider().value.networkAddress?.defaultAddress?.value
        }.getOrNull().orEmpty()

        return this.filterNotNull()
            .filter {
                val isCoin = it.cryptoCurrency is CryptoCurrency.Coin
                val isNotSameAddress = it.address != currentAddress
                val isNotBlankAddress = it.address.isNotBlank()

                isNotBlankAddress && isCoin && (isNotSameAddress || isUtxoConsolidationAvailableProvider())
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

                            SendRecipientListContent(
                                id = "${WALLET_KEY_TAG}${walletsCounter++}",
                                title = TextReference.Str(wallet.address),
                                subtitle = TextReference.Str(name),
                            )
                        }
                    }
            }
            .flatten()
            .toPersistentList()
    }
}