package com.tangem.tap.domain.tokens

import android.app.Application
import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.tap.common.extensions.appendIf
import com.tangem.tap.common.extensions.readJsonFileToString
import com.tangem.tap.domain.TapWorkarounds.derivationStyle
import com.tangem.tap.domain.extensions.setCustomIconUrl
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.network.createMoshi
import timber.log.Timber
import java.util.*

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
    private val currenciesAdapter: JsonAdapter<CurrenciesFromJson> =
        moshi.adapter(CurrenciesFromJson::class.java)

    private val blockchainNetworkAdapter: JsonAdapter<List<BlockchainNetwork>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, BlockchainNetwork::class.java))

    fun saveUpdatedCurrency(cardId: String, blockchainNetwork: BlockchainNetwork) {
        var changed = false
        val currencies = loadSavedCurrencies(cardId).map {
            if (it == blockchainNetwork) {
                changed = true
                blockchainNetwork
            } else {
                it
            }
        }
        val updatedCurrencies = if (changed) currencies else currencies + blockchainNetwork
        saveCurrencies(cardId, updatedCurrencies.distinct())
    }

    fun removeToken(cardId: String, token: Token, blockchainNetwork: BlockchainNetwork) {
        val currencies = loadSavedCurrencies(cardId).map {
            if (it == blockchainNetwork) {
                it.copy(tokens = it.tokens.filterNot { it == token })
            } else {
                it
            }
        }
        saveCurrencies(cardId, currencies)
    }

    fun removeBlockchain(cardId: String, blockchainNetwork: BlockchainNetwork) {
        val currencies = loadSavedCurrencies(cardId).filterNot { it == blockchainNetwork }
        saveCurrencies(cardId, currencies)
    }

    fun removeCurrencies(cardId: String) {
        saveCurrencies(cardId, emptyList())
    }

    @Deprecated("Use BlockchainNetwork instead")
    private fun loadSavedTokens(cardId: String): List<TokenDao> {
        val json = try {
            context.readFileText(getFileNameForTokens(cardId))
        } catch (exception: Exception) {
            return emptyList()
        }

        return try {
            tokensAdapter.fromJson(json) ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    @Deprecated("Use BlockchainNetwork instead")
    private fun loadSavedBlockchains(cardId: String): List<Blockchain> {
        return try {
            val json = context.readFileText(getFileNameForBlockchains(cardId))
            blockchainsAdapter.fromJson(json)?.distinct() ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    fun loadSavedCurrencies(
        cardId: String,
        derivationStyle: DerivationStyle? = null
    ): List<BlockchainNetwork> {
        if (DemoHelper.isDemoCardId(cardId)) {
            return loadDemoCurrencies(cardId)
        }
        return try {
            val json = context.readFileText(getFileNameForBlockchains(cardId))
            blockchainNetworkAdapter.fromJson(json)?.distinct() ?: loadSavedCurrenciesOldWay(
                cardId,
                derivationStyle
            )
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun loadDemoCurrencies(cardId: String): List<BlockchainNetwork> {
        return DemoHelper.config.demoBlockchains.map {
            BlockchainNetwork(
                blockchain = it,
                derivationPath = it.derivationPath(DerivationStyle.LEGACY)?.rawPath,
                tokens = emptyList()
            )
        }
    }

    private fun loadSavedCurrenciesOldWay(
        cardId: String, derivationStyle: DerivationStyle?
    ): List<BlockchainNetwork> {
        val blockchains = loadSavedBlockchains(cardId)
        val tokens = loadSavedTokens(cardId)
        val currencies = getSupportedTokens()
        val blockchainNetworks = blockchains.map { blockchain ->
            BlockchainNetwork(
                blockchain = blockchain,
                derivationPath = blockchain.derivationPath(derivationStyle)?.rawPath,
                tokens = tokens
                    .filter { it.blockchainDao.toBlockchain() == blockchain }
                    .map {
                        val token = it.toToken()
                        val id = currencies
                            .find { it.contracts?.find { it.address == token.contractAddress } != null }
                            ?.id
                        token.copy(id = id)
                    }
            )
        }
        saveCurrencies(cardId, blockchainNetworks) // migrate saved currencies
        return blockchainNetworks
    }

    fun saveCurrencies(cardId: String, currencies: List<BlockchainNetwork>) {
        val json = blockchainNetworkAdapter.toJson(currencies)
        context.rewriteFile(json, getFileNameForBlockchains(cardId))
    }

    private fun Context.readFileText(fileName: String): String =
        this.openFileInput(fileName).bufferedReader().readText()

    private fun Context.rewriteFile(content: String, fileName: String) {
        this.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray(), 0, content.length)
        }
    }

    fun getSupportedTokens(isTestNet: Boolean = false): List<Currency> {
        val fileName = if (isTestNet) "testnet_tokens" else "tokens"
        val json = context.assets.readJsonFileToString(fileName)
        return currenciesAdapter.fromJson(json)!!.tokens.map { Currency.fromJsonObject(it) }
    }

    private fun loadTokensJson(blockchain: Blockchain): String? {
        val fileName = getFileName(blockchain)
        return try {
            context.assets.readJsonFileToString(fileName)
        } catch (ex: Exception) {
            Timber.e(ex, "Tokens with the file name %s not found", fileName)
            null
        }
    }

    private fun getFileName(blockchain: Blockchain): String {
        return StringBuilder().apply {
            append(blockchain.id.lowercase(Locale.getDefault()).replace("/test", ""))
            append("_tokens")
            appendIf("_testnet") { blockchain.isTestnet() }
        }.toString()
    }

    private fun fromJsonToTokensDao(tokenJson: String, blockchain: Blockchain): List<TokenDao> {
        return obsoleteTokensAdapter.fromJson(tokenJson)!!.map { it.toTokenDao(blockchain) }
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

    // Use this list to temporarily exclude a blockchain from the list of tokens.
    private fun excludeUnsupportedBlockchains(blockchains: List<Blockchain>): List<Blockchain> {
        return blockchains.toMutableList().apply {
            removeAll(
                listOf(
//                Any blockchain
                )
            )
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

fun Blockchain.getTokensName(): String {
    return when (this) {
        Blockchain.Fantom -> "Fantom Opera"
        else -> this.fullName
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
        ).apply {
            customIconUrl?.let { this.setCustomIconUrl(it) }
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

//@JsonClass(generateAdapter = true)
//data class CardCurrenciesDao(
//    val tokens: List<TokenDao>,
//    val blockchains: List<Blockchain>,
//) {
//    fun toCardCurrencies(): CardCurrencies {
//        return CardCurrencies(
//            tokens = tokens.map { it.toToken() }.distinct(),
//            blockchains = blockchains
//        )
//    }
//
//    companion object {
//        fun fromCardCurrencies(cardCurrencies: CardCurrencies): CardCurrenciesDao {
//            return CardCurrenciesDao(
//                tokens = cardCurrencies.tokens.map { TokenDao.fromToken(it) }.distinct(),
//                blockchains = cardCurrencies.blockchains
//            )
//        }
//    }
//}

data class CardCurrencies(
    val tokens: List<Token>,
    val blockchains: List<Blockchain>,
)

@JsonClass(generateAdapter = true)
data class BlockchainNetwork(
    val blockchain: Blockchain,
    val derivationPath: String?,
    val tokens: List<Token>
) {

    constructor(blockchain: Blockchain, card: Card) : this(
        blockchain = blockchain,
        derivationPath = if (card.settings.isHDWalletAllowed) blockchain.derivationPath(card.derivationStyle)?.rawPath else null,
        tokens = emptyList()
    )


    fun updateTokens(tokens: List<Token>): BlockchainNetwork {
        return copy(
            tokens = (this.tokens + tokens).distinct()
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockchainNetwork

        if (blockchain != other.blockchain) return false
        if (derivationPath != other.derivationPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockchain.hashCode()
        result = 31 * result + (derivationPath?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun fromWalletManager(walletManager: WalletManager): BlockchainNetwork {
            return BlockchainNetwork(
                walletManager.wallet.blockchain,
                walletManager.wallet.publicKey.derivationPath?.rawPath,
                walletManager.cardTokens.toList()
            )
        }
    }
}