package com.tangem.tap.features.tokens.redux

import androidx.annotation.StringRes
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.wallet.R

sealed class CurrencyListItem {
    data class TokenListItem(val token: Token) : CurrencyListItem()
    data class BlockchainListItem(val blockchain: Blockchain) : CurrencyListItem()
    data class TitleListItem(
        @StringRes val titleResId: Int,
        val isContentShown: Boolean = true,
        val blockchain: Blockchain? = null,
    ) : CurrencyListItem()

    companion object {
        fun createListOfCurrencies(
            blockchains: List<Blockchain>,
            tokens: List<Token>,
        ): List<CurrencyListItem> {
            val blockchainsTitle = R.string.add_tokens_subtitle_blockchains
            val ethereumTokensTitle = R.string.add_tokens_subtitle_ethereum_tokens
            val bscTokensTitle = R.string.add_tokens_subtitle_bsc_tokens
            val binanceTokensTitle = R.string.add_tokens_subtitle_binance_tokens

            val ethereumTokens = tokens.filter { it.blockchain == Blockchain.Ethereum }
            val bscTokens = tokens.filter { it.blockchain == Blockchain.BSC }
            val binanceTokens = tokens.filter { it.blockchain == Blockchain.Binance }
            return listOf(TitleListItem(blockchainsTitle)) +
                    blockchains.map { BlockchainListItem(it) } +
                    listOf(TitleListItem(ethereumTokensTitle, blockchain = Blockchain.Ethereum)) +
                    ethereumTokens.map { TokenListItem(it) } +
                    listOf(TitleListItem(bscTokensTitle, blockchain = Blockchain.BSC)) +
                    bscTokens.map { TokenListItem(it) } +
                    listOf(TitleListItem(binanceTokensTitle, blockchain = Blockchain.Binance)) +
                    binanceTokens.map { TokenListItem(it) }
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

fun List<CurrencyListItem>.toggleHeaderContentShownValue(
    blockchain: Blockchain
): List<CurrencyListItem> {
    val indexOfTitle = indexOfFirst {
        it is CurrencyListItem.TitleListItem && it.blockchain == blockchain
    }
    val title = this[indexOfTitle] as CurrencyListItem.TitleListItem
    val updatedTitle = title.copy(isContentShown = !title.isContentShown)
    return this.toMutableList().apply { set(indexOfTitle, updatedTitle) }
}