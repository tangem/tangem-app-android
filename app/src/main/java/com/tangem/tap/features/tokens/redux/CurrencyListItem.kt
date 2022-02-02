package com.tangem.tap.features.tokens.redux

import androidx.annotation.StringRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.wallet.R

sealed class CurrencyListItem {
    var isAdded: Boolean = false
    var isLocked: Boolean = false

    data class TokenListItem(val token: Token) : CurrencyListItem()
    data class BlockchainListItem(val blockchain: Blockchain) : CurrencyListItem()

    data class TitleListItem(
        @StringRes val titleResId: Int,
        var isContentShown: Boolean = true,
        val blockchain: Blockchain? = null,
    ) : CurrencyListItem()

    companion object {
        fun createListOfCurrencies(
            blockchains: List<Blockchain>,
            tokens: List<Token>,
        ): List<CurrencyListItem> {
            return createBlockchainList(blockchains) +
                createTokensList(R.string.add_tokens_subtitle_ethereum_tokens, Blockchain.Ethereum, tokens) +
                createTokensList(R.string.add_tokens_subtitle_bsc_tokens, Blockchain.BSC, tokens) +
                createTokensList(R.string.add_tokens_subtitle_binance_tokens, Blockchain.Binance, tokens) +
                createTokensList(R.string.add_tokens_subtitle_avalanche_tokens, Blockchain.Avalanche, tokens)
        }

        private fun createBlockchainList(blockchains: List<Blockchain>): List<CurrencyListItem> {
            val blockchainsTitle = R.string.add_tokens_subtitle_blockchains
            return listOf(TitleListItem(blockchainsTitle)) + blockchains.map { BlockchainListItem(it) }
        }

        private fun createTokensList(
            @StringRes titleResId: Int,
            blockchain: Blockchain,
            tokens: List<Token>
        ): List<CurrencyListItem> {
            val filteredTokens = tokens.filter {
                it.blockchain == blockchain || it.blockchain == blockchain.getTestnetVersion()
            }
            val tokensListItem = filteredTokens.map { TokenListItem(it) }
            return listOf(TitleListItem(titleResId, blockchain = blockchain)) + tokensListItem
        }
    }
}

fun List<CurrencyListItem>.removeTokensForBlockchain(blockchain: Blockchain): List<CurrencyListItem> {
    return filterNot {
        it is CurrencyListItem.TokenListItem && it.token.blockchain == blockchain
    }
}

fun List<CurrencyListItem>.addTokensForBlockchain(
    blockchain: Blockchain, fullCurrenciesList: List<CurrencyListItem>,
): List<CurrencyListItem> {
    val tokensToAdd = fullCurrenciesList.filter {
        it is CurrencyListItem.TokenListItem && it.token.blockchain == blockchain
    }
    val indexToInsert = indexOfFirst {
        it is CurrencyListItem.TitleListItem && it.blockchain == blockchain
    } + 1
    return this.toMutableList().apply { addAll(indexToInsert, tokensToAdd) }
}

fun List<CurrencyListItem>.toggleHeaderContentShownValue(blockchain: Blockchain) {
    map {
        if (it is CurrencyListItem.TitleListItem && it.blockchain == blockchain) {
            it.isContentShown = !it.isContentShown
        }
    }
}