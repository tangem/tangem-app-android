package com.tangem.data.txhistory.repository.factory

import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.TokenInfoRepository
import com.tangem.store.datasource.txhistory.db.dao.TokenInfoDao
import com.tangem.store.datasource.txhistory.db.entity.express.TokenInfoEntity
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Owns the cached token metadata (from [TangemTechApi.getCoins] or from the user's own portfolio). [fetchMissing]
 * populates the cache for unowned tokens, [save] writes through portfolio tokens as they're observed;
 * [getCachedTokens] reads the cache and opportunistically [fetchMissing]-warms any id it couldn't resolve, so a
 * later call for the same id succeeds without an explicit fetch.
 */
@Suppress("LongParameterList")
internal class DefaultTokenInfoRepository @Inject constructor(
    private val tangemTechApi: TangemTechApi,
    private val tokenInfoDao: TokenInfoDao,
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val appScope: AppCoroutineScope,
    excludedBlockchains: ExcludedBlockchains,
) : TokenInfoRepository {
    private val cryptoCurrencyFactory = CryptoCurrencyFactory(excludedBlockchains)

    override suspend fun getCachedTokens(
        userWalletId: UserWalletId,
        ids: Set<ExpressAsset.ID>,
    ): Map<ExpressAsset.ID, CryptoCurrency.Token> {
        val userWallet = userWalletsListRepository.userWalletsSync()
            .firstOrNull { it.walletId == userWalletId }
        val cachedTokens = getCached(ids)
            .associateBy { cacheKey(it.networkId, it.contractAddress) }
        val result = ids.mapNotNull { id ->
            val token = cachedTokens[cacheKey(id)]
                ?.let { createToken(it, userWallet) } ?: return@mapNotNull null
            id to token
        }.toMap()

        fetchMissing(ids.subtract(result.keys))
        return result
    }

    override suspend fun getCachedTokensForNetwork(
        userWalletId: UserWalletId,
        networkId: String,
    ): Set<CryptoCurrency.Token> {
        val userWallet = userWalletsListRepository.userWalletsSync()
            .firstOrNull { it.walletId == userWalletId } ?: return emptySet()
        val entities = withContext(dispatchers.io) { tokenInfoDao.getAllForNetwork(networkId) }
        return entities.mapNotNullTo(mutableSetOf()) { createToken(it, userWallet) }
    }

    /** Cached token infos for the token assets among [assetIds] (coins are skipped). */
    private suspend fun getCached(assetIds: Set<ExpressAsset.ID>): List<TokenInfoEntity> = withContext(dispatchers.io) {
        val networkIds = mutableSetOf<String>()
        val contracts = mutableSetOf<String>()
        assetIds.forEach { id ->
            if (id.contractAddress == ExpressAsset.EMPTY_CONTRACT_ADDRESS_VALUE) return@forEach
            networkIds += id.networkId
            contracts += id.contractAddress
        }
        if (networkIds.isEmpty()) emptyList() else tokenInfoDao.getCached(networkIds, contracts)
    }

    /** Writes through the metadata of known (portfolio) tokens, so it's cached even after a token is removed. */
    override suspend fun save(currencies: Collection<CryptoCurrency>) = withContext(dispatchers.io) {
        val now = System.currentTimeMillis()
        val entities = currencies.filterIsInstance<CryptoCurrency.Token>().mapNotNull { token ->
            val coinId = token.id.rawCurrencyId?.value ?: return@mapNotNull null
            TokenInfoEntity(
                networkId = token.network.rawId,
                contractAddress = token.contractAddress,
                coinId = coinId,
                name = token.name,
                symbol = token.symbol,
                decimals = token.decimals,
                updatedAt = now,
            )
        }
        if (entities.isNotEmpty()) tokenInfoDao.upsert(entities)
    }

    /** Fetches and caches metadata for token assets that are stale/missing in the cache and not already owned. */
    override fun fetchMissing(assetIds: Set<ExpressAsset.ID>) {
        appScope.launch { runSuspendCatching { internalFetchMissing(assetIds) } }
    }

    private suspend fun internalFetchMissing(assetIds: Set<ExpressAsset.ID>) = withContext(dispatchers.io) {
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

    private fun cacheKey(id: ExpressAsset.ID): String = cacheKey(id.networkId, id.contractAddress)

    private fun cacheKey(networkId: String, contractAddress: String): String =
        "$networkId|${contractAddress.lowercase()}"

    private fun createToken(entity: TokenInfoEntity, userWallet: UserWallet?): CryptoCurrency.Token? {
        userWallet ?: return null
        return cryptoCurrencyFactory.createToken(
            token = CryptoCurrencyFactory.Token(
                name = entity.name,
                symbol = entity.symbol,
                contractAddress = entity.contractAddress,
                decimals = entity.decimals,
                id = entity.coinId,
            ),
            networkId = entity.networkId,
            extraDerivationPath = null,
            userWallet = userWallet,
        )
    }

    private companion object {
        val CACHE_TTL_MILLIS: Long = TimeUnit.DAYS.toMillis(10)
    }
}