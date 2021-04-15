package com.tangem.tap.domain.tokens

import android.app.Application
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.common.extensions.readJsonFileToString
import com.tangem.tap.network.createMoshi

class CurrenciesRepository(val context: Application) {
    private val moshi = createMoshi()
    private val tokensAdapter: JsonAdapter<List<TokenDao>> = moshi.adapter(
            Types.newParameterizedType(List::class.java, TokenDao::class.java)
    )
    private val blockchainsAdapter: JsonAdapter<List<Blockchain>> = moshi.adapter(
            Types.newParameterizedType(List::class.java, Blockchain::class.java)
    )

    fun loadCardCurrencies(cardId: String): CardCurrencies? {
        val blockchains = loadSavedBlockchains(cardId)
        if (blockchains.isEmpty()) return null

        return CardCurrencies(loadSavedTokens(cardId), blockchains)
    }

    fun saveCardCurrencies(cardId: String, currencies: CardCurrencies) {
        saveTokens(cardId, currencies.tokens)
        saveBlockchains(cardId, currencies.blockchains)
    }

    fun saveAddedToken(cardId: String, token: Token) {
        val tokens = loadSavedTokens(cardId) + token
        saveTokens(cardId, tokens)
    }

    fun saveAddedBlockchain(cardId: String, blockchain: Blockchain) {
        val blockchains = loadSavedBlockchains(cardId) + blockchain
        saveBlockchains(cardId, blockchains)
    }

    fun removeToken(cardId: String, token: Token) {
        val tokens = loadSavedTokens(cardId).filterNot { it == token }
        saveTokens(cardId, tokens)
    }

    fun removeBlockchain(cardId: String, blockchain: Blockchain) {
        val blockchains = loadSavedBlockchains(cardId).filterNot { it == blockchain }
        saveBlockchains(cardId, blockchains)
    }

    private fun loadSavedTokens(cardId: String): List<Token> {
        return try {
            val json = context.readFileText(getFileNameForTokens(cardId))
            tokensAdapter.fromJson(json)!!.map { it.toToken() }
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun saveTokens(cardId: String, tokens: List<Token>) {
        val json = tokensAdapter.toJson(tokens.map { TokenDao.fromToken(it) })
        context.rewriteFile(json, getFileNameForTokens(cardId))
    }

    private fun loadSavedBlockchains(cardId: String): List<Blockchain> {
        return try {
            val json = context.readFileText(getFileNameForBlockchains(cardId))
            blockchainsAdapter.fromJson(json) ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun saveBlockchains(cardId: String, blockchains: List<Blockchain>) {
        val json = blockchainsAdapter.toJson(blockchains)
        context.rewriteFile(json, getFileNameForBlockchains(cardId))
    }

    private fun Context.readFileText(fileName: String): String =
            this.openFileInput(fileName).bufferedReader().readText()

    private fun Context.rewriteFile(content: String, fileName: String) {
        this.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray(), 0, content.length)
        }
    }

    fun getPopularTokens(): List<Token> {
        val json = context.assets.readJsonFileToString(POPULAR_TOKENS_FILE_NAME)
        return tokensAdapter.fromJson(json)!!.map { it.toToken() }
    }

    fun getBlockchains(): List<Blockchain> {
        return listOf(
                Blockchain.Bitcoin, Blockchain.BitcoinCash, Blockchain.Binance, Blockchain.Litecoin,
                Blockchain.XRP, Blockchain.Tezos,
                Blockchain.Ethereum, Blockchain.RSK)
    }

    companion object {
        private const val POPULAR_TOKENS_FILE_NAME = "erc20_tokens"
        private const val FILE_NAME_PREFIX_TOKENS = "tokens"
        private const val FILE_NAME_PREFIX_BLOCKCHAINS = "blockchains"
        fun getFileNameForTokens(cardId: String): String = "${FILE_NAME_PREFIX_TOKENS}_$cardId"
        fun getFileNameForBlockchains(cardId: String): String = "${FILE_NAME_PREFIX_BLOCKCHAINS}_$cardId"
    }
}


@JsonClass(generateAdapter = true)
data class TokenDao(
        val name: String,
        val symbol: String,
        val contractAddress: String,
        val decimalCount: Int
) {
    fun toToken(): Token {
        return Token(name = name, symbol = symbol, contractAddress = contractAddress, decimals = decimalCount)
    }

    companion object {
        fun fromToken(token: Token): TokenDao {
            return TokenDao(name = token.name, symbol = token.symbol, contractAddress = token.contractAddress, decimalCount = token.decimals)

        }
    }
}

@JsonClass(generateAdapter = true)
data class CardCurrenciesDao(
        val tokens: List<TokenDao>,
        val blockchains: List<Blockchain>
) {
    fun toCardCurrencies(): CardCurrencies {
        return CardCurrencies(tokens = tokens.map { it.toToken() }, blockchains = blockchains)
    }

    companion object {
        fun fromCardCurrencies(cardCurrencies: CardCurrencies): CardCurrenciesDao {
            return CardCurrenciesDao(tokens = cardCurrencies.tokens.map { TokenDao.fromToken(it) }, blockchains = cardCurrencies.blockchains)
        }
    }
}

data class CardCurrencies(
        val tokens: List<Token>,
        val blockchains: List<Blockchain>
)