package com.tangem.data.nft

import arrow.core.Either
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.local.nft.NFTPersistenceStore
import com.tangem.datasource.local.nft.NFTPersistenceStoreFactory
import com.tangem.datasource.local.nft.NFTRuntimeStore
import com.tangem.datasource.local.nft.NFTRuntimeStoreFactory
import com.tangem.datasource.local.nft.converter.NFTSdkAssetIdentifierConverter
import com.tangem.datasource.local.nft.converter.NFTSdkCollectionConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.domain.nft.repository.NFTRepository
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.blockchain.nft.models.NFTCollection as SdkNFTCollection

internal class DefaultNFTRepository @Inject constructor(
    private val nftPersistenceStoreFactory: NFTPersistenceStoreFactory,
    private val nftRuntimeStoreFactory: NFTRuntimeStoreFactory,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : NFTRepository {

    private val jobs = mutableMapOf<Network, JobHolder>()

    private val nftRuntimeStores = mutableMapOf<String, NFTRuntimeStore>()
    private val nftPersistenceStores = mutableMapOf<String, NFTPersistenceStore>()

    override fun observeCollections(userWalletId: UserWalletId, networks: List<Network>): Flow<List<NFTCollections>> =
        flow { emitAll(observeCollectionsInternal(userWalletId, networks)) }

    private suspend fun observeCollectionsInternal(
        userWalletId: UserWalletId,
        networks: List<Network>,
    ): Flow<List<NFTCollections>> = combine(
        networks.map { observeCollectionsInternal(userWalletId, it) },
    ) { it.asList() }

    private suspend fun observeCollectionsInternal(
        userWalletId: UserWalletId,
        network: Network,
    ): Flow<NFTCollections> = if (network.canHandleNFTs()) {
        getNFTRuntimeStore(userWalletId, network).getCollections()
    } else {
        flowOf(NFTCollections.empty(network))
    }

    override suspend fun refreshCollections(userWalletId: UserWalletId, networks: List<Network>) = coroutineScope {
        networks.mapNotNull { network ->
            if (network.canHandleNFTs()) {
                launch(dispatchers.io) {
                    Either.catch {
                        expireCollections(userWalletId, network)
                        walletManagersFacade.getNFTCollections(userWalletId, network)
                    }.onLeft {
                        saveFailedStateInRuntime(
                            userWalletId = userWalletId,
                            network = network,
                            error = it,
                        )
                    }.onRight {
                        val mergedCollections = it.mergeWithStoredAssets(userWalletId, network)

                        saveCollectionsInRuntime(
                            userWalletId = userWalletId,
                            network = network,
                            collections = mergedCollections,
                        )
                        saveCollectionsInPersistence(
                            userWalletId = userWalletId,
                            network = network,
                            collections = mergedCollections,
                        )
                    }
                }.saveIn(getJobHolder(network))
            } else {
                null
            }
        }.joinAll()
    }

    private suspend fun expireCollections(userWalletId: UserWalletId, network: Network) {
        val runtimeStore = getNFTRuntimeStore(userWalletId, network)
        val expiredCollections = runtimeStore
            .getCollectionsSync()
            .changeStatusSource(StatusSource.CACHE)
        runtimeStore.saveCollections(expiredCollections)
    }

    private suspend fun saveCollectionsInRuntime(
        userWalletId: UserWalletId,
        network: Network,
        collections: List<SdkNFTCollection>,
    ) {
        getNFTRuntimeStore(userWalletId, network).saveCollections(
            NFTCollections(
                network = network,
                content = NFTCollections.Content.Collections(
                    collections = collections
                        .map { collection ->
                            NFTSdkCollectionConverter.convert(network to collection)
                        }
                        .filter {
                            it.id !is NFTCollection.Identifier.Unknown
                        },
                    source = StatusSource.ACTUAL,
                ),
            ),
        )
    }

    private suspend fun saveFailedStateInRuntime(userWalletId: UserWalletId, network: Network, error: Throwable) {
        getNFTRuntimeStore(userWalletId, network).let { store ->
            val storedCollections = store.getCollectionsSync()
            val content = storedCollections.content
            val updatedCollections = if (content is NFTCollections.Content.Collections && content.collections != null) {
                // if there is any cached collections in store, then mark them as not actual and emit anyway
                storedCollections.changeStatusSource(StatusSource.ONLY_CACHE)
            } else {
                // otherwise, just emit an error
                NFTCollections(
                    network = network,
                    content = NFTCollections.Content.Error(error),
                )
            }
            store.saveCollections(updatedCollections)
        }
    }

    private suspend fun saveCollectionsInPersistence(
        userWalletId: UserWalletId,
        network: Network,
        collections: List<SdkNFTCollection>,
    ) {
        getNFTPersistenceStore(userWalletId, network).saveCollections(collections)
    }

    private fun getJobHolder(network: Network): JobHolder = jobs[network] ?: run {
        JobHolder().also {
            jobs[network] = it
        }
    }

    private fun getNFTPersistenceStore(userWalletId: UserWalletId, network: Network): NFTPersistenceStore {
        val storeId = (userWalletId to network).formatted()
        return nftPersistenceStores[storeId] ?: run {
            nftPersistenceStoreFactory.provide(userWalletId, network).also {
                nftPersistenceStores[storeId] = it
            }
        }
    }

    private suspend fun getNFTRuntimeStore(userWalletId: UserWalletId, network: Network): NFTRuntimeStore {
        val storeId = (userWalletId to network).formatted()
        return nftRuntimeStores[storeId] ?: run {
            nftRuntimeStoreFactory.provide(network).also {
                nftRuntimeStores[storeId] = it
                val storedCollections = getStoredCollections(userWalletId, network)
                val storedPrices = getStoredPrices(userWalletId, network)
                it.initialize(
                    collections = storedCollections,
                    prices = storedPrices,
                )
            }
        }
    }

    private suspend fun getStoredCollections(userWalletId: UserWalletId, network: Network) =
        getNFTPersistenceStore(userWalletId, network)
            .getCollectionsSync()
            .let {
                NFTCollections(
                    network = network,
                    content = NFTCollections.Content.Collections(
                        collections = it
                            ?.map { collection ->
                                NFTSdkCollectionConverter.convert(network to collection)
                            }
                            ?.filter {
                                it.id !is NFTCollection.Identifier.Unknown
                            },
                        source = StatusSource.CACHE,
                    ),
                )
            }

    private suspend fun getStoredPrices(userWalletId: UserWalletId, network: Network) =
        getNFTPersistenceStore(userWalletId, network)
            .getSalePricesSync()
            .orEmpty()
            .let { prices ->
                prices
                    .mapKeys {
                        val (assetId, _) = it
                        NFTSdkAssetIdentifierConverter.convert(assetId)
                    }
                    .mapValues {
                        val (assetId, price) = it
                        NFTSalePrice.Value(
                            assetId = assetId,
                            value = price.value,
                            symbol = price.symbol,
                            source = StatusSource.CACHE,
                        )
                    }
            }

    private fun NFTCollections.changeStatusSource(source: StatusSource) = copy(
        content = when (val content = content) {
            is NFTCollections.Content.Collections -> content.copy(
                source = source,
            )
            is NFTCollections.Content.Error -> content
        },
    )

    private suspend fun List<SdkNFTCollection>.mergeWithStoredAssets(
        userWalletId: UserWalletId,
        network: Network,
    ): List<SdkNFTCollection> {
        val storedCollections =
            getNFTPersistenceStore(userWalletId, network)
                .getCollectionsSync()
                .orEmpty()
                .associateBy(SdkNFTCollection::identifier)

        return this.map { updatedCollection ->
            if (!storedCollections.containsKey(updatedCollection.identifier)) {
                // if there is no collection in cache, then write a new one
                updatedCollection
            } else if (updatedCollection.assets.isNotEmpty()) {
                // if there are assets in a new collection, then write this
                updatedCollection
            } else {
                // otherwise, just update a stored collection fields
                storedCollections[updatedCollection.identifier]
                    ?.copy(
                        name = updatedCollection.name,
                        count = updatedCollection.count,
                        description = updatedCollection.description,
                        logoUrl = updatedCollection.logoUrl,
                    )
                    ?: updatedCollection
            }
        }
    }

    private fun Pair<UserWalletId, Network>.formatted(): String {
        val (walletId, network) = this
        return walletId.stringValue + "_" + network.id.value + "_" + network.derivationPath.value
    }

    private fun Network.canHandleNFTs(): Boolean = Blockchain.fromNetworkId(backendId)?.canHandleNFTs() == true
}