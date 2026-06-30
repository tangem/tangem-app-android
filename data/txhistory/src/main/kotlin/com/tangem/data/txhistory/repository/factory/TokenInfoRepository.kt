package com.tangem.data.txhistory.repository.factory

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.txhistory.db.dao.TokenInfoDao
import com.tangem.datasource.local.txhistory.db.entity.express.TokenInfoEntity
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Owns the cached token metadata (from [TangemTechApi.getCoins]) so [ExpressTransactionAssetFactory] can resolve
 * tokens that are not in any user portfolio. [fetchMissing] populates the cache; [getCached] reads it back.
 */
internal class TokenInfoRepository @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val tokenInfoDao: TokenInfoDao,
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    /** Cached token infos for the token assets among [assetIds] (coins are skipped). */
    suspend fun getCached(assetIds: Set<ExpressAsset.ID>): List<TokenInfoEntity> = withContext(dispatchers.io) {
        val networkIds = mutableSetOf<String>()
        val contracts = mutableSetOf<String>()
        assetIds.forEach { id ->
            if (id.contractAddress == ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE) return@forEach
            networkIds += id.networkId
            contracts += id.contractAddress
        }
        if (networkIds.isEmpty()) emptyList() else tokenInfoDao.getCached(networkIds, contracts)
    }

    /** Fetches and caches metadata for token assets that are stale/missing in the cache and not already owned. */
    suspend fun fetchMissing(assetIds: Set<ExpressAsset.ID>) = withContext(dispatchers.io) {
        // Coins resolve without the catalog — only tokens are looked up.
        val tokenIds = mutableListOf<ExpressAsset.ID>()
        val networkIds = mutableSetOf<String>()
        val contracts = mutableSetOf<String>()
        assetIds.forEach { id ->
            if (id.contractAddress == ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE) return@forEach
            tokenIds += id
            networkIds += id.networkId
            contracts += id.contractAddress
        }
        if (tokenIds.isEmpty()) return@withContext

        val now = System.currentTimeMillis()
        val freshKeys = tokenInfoDao.getCached(
            networkIds = networkIds,
            contractAddresses = contracts,
            minUpdatedAt = now - CACHE_TTL_MILLIS,
        ).mapTo(hashSetOf()) { cacheKey(it.networkId, it.contractAddress) }

        val notCached = tokenIds.filterNot { cacheKey(it.networkId, it.contractAddress) in freshKeys }
        if (notCached.isEmpty()) return@withContext

        // Tokens already present in any portfolio don't need a catalog fetch.
        val portfolioKeys = portfolioTokenKeys()
        val unresolved = notCached.filterNot { cacheKey(it.networkId, it.contractAddress) in portfolioKeys }
        if (unresolved.isEmpty()) return@withContext

        val entities = fetchTokenInfos(unresolved, now)
        if (entities.isNotEmpty()) tokenInfoDao.upsert(entities)
    }

    /** Cache keys of every token owned across all wallets. */
    private suspend fun portfolioTokenKeys(): Set<String> = buildSet {
        multiAccountListSupplier.invoke().first().forEach { accountList ->
            accountList.flattenCurrencies().forEach { currency ->
                if (currency is CryptoCurrency.Token) {
                    add(cacheKey(currency.network.rawId, currency.contractAddress))
                }
            }
        }
    }

    private suspend fun fetchTokenInfos(ids: List<ExpressAsset.ID>, timestamp: Long): List<TokenInfoEntity> {
        val requestedPairs = mutableSetOf<String>()
        val networkIds = mutableSetOf<String>()
        val contracts = mutableSetOf<String>()
        ids.forEach { id ->
            requestedPairs += cacheKey(id.networkId, id.contractAddress)
            networkIds += id.networkId
            contracts += id.contractAddress
        }
        return try {
            // getCoins returns a cross-product of the queried networks×contracts, so the response is filtered back
            // to the exact requested pairs.
            val coins = tangemTechApi.getCoins(
                networkIds = networkIds.joinToString(separator = ","),
                contractAddresses = contracts.joinToString(separator = ","),
                active = true,
            ).getOrThrow().coins

            coins.flatMap { coin ->
                coin.networks.mapNotNull { network ->
                    val contract = network.contractAddress
                    val decimals = network.decimalCount?.toInt()
                    if (contract == null || decimals == null ||
                        cacheKey(network.networkId, contract) !in requestedPairs
                    ) {
                        return@mapNotNull null
                    }
                    TokenInfoEntity(
                        networkId = network.networkId,
                        contractAddress = contract,
                        coinId = coin.id,
                        name = coin.name,
                        symbol = coin.symbol,
                        decimals = decimals,
                        updatedAt = timestamp,
                    )
                }
            }
        } catch (e: Throwable) {
            TangemLogger.w("Failed to fetch token info for ${ids.size} express assets", e)
            emptyList()
        }
    }

    private fun cacheKey(networkId: String, contractAddress: String): String =
        "$networkId|${contractAddress.lowercase()}"

    private companion object {
        val CACHE_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(10)
    }
}