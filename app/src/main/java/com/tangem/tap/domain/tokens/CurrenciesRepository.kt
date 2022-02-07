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
import com.tangem.common.json.MoshiJsonConverter
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
            try {
                obsoleteTokensAdapter.fromJson(json)!!.map { it.toToken() }
            } catch (exception: Exception) {
                emptyList()
            }
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

    fun getPopularTokens(isTestNet: Boolean = false): List<Token> {
        val ethereumTokensFileName =
            if (isTestNet) ETHEREUM_TESTNET_TOKENS_FILE_NAME else ETHEREUM_TOKENS_FILE_NAME
        val bscTokensFileName =
            if (isTestNet) BSC_TESTNET_TOKENS_FILE_NAME else BSC_TOKENS_FILE_NAME
        val binanceTokensFileName =
            if (isTestNet) BINANCE_TESTNET_TOKENS_FILE_NAME else BINANCE_TOKENS_FILE_NAME
        val avalancheTokensFileName =
            if (isTestNet) AVALANCHE_TESTNET_TOKENS_FILE_NAME else AVALANCHE_TOKENS_FILE_NAME

        val ethereumTokensJson = context.assets.readJsonFileToString(ethereumTokensFileName)
        val bscTokensJson = context.assets.readJsonFileToString(bscTokensFileName)
        val binanceTokensJson = context.assets.readJsonFileToString(binanceTokensFileName)
        val avalancheTokensJson = context.assets.readJsonFileToString(avalancheTokensFileName)

        return tokensAdapter.fromJson(ethereumTokensJson)!!.map { it.toToken() } +
            tokensAdapter.fromJson(bscTokensJson)!!.map { it.toToken() } +
            tokensAdapter.fromJson(binanceTokensJson)!!.mapNotNull {
                // temporary exclude Binance BEP-8 tokens
                if (it.type != null && it.type == BINANCE_TOKEN_TYPE_BEP8) null else it.toToken()
            } +
            tokensAdapter.fromJson(avalancheTokensJson)!!.map { it.toToken() }
    }

    fun getBlockchains(
        cardFirmware: FirmwareVersion,
        isTestNet: Boolean = false
    ): List<Blockchain> {
        return if (cardFirmware < FirmwareVersion.MultiWalletAvailable) {
            Blockchain.secp256k1Blockchains(isTestNet)
        } else {
            Blockchain.secp256k1Blockchains(isTestNet) + Blockchain.ed25519OnlyBlockchains(isTestNet)
        }
    }

    companion object {
        private const val ETHEREUM_TOKENS_FILE_NAME = "ethereum_tokens"
        private const val ETHEREUM_TESTNET_TOKENS_FILE_NAME = "ethereum_tokens_testnet"
        private const val BSC_TOKENS_FILE_NAME = "bsc_tokens"
        private const val BSC_TESTNET_TOKENS_FILE_NAME = "bsc_tokens_testnet"
        private const val BINANCE_TOKENS_FILE_NAME = "binance_tokens"
        private const val BINANCE_TESTNET_TOKENS_FILE_NAME = "binance_tokens_testnet"
        private const val AVALANCHE_TOKENS_FILE_NAME = "avalanche_tokens"
        private const val AVALANCHE_TESTNET_TOKENS_FILE_NAME = "avalanche_tokens_testnet"

        private const val FILE_NAME_PREFIX_TOKENS = "tokens"
        private const val FILE_NAME_PREFIX_BLOCKCHAINS = "blockchains"

        private const val BINANCE_TOKEN_TYPE_BEP8 = "bep8"

        fun getFileNameForTokens(cardId: String): String = "${FILE_NAME_PREFIX_TOKENS}_$cardId"
        fun getFileNameForBlockchains(cardId: String): String =
            "${FILE_NAME_PREFIX_BLOCKCHAINS}_$cardId"

        fun injectDaoBlockchain(context: Context, tokenFileName: String, blockchain: Blockchain): String? {
            val tokenJson = context.assets.readJsonFileToString(tokenFileName)
            val converter = MoshiJsonConverter.default()
            val rawTokensList = converter.fromJson<List<MutableMap<String, Any>>>(tokenJson) ?: return null

            rawTokensList.forEach {
                it["blockchain"] = BlockchainDao.fromBlockchain(blockchain)
            }

            return converter.toJson(rawTokensList)
        }
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
) {
    fun toToken(): Token {
        return Token(
            name = name,
            symbol = symbol,
            contractAddress = contractAddress,
            decimals = decimalCount
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