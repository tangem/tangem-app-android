package com.tangem.data.nft

import arrow.core.Either
import com.tangem.datasource.local.nft.NFTPersistenceStore
import com.tangem.datasource.local.nft.NFTPersistenceStoreFactory
import com.tangem.datasource.local.nft.NFTRuntimeStore
import com.tangem.datasource.local.nft.NFTRuntimeStoreFactory
import com.tangem.datasource.local.nft.converter.NFTSdkAssetConverter
import com.tangem.datasource.local.nft.converter.NFTSdkAssetIdentifierConverter
import com.tangem.datasource.local.nft.converter.NFTSdkCollectionConverter
import com.tangem.datasource.local.nft.converter.NFTSdkCollectionIdentifierConverter
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.tangem.blockchain.nft.models.NFTCollection as SdkNFTCollection

internal class DefaultNFTRepository @Inject constructor(
    private val nftPersistenceStoreFactory: NFTPersistenceStoreFactory,
    private val nftRuntimeStoreFactory: NFTRuntimeStoreFactory,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : NFTRepository {

    private val jobs = mutableMapOf<Network, JobHolder>()

    private val nftRuntimeStores = mutableMapOf<Network, NFTRuntimeStore>()
    private val nftPersistenceStores = mutableMapOf<Network, NFTPersistenceStore>()

    private val nftSdkAssetConverter = NFTSdkAssetConverter(
        nftSdkAssetIdentifierConverter = NFTSdkAssetIdentifierConverter,
        nftSdkCollectionIdentifierConverter = NFTSdkCollectionIdentifierConverter,
    )
    private val collectionConverter = NFTSdkCollectionConverter(
        nftSdkCollectionIdentifierConverter = NFTSdkCollectionIdentifierConverter,
        nftSdkAssetConverter = nftSdkAssetConverter,
    )

    override fun observeCollections(userWalletId: UserWalletId, networks: List<Network>): Flow<List<NFTCollections>> =
        flow {
            emitAll(
                combine(
                    networks.map { getNFTRuntimeStore(it).getCollections() },
                ) { it.asList() },
            )
        }.onStart {
            refreshCollections(userWalletId, networks)
        }

    override suspend fun refreshCollections(userWalletId: UserWalletId, networks: List<Network>) = coroutineScope {
        networks.map { network ->
            launch(dispatchers.io) {
                Either.catch {
                    expireCollections(network)
                    walletManagersFacade.getNFTCollections(userWalletId, network)
                }.onLeft {
                    saveFailedStateInRuntime(
                        network = network,
                        error = it,
                    )
                }.onRight {
                    val mergedCollections = it.mergeWithStoredAssets(network)

                    saveCollectionsInRuntime(
                        network = network,
                        collections = mergedCollections,
                    )
                    saveCollectionsInPersistence(
                        network = network,
                        collections = mergedCollections,
                    )
                }
            }.saveIn(getJobHolder(network))
        }.joinAll()
    }

    private suspend fun expireCollections(network: Network) {
        val runtimeStore = getNFTRuntimeStore(network)

        val expiredCollections = runtimeStore
            .getCollectionsSync()
            .let { collections ->
                collections.copy(
                    content = when (val content = collections.content) {
                        is NFTCollections.Content.Collections -> content.copy(
                            source = StatusSource.CACHE,
                        )
                        is NFTCollections.Content.Error -> content
                    },
                )
            }
        runtimeStore.saveCollections(expiredCollections)
    }

    private suspend fun saveCollectionsInRuntime(network: Network, collections: List<SdkNFTCollection>) {
        getNFTRuntimeStore(network).saveCollections(
            NFTCollections(
                network = network,
                content = NFTCollections.Content.Collections(
                    collections = collections
                        .map { collection ->
                            collectionConverter.convert(network to collection)
                        }
                        .filter {
                            it.id !is NFTCollection.Identifier.Unknown
                        },
                    source = StatusSource.ACTUAL,
                ),
            ),
        )
    }

    private suspend fun saveFailedStateInRuntime(network: Network, error: Throwable) {
        getNFTRuntimeStore(network).saveCollections(
            NFTCollections(
                network = network,
                content = NFTCollections.Content.Error(
                    error = error,
                ),
            ),
        )
    }

    private suspend fun saveCollectionsInPersistence(network: Network, collections: List<SdkNFTCollection>) {
        getNFTPersistenceStore(network).saveCollections(collections)
    }

    private fun getJobHolder(network: Network): JobHolder = jobs[network] ?: run {
        JobHolder().also {
            jobs[network] = it
        }
    }

    private fun getNFTPersistenceStore(network: Network): NFTPersistenceStore = nftPersistenceStores[network] ?: run {
        nftPersistenceStoreFactory.provide(network).also {
            nftPersistenceStores[network] = it
        }
    }

    private suspend fun getNFTRuntimeStore(network: Network): NFTRuntimeStore = nftRuntimeStores[network] ?: run {
        nftRuntimeStoreFactory.provide(network).also {
            nftRuntimeStores[network] = it
            val storedCollections = getStoredCollections(network)
            val storedPrices = getStoredPrices(network)
            it.initialize(
                collections = storedCollections,
                prices = storedPrices,
            )
        }
    }

    private suspend fun getStoredCollections(network: Network) = getNFTPersistenceStore(network)
        .getCollectionsSync()
        .let {
            NFTCollections(
                network = network,
                content = NFTCollections.Content.Collections(
                    collections = it
                        ?.map { collection ->
                            collectionConverter.convert(network to collection)
                        }
                        ?.filter {
                            it.id !is NFTCollection.Identifier.Unknown
                        },
                    source = StatusSource.CACHE,
                ),
            )
        }

    private suspend fun getStoredPrices(network: Network) = getNFTPersistenceStore(network)
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

    private suspend fun List<SdkNFTCollection>.mergeWithStoredAssets(network: Network): List<SdkNFTCollection> {
        val storedCollections =
            getNFTPersistenceStore(network)
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
}