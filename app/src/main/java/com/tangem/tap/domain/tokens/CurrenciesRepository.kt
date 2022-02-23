package com.tangem.tap.domain.tokens

import android.app.Application
import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.FirmwareVersion
import com.tangem.tap.common.extensions.appendIf
import com.tangem.tap.common.extensions.readJsonFileToString
import com.tangem.tap.domain.extensions.getCustomIconUrl
import com.tangem.tap.domain.extensions.setCustomIconUrl
import com.tangem.tap.network.createMoshi

class CurrenciesRepository(val context: Application) {
    private val moshi = createMoshi()
    private val blockchainsAdapter: JsonAdapter<List<Blockchain>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, Blockchain::class.java)
    )
    private val tokensAdapter: JsonAdapter<List<TokenDao>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, TokenDao::class.java)
    )
    private val obsoleteTokensAdapter: JsonAdapter<List<ObsoleteTokenDao>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, ObsoleteTokenDao::class.java)
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
        saveTokens(cardId, loadSavedTokens(cardId) + tokens.distinct())
    }

    fun saveAddedBlockchain(cardId: String, blockchain: Blockchain) {
        val blockchains = loadSavedBlockchains(cardId) + blockchain
        saveBlockchains(cardId, blockchains.distinct())
    }

    fun removeToken(cardId: String, token: Token) {
        val tokens = loadSavedTokens(cardId).filterNot { it == token }
        saveTokens(cardId, tokens)
    }

    fun removeBlockchain(cardId: String, blockchain: Blockchain) {
        val blockchains = loadSavedBlockchains(cardId).filterNot { it == blockchain }
        saveBlockchains(cardId, blockchains)
    }

    fun removeCurrencies(cardId: String) {
        saveTokens(cardId, emptyList())
        saveBlockchains(cardId, emptyList())
    }

    private fun loadSavedTokens(cardId: String): List<Token> {
        val json = try {
            context.readFileText(getFileNameForTokens(cardId))
        } catch (exception: Exception) {
            return emptyList()
        }

        return try {
            tokensAdapter.fromJson(json)!!.map { it.toToken() }.distinct()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun saveTokens(cardId: String, tokens: List<Token>) {
        val json = tokensAdapter.toJson(tokens.distinct().map { TokenDao.fromToken(it) })
        context.rewriteFile(json, getFileNameForTokens(cardId))
    }

    private fun loadSavedBlockchains(cardId: String): List<Blockchain> {
        return try {
            val json = context.readFileText(getFileNameForBlockchains(cardId))
            blockchainsAdapter.fromJson(json)?.distinct() ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun saveBlockchains(cardId: String, blockchains: List<Blockchain>) {
        val json = blockchainsAdapter.toJson(blockchains.distinct())
        context.rewriteFile(json, getFileNameForBlockchains(cardId))
    }

    private fun Context.readFileText(fileName: String): String =
        this.openFileInput(fileName).bufferedReader().readText()

    private fun Context.rewriteFile(content: String, fileName: String) {
        this.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray(), 0, content.length)
        }
    }

    fun getSupportedTokens(isTestNet: Boolean = false): List<Token> {
        val supportedTokens = if (isTestNet) {
            getSupportedTokens().mapNotNull { it.getTestnetVersion() }
        } else {
            getSupportedTokens()
        }

        val blockchainTokens = supportedTokens.map {
            fromJsonToTokensDao(loadTokensJson(it), it)
        }
        val tokens = blockchainTokens.flatten().map { it.toToken() }
        return tokens
    }

    private fun loadTokensJson(blockchain: Blockchain): String {
        val fileName = getFileName(blockchain)
        return context.assets.readJsonFileToString(fileName)
    }

    private fun getFileName(blockchain: Blockchain): String {
        return StringBuilder().apply {
            append(blockchain.id.toLowerCase().replace("/test", ""))
            append("_tokens")
            appendIf("_testnet") { blockchain.isTestnet() }
        }.toString()
    }

    private fun fromJsonToTokensDao(tokenJson: String, blockchain: Blockchain): List<TokenDao> {
        return obsoleteTokensAdapter.fromJson(tokenJson)!!.map { it.toTokenDao(blockchain) }
    }

    private fun getSupportedTokens(): List<Blockchain> {
        return listOf(
            Blockchain.Avalanche,
            Blockchain.Binance,
            Blockchain.BSC,
            Blockchain.Ethereum,
            Blockchain.Polygon,
            Blockchain.Solana,
        )
    }

    fun getBlockchains(
        cardFirmware: FirmwareVersion,
        isTestNet: Boolean = false
    ): List<Blockchain> {
        val blockchains = if (cardFirmware < FirmwareVersion.MultiWalletAvailable) {
            Blockchain.secp256k1Blockchains(isTestNet)
        } else {
            Blockchain.secp256k1Blockchains(isTestNet) + Blockchain.ed25519OnlyBlockchains(isTestNet)
        }
        return excludeUnsupportedBlockchains(blockchains)
    }

    //TODO: move to the App settings
    private fun excludeUnsupportedBlockchains(blockchains: List<Blockchain>): List<Blockchain> {
        return blockchains.toMutableList().apply {
            removeAll(listOf(
                Blockchain.Fantom, Blockchain.FantomTestnet
            ))
        }
    }

    companion object {
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
    @Json(name = "blockchain")
    val blockchainDao: BlockchainDao,
    val customIconUrl: String? = null,
    val type: String? = null
) {
    fun toToken(): Token {
        return Token(
            name = name,
            symbol = symbol,
            contractAddress = contractAddress,
            decimals = decimalCount,
            blockchain = blockchainDao.toBlockchain()
        ).apply {
            customIconUrl?.let { this.setCustomIconUrl(it) }
        }
    }

    companion object {
        fun fromToken(token: Token): TokenDao {
            return TokenDao(
                name = token.name,
                symbol = token.symbol,
                contractAddress = token.contractAddress,
                decimalCount = token.decimals,
                blockchainDao = BlockchainDao.fromBlockchain(token.blockchain),
                customIconUrl = token.getCustomIconUrl()
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class BlockchainDao(
    @Json(name = "key")
    val name: String,
    @Json(name = "testnet")
    val isTestNet: Boolean
) {
    fun toBlockchain(): Blockchain {
        val blockchain = Blockchain.values().find { it.name.lowercase() == name.lowercase() }
            ?: throw Exception("Invalid BlockchainDao")
        return if (!isTestNet) blockchain else blockchain.getTestnetVersion()
            ?: throw Exception("Invalid BlockchainDao")
    }

    companion object {
        fun fromBlockchain(blockchain: Blockchain): BlockchainDao {
            val name = blockchain.name.removeSuffix("Testnet").lowercase()
            return BlockchainDao(name, blockchain.isTestnet())
        }
    }
}

@JsonClass(generateAdapter = true)
data class ObsoleteTokenDao(
    val name: String,
    val symbol: String,
    val contractAddress: String,
    val decimalCount: Int,
    val customIconUrl: String?,
) {
    fun toTokenDao(blockchain: Blockchain): TokenDao {
        return TokenDao(
            name = name,
            symbol = symbol,
            contractAddress = contractAddress,
            decimalCount = decimalCount,
            blockchainDao = BlockchainDao.fromBlockchain(blockchain),
            customIconUrl = customIconUrl
        )
    }
}

@JsonClass(generateAdapter = true)
data class CardCurrenciesDao(
    val tokens: List<TokenDao>,
    val blockchains: List<Blockchain>,
) {
    fun toCardCurrencies(): CardCurrencies {
        return CardCurrencies(
            tokens = tokens.map { it.toToken() }.distinct(),
            blockchains = blockchains
        )
    }

    companion object {
        fun fromCardCurrencies(cardCurrencies: CardCurrencies): CardCurrenciesDao {
            return CardCurrenciesDao(
                tokens = cardCurrencies.tokens.map { TokenDao.fromToken(it) }.distinct(),
                blockchains = cardCurrencies.blockchains
            )
        }
    }
}

data class CardCurrencies(
    val tokens: List<Token>,
    val blockchains: List<Blockchain>,
)