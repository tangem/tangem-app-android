package com.tangem.tap.domain.tokens

import android.app.Application
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.commands.common.card.FirmwareVersion
import com.tangem.tap.common.extensions.readJsonFileToString
import com.tangem.tap.network.createMoshi

class CurrenciesRepository(val context: Application) {
    private val moshi = createMoshi()
    private val tokensAdapter: JsonAdapter<Set<TokenDao>> = moshi.adapter(
        Types.newParameterizedType(Set::class.java, TokenDao::class.java)
    )
    private val blockchainsAdapter: JsonAdapter<Set<Blockchain>> = moshi.adapter(
        Types.newParameterizedType(Set::class.java, Blockchain::class.java)
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

    fun saveAddedTokens(cardId: String, tokens: Collection<Token>) {
        saveTokens(cardId, loadSavedTokens(cardId) + tokens)
    }

    fun saveAddedBlockchain(cardId: String, blockchain: Blockchain) {
        val blockchains = loadSavedBlockchains(cardId) + blockchain
        saveBlockchains(cardId, blockchains)
    }

    fun removeToken(cardId: String, token: Token) {
        val tokens = loadSavedTokens(cardId).filterNot { it == token }.toSet()
        saveTokens(cardId, tokens)
    }

    fun removeBlockchain(cardId: String, blockchain: Blockchain) {
        val blockchains = loadSavedBlockchains(cardId).filterNot { it == blockchain }.toSet()
        saveBlockchains(cardId, blockchains)
    }

    private fun loadSavedTokens(cardId: String): Set<Token> {
        return try {
            val json = context.readFileText(getFileNameForTokens(cardId))
            tokensAdapter.fromJson(json)!!.map { it.toToken() }.toSet()
        } catch (exception: Exception) {
            emptySet()
        }
    }

    private fun saveTokens(cardId: String, tokens: Set<Token>) {
        val json = tokensAdapter.toJson(tokens.map { TokenDao.fromToken(it) }.toSet())
        context.rewriteFile(json, getFileNameForTokens(cardId))
    }

    private fun loadSavedBlockchains(cardId: String): Set<Blockchain> {
        return try {
            val json = context.readFileText(getFileNameForBlockchains(cardId))
            blockchainsAdapter.fromJson(json) ?: emptySet()
        } catch (exception: Exception) {
            emptySet()
        }
    }

    private fun saveBlockchains(cardId: String, blockchains: Set<Blockchain>) {
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

    fun getPopularTokens(isTestNet: Boolean = false): List<Token> {
        val fileName = if (isTestNet) TESTNET_TOKENS_FILE_NAME else POPULAR_TOKENS_FILE_NAME
        val json = context.assets.readJsonFileToString(fileName)
        return tokensAdapter.fromJson(json)!!.map { it.toToken() }
    }

    fun getBlockchains(cardFirmware: FirmwareVersion?, isTestNet: Boolean = false): List<Blockchain> {
        return if (cardFirmware == null || cardFirmware.major < 4) {
            Blockchain.secp256k1Blockchains(isTestNet)
        } else {
            Blockchain.secp256k1Blockchains(isTestNet) + Blockchain.ed25519OnlyBlockchains(isTestNet)
        }
    }

    companion object {
        private const val POPULAR_TOKENS_FILE_NAME = "erc20_tokens"
        private const val TESTNET_TOKENS_FILE_NAME = "ethereum_tokens_testnet"
        private const val FILE_NAME_PREFIX_TOKENS = "tokens"
        private const val FILE_NAME_PREFIX_BLOCKCHAINS = "blockchains"

        fun getFileNameForTokens(cardId: String): String = "${FILE_NAME_PREFIX_TOKENS}_$cardId"
        fun getFileNameForBlockchains(cardId: String): String =
            "${FILE_NAME_PREFIX_BLOCKCHAINS}_$cardId"
    }
}


@JsonClass(generateAdapter = true)
data class TokenDao(
    val name: String,
    val symbol: String,
    val contractAddress: String,
    val decimalCount: Int,
) {
    fun toToken(): Token {
        return Token(name = name,
            symbol = symbol,
            contractAddress = contractAddress,
            decimals = decimalCount)
    }

    companion object {
        fun fromToken(token: Token): TokenDao {
            return TokenDao(name = token.name,
                symbol = token.symbol,
                contractAddress = token.contractAddress,
                decimalCount = token.decimals)

        }
    }
}

@JsonClass(generateAdapter = true)
data class CardCurrenciesDao(
    val tokens: Set<TokenDao>,
    val blockchains: Set<Blockchain>,
) {
    fun toCardCurrencies(): CardCurrencies {
        return CardCurrencies(tokens = tokens.map { it.toToken() }.toSet(),
            blockchains = blockchains)
    }

    companion object {
        fun fromCardCurrencies(cardCurrencies: CardCurrencies): CardCurrenciesDao {
            return CardCurrenciesDao(
                tokens = cardCurrencies.tokens.map { TokenDao.fromToken(it) }.toSet(),
                blockchains = cardCurrencies.blockchains
            )
        }
    }
}

data class CardCurrencies(
    val tokens: Set<Token>,
    val blockchains: Set<Blockchain>,
)