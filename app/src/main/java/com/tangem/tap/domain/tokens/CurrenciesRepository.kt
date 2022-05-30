package com.tangem.tap.domain.tokens

import android.app.Application
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.services.Result
import com.tangem.domain.common.extensions.getTokens
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.network.api.tangemTech.TangemTechService
import com.tangem.network.common.MoshiConverter
import com.tangem.tap.common.extensions.appendIf
import com.tangem.tap.common.extensions.readJsonFileToString
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.domain.tokens.models.ObsoleteTokenDao
import com.tangem.tap.domain.tokens.models.TokenDao
import com.tangem.tap.features.demo.DemoHelper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.*

class CurrenciesRepository(
    private val context: Application,
    private val tangemNetworkService: TangemTechService
) {

    private val moshi = MoshiConverter.defaultMoshi()
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
        val currencies = loadSavedCurrenciesWithoutMigration(cardId).map {
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
        val currencies = loadSavedCurrenciesWithoutMigration(cardId).map {
            if (it == blockchainNetwork) {
                it.copy(tokens = it.tokens.filterNot { it == token })
            } else {
                it
            }
        }
        saveCurrencies(cardId, currencies)
    }

    fun removeBlockchain(cardId: String, blockchainNetwork: BlockchainNetwork) {
        val currencies = loadSavedCurrenciesWithoutMigration(cardId)
            .filterNot { it == blockchainNetwork }
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

    suspend fun loadSavedCurrencies(
        cardId: String,
        isHdWalletSupported: Boolean = false
    ): List<BlockchainNetwork> {
        if (DemoHelper.isDemoCardId(cardId)) {
            return loadDemoCurrencies()
        }
        return try {
            val json = context.readFileText(getFileNameForBlockchains(cardId))
            blockchainNetworkAdapter.fromJson(json)?.distinct() ?: emptyList()
        } catch (exception: Exception) {
            tryToLoadPreviousFormatAndMigrate(cardId, isHdWalletSupported)
        }
    }

    fun loadSavedCurrenciesWithoutMigration(
        cardId: String,
    ): List<BlockchainNetwork> {
        if (DemoHelper.isDemoCardId(cardId)) {
            return loadDemoCurrencies()
        }
        return try {
            val json = context.readFileText(getFileNameForBlockchains(cardId))
            blockchainNetworkAdapter.fromJson(json)?.distinct() ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun loadDemoCurrencies(): List<BlockchainNetwork> {
        return DemoHelper.config.demoBlockchains.map {
            BlockchainNetwork(
                blockchain = it,
                derivationPath = it.derivationPath(DerivationStyle.LEGACY)?.rawPath,
                tokens = emptyList()
            )
        }
    }

    private suspend fun tryToLoadPreviousFormatAndMigrate(
        cardId: String,
        isHdWalletSupported: Boolean = false
    ): List<BlockchainNetwork> {
        return try {
            loadSavedCurrenciesOldWay(
                cardId,
                isHdWalletSupported
            )
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private suspend fun loadSavedCurrenciesOldWay(
        cardId: String, isHdWalletSupported: Boolean = false
    ): List<BlockchainNetwork> {
        val blockchains = loadSavedBlockchains(cardId)
        val tokens = loadSavedTokens(cardId)
        val ids = getTokensIds(tokens)
        val derivationStyle = if (isHdWalletSupported) DerivationStyle.LEGACY else null
        val blockchainNetworks = blockchains.map { blockchain ->
            BlockchainNetwork(
                blockchain = blockchain,
                derivationPath = blockchain.derivationPath(derivationStyle)?.rawPath,
                tokens = tokens
                    .filter { it.blockchainDao.toBlockchain() == blockchain }
                    .map {
                        val token = it.toToken()
                        token.copy(id = ids[token.contractAddress])
                    }
            )
        }
        saveCurrencies(cardId, blockchainNetworks) // migrate saved currencies
        return blockchainNetworks
    }

    private suspend fun getTokensIds(tokens: List<TokenDao>): Map<String, String> = coroutineScope {
        tokens.map {
            async {
                tangemNetworkService.getTokens(
                    it.contractAddress,
                    it.blockchainDao.toBlockchain().toNetworkId()
                )
            }
        }.map { it.await() }
            .map { (it as? Result.Success)?.data?.coins?.firstOrNull()?.id }
            .mapIndexedNotNull { index, s ->
                if (s == null) null else tokens[index].contractAddress to s
            }.toMap()
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

    fun getTestnetCoins(): List<Currency> {
        val json = context.assets.readJsonFileToString(FILE_NAME_TESTNET_COINS)
        return currenciesAdapter.fromJson(json)!!.coins
            .map { Currency.fromJsonObject(it) }
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
        private const val FILE_NAME_TESTNET_COINS = "testnet_tokens"

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
