package com.tangem.feature.wallet.presentation.wallet.utils

import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.model.TokenStatus
import com.tangem.feature.wallet.presentation.wallet.state.WalletContentItemState.MultiCurrencyItem
import com.tangem.feature.wallet.presentation.wallet.utils.LoadingItemsProvider.getLoadingMultiCurrencyTokens
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

internal class TokenListToContentItemsConverter(
    isWalletContentHidden: Boolean,
    fiatCurrencyCode: String,
    fiatCurrencySymbol: String,
) : Converter<TokenList, ImmutableList<MultiCurrencyItem>> {

    private val tokenStatusConverter = TokenStatusToTokenItemConverter(
        isWalletContentHidden,
        fiatCurrencyCode,
        fiatCurrencySymbol,
    )

    override fun convert(value: TokenList): ImmutableList<MultiCurrencyItem> {
        return when (value) {
            is TokenList.GroupedByNetwork -> value.mapToMultiCurrencyItems()
            is TokenList.Ungrouped -> value.mapToMultiCurrencyItems()
            is TokenList.NotInitialized -> getLoadingMultiCurrencyTokens()
        }
    }

    private fun TokenList.GroupedByNetwork.mapToMultiCurrencyItems(): PersistentList<MultiCurrencyItem> {
        return groups.fold(initial = persistentListOf()) { acc, group ->
            acc.mutate { it.addGroup(group) }
        }
    }

    private fun TokenList.Ungrouped.mapToMultiCurrencyItems(): PersistentList<MultiCurrencyItem> {
        return tokens.fold(initial = persistentListOf()) { acc, token ->
            acc.mutate { it.addToken(token) }
        }
    }

    private fun MutableList<MultiCurrencyItem>.addGroup(group: NetworkGroup): List<MultiCurrencyItem> {
        this.add(MultiCurrencyItem.NetworkGroupTitle(group.network.name))

        group.tokens.forEach { token ->
            this.addToken(token)
        }

        return this
    }

    private fun MutableList<MultiCurrencyItem>.addToken(token: TokenStatus): List<MultiCurrencyItem> {
        val tokenItemState = tokenStatusConverter.convert(token)

        this.add(MultiCurrencyItem.Token(tokenItemState))

        return this
    }
}
