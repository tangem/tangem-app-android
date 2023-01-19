package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.tap.common.FileReader
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.domain.tokens.models.TokenDao
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

@Deprecated("Use this only for migration")
class OldUserTokensRepository(
    private val fileReader: FileReader,
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    private val moshi = MoshiConverter.networkMoshi
    private val blockchainsAdapter: JsonAdapter<List<Blockchain>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, Blockchain::class.java),
    )
    private val tokensAdapter: JsonAdapter<List<TokenDao>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, TokenDao::class.java),
    )
    private val blockchainNetworkAdapter: JsonAdapter<List<BlockchainNetwork>> =
        moshi.adapter(Types.newParameterizedType(List::class.java, BlockchainNetwork::class.java))

    @Deprecated("Use BlockchainNetwork instead")
    private fun loadSavedTokens(cardId: String): List<TokenDao> {
        val json = try {
            fileReader.readFile(getFileNameForTokens(cardId))
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
            val json = fileReader.readFile(getFileNameForBlockchains(cardId))
            blockchainsAdapter.fromJson(json)?.distinct() ?: emptyList()
        } catch (exception: Exception) {
            emptyList()
        }
    }

    @Deprecated("Use TokensRepository instead")
    suspend fun loadSavedCurrencies(
        cardId: String,
        isHdWalletSupported: Boolean = false,
    ): List<BlockchainNetwork> {
        return try {
            val json = fileReader.readFile(getFileNameForBlockchains(cardId))
            blockchainNetworkAdapter.fromJson(json)?.distinct() ?: emptyList()
        } catch (exception: Exception) {
            tryToLoadPreviousFormatAndMigrate(cardId, isHdWalletSupported)
        }
    }

    private suspend fun tryToLoadPreviousFormatAndMigrate(
        cardId: String,
        isHdWalletSupported: Boolean = false,
    ): List<BlockchainNetwork> {
        return try {
            loadSavedCurrenciesOldWay(
                cardId,
                isHdWalletSupported,
            )
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private suspend fun loadSavedCurrenciesOldWay(
        cardId: String,
        isHdWalletSupported: Boolean = false,
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
                    },
            )
        }
        return blockchainNetworks
    }

    private suspend fun getTokensIds(tokens: List<TokenDao>): Map<String, String> = withContext(dispatchers.io) {
        tokens.map {
            async {
                tangemTechApi.getCoins(
                    contractAddress = it.contractAddress,
                    networkIds = it.blockchainDao.toBlockchain().toNetworkId(),
                    active = true,
                )
            }
        }
            .map {
                runCatching { it.await() }
                    .onSuccess { return@map it.coins.firstOrNull()?.id }
                    .onFailure { return@map null }

                error("Unreachable code because runCatching must return result")
            }
            .mapIndexedNotNull { index, id ->
                if (id == null) null else tokens[index].contractAddress to id
            }
            .toMap()
    }

    companion object {
        private const val FILE_NAME_PREFIX_TOKENS = "tokens"
        private const val FILE_NAME_PREFIX_BLOCKCHAINS = "blockchains"
        private fun getFileNameForTokens(cardId: String): String = "${FILE_NAME_PREFIX_TOKENS}_$cardId"
        private fun getFileNameForBlockchains(cardId: String): String =
            "${FILE_NAME_PREFIX_BLOCKCHAINS}_$cardId"
    }
}
