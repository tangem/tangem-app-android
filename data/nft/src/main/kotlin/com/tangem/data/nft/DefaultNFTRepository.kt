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
import com.tangem.datasource.local.nft.converter.NFTSdkCollectionIdentifierConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.nft.models.NFTAsset
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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import com.tangem.blockchain.nft.models.NFTAsset as SdkNFTAsset
import com.tangem.blockchain.nft.models.NFTCollection as SdkNFTCollection

@Suppress("LargeClass")
internal class DefaultNFTRepository @Inject constructor(
    private val nftPersistenceStoreFactory: NFTPersistenceStoreFactory,
    private val nftRuntimeStoreFactory: NFTRuntimeStoreFactory,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : NFTRepository {

    private val networkJobs = ConcurrentHashMap<Network, JobHolder>()
    private val collectionJobs = ConcurrentHashMap<NFTCollection.Identifier, JobHolder>()

    private val nftRuntimeStores = ConcurrentHashMap<String, NFTRuntimeStore>()
    private val nftPersistenceStores = ConcurrentHashMap<String, NFTPersistenceStore>()

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

    override suspend fun refreshCollections(userWalletId: UserWalletId, networks: List<Network>) =
        refreshCollectionsInternal(userWalletId, networks, refreshAssets = false)

    override suspend fun refreshAll(userWalletId: UserWalletId, networks: List<Network>) {
        refreshCollectionsInternal(userWalletId, networks, refreshAssets = true)
    }

    override suspend fun refreshAssets(
        userWalletId: UserWalletId,
        network: Network,
        collectionId: NFTCollection.Identifier,
    ) = coroutineScope {
        launch(dispatchers.io) {
            Either.catch {
                expireAssets(userWalletId, network, collectionId)

                val sdkCollectionId = NFTSdkCollectionIdentifierConverter.convertBack(collectionId)

                val assets = walletManagersFacade.getNFTAssets(
                    userWalletId = userWalletId,
                    network = network,
                    collectionIdentifier = sdkCollectionId,
                )

                assets.forEach {
                    val assetId = NFTSdkAssetIdentifierConverter.convert(it.identifier)
                    val price = getNFTRuntimeStore(userWalletId, network).getSalePriceSync(assetId)
                    if (price is NFTSalePrice.Empty || price is NFTSalePrice.Error) {
                        refreshSalePrice(userWalletId, network, sdkCollectionId, it.identifier)
                    }
                }

                getNFTPersistenceStore(userWalletId, network)
                    .getCollectionsSync()
                    ?.map {
                        if (it.identifier == sdkCollectionId) {
                            it.copy(assets = assets)
                        } else {
                            it
                        }
                    }
                    ?.let {
                        saveCollectionsInRuntime(
                            userWalletId = userWalletId,
                            network = network,
                            collections = it,
                        )
                        saveCollectionsInPersistence(
                            userWalletId = userWalletId,
                            network = network,
                            collections = it,
                        )
                    }
            }.onLeft {
                saveFailedStateInRuntime(
                    userWalletId = userWalletId,
                    network = network,
                    error = it,
                )
            }
        }.saveIn(getCollectionJobHolder(collectionId)).join()
    }

    override suspend fun isNFTSupported(network: Network): Boolean = network.canHandleNFTs()

    override suspend fun getNFTExploreUrl(network: Network, assetIdentifier: NFTAsset.Identifier): String? =
        walletManagersFacade.getNFTExploreUrl(
            network = network,
            assetIdentifier = NFTSdkAssetIdentifierConverter.convertBack(assetIdentifier),
        )

    private suspend fun refreshCollectionsInternal(
        userWalletId: UserWalletId,
        networks: List<Network>,
        refreshAssets: Boolean,
    ) = coroutineScope {
        networks.mapNotNull { network ->
            if (network.canHandleNFTs()) {
                launch(dispatchers.io) {
                    Either.catch {
                        expireCollections(userWalletId, network)

                        val collections = walletManagersFacade.getNFTCollections(userWalletId, network)
                        val mergedCollections = collections.mergeWithStoredAssets(userWalletId, network)

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

                        if (refreshAssets) {
                            mergedCollections.forEach { collection ->
                                refreshAssets(
                                    userWalletId = userWalletId,
                                    network = network,
                                    collectionId = NFTSdkCollectionIdentifierConverter.convert(collection.identifier),
                                )
                            }
                        }
                    }.onLeft {
                        saveFailedStateInRuntime(
                            userWalletId = userWalletId,
                            network = network,
                            error = it,
                        )
                    }
                }.saveIn(getNetworkJobHolder(network))
            } else {
                null
            }
        }.joinAll()
    }

    private suspend fun refreshSalePrice(
        userWalletId: UserWalletId,
        network: Network,
        sdkCollectionId: SdkNFTCollection.Identifier,
        sdkAssetId: SdkNFTAsset.Identifier,
    ) = coroutineScope {
        launch(dispatchers.io) {
            val assetId = NFTSdkAssetIdentifierConverter.convert(sdkAssetId)

            Either.catch {
                saveSalePriceInRuntime(userWalletId, network, NFTSalePrice.Loading(assetId))

                val sdkSalePrice =
                    walletManagersFacade.getNFTSalePrice(userWalletId, network, sdkCollectionId, sdkAssetId)

                val salePrice = if (sdkSalePrice == null) {
                    NFTSalePrice.Empty(assetId)
                } else {
                    NFTSalePrice.Value(
                        assetId = assetId,
                        value = sdkSalePrice.value,
                        symbol = sdkSalePrice.symbol,
                    )
                }

                saveSalePriceInRuntime(userWalletId, network, salePrice)

                sdkSalePrice?.let {
                    saveSalePriceInPersistence(userWalletId, network, sdkAssetId, it)
                }
            }.onLeft {
                saveSalePriceInRuntime(userWalletId, network, NFTSalePrice.Error(assetId))
            }
        }
    }

    private suspend fun expireCollections(userWalletId: UserWalletId, network: Network) {
        val runtimeStore = getNFTRuntimeStore(userWalletId, network)
        val expiredCollections = runtimeStore
            .getCollectionsSync()
            .changeStatusSource(StatusSource.CACHE)
        runtimeStore.saveCollections(expiredCollections)
    }

    private suspend fun expireAssets(
        userWalletId: UserWalletId,
        network: Network,
        collectionId: NFTCollection.Identifier,
    ) {
        val runtimeStore = getNFTRuntimeStore(userWalletId, network)
        val storedCollections = runtimeStore.getCollectionsSync()
        val expiredCollections = storedCollections
            .changeCollectionAssetsStatusSource(collectionId, StatusSource.CACHE)
            .let {
                storedCollections.copy(
                    content = it,
                )
            }
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
            val updatedCollections = if (content is NFTCollections.Content.Collections &&
                !content.collections.isNullOrEmpty()
            ) {
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

    private suspend fun saveSalePriceInRuntime(userWalletId: UserWalletId, network: Network, salePrice: NFTSalePrice) {
        getNFTRuntimeStore(userWalletId, network).saveSalePrice(salePrice)
    }

    private suspend fun saveSalePriceInPersistence(
        userWalletId: UserWalletId,
        network: Network,
        assetId: SdkNFTAsset.Identifier,
        salePrice: SdkNFTAsset.SalePrice,
    ) {
        getNFTPersistenceStore(userWalletId, network).saveSalePrice(assetId, salePrice)
    }

    private fun getNetworkJobHolder(network: Network): JobHolder = networkJobs.getOrPut(network) {
        JobHolder().also {
            networkJobs[network] = it
        }
    }

    private fun getCollectionJobHolder(collectionId: NFTCollection.Identifier): JobHolder =
        collectionJobs.getOrPut(collectionId) {
            JobHolder().also {
                collectionJobs[collectionId] = it
            }
        }

    private fun getNFTPersistenceStore(userWalletId: UserWalletId, network: Network): NFTPersistenceStore {
        val storeId = (userWalletId to network).formatted()
        return nftPersistenceStores.getOrPut(storeId) {
            nftPersistenceStoreFactory.provide(userWalletId, network).also {
                nftPersistenceStores[storeId] = it
            }
        }
    }

    private suspend fun getNFTRuntimeStore(userWalletId: UserWalletId, network: Network): NFTRuntimeStore {
        val storeId = (userWalletId to network).formatted()
        return nftRuntimeStores.getOrPut(storeId) {
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

    private fun NFTCollections.changeCollectionAssetsStatusSource(
        collectionId: NFTCollection.Identifier,
        source: StatusSource,
    ) = when (val content = content) {
        is NFTCollections.Content.Collections ->
            content
                .copy(
                    collections = content
                        .collections
                        ?.map {
                            if (it.id == collectionId) {
                                it.changeAssetsStatusSource(source)
                            } else {
                                it
                            }
                        },
                )
        is NFTCollections.Content.Error -> content
    }

    private fun NFTCollection.changeAssetsStatusSource(source: StatusSource) = copy(
        assets = when (val assets = this.assets) {
            is NFTCollection.Assets.Empty -> NFTCollection.Assets.Loading
            is NFTCollection.Assets.Loading -> assets
            is NFTCollection.Assets.Failed -> assets
            is NFTCollection.Assets.Value -> assets.copy(
                source = source,
            )
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