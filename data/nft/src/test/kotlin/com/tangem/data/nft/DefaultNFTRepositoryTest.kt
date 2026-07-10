package com.tangem.data.nft

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.nft.NFTPersistenceStore
import com.tangem.datasource.local.nft.NFTPersistenceStoreFactory
import com.tangem.datasource.local.nft.NFTRuntimeStore
import com.tangem.datasource.local.nft.NFTRuntimeStoreFactory
import com.tangem.datasource.local.nft.converter.NFTSdkCollectionIdentifierConverter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import java.math.BigInteger
import com.tangem.blockchain.nft.models.NFTAsset as SdkNFTAsset
import com.tangem.blockchain.nft.models.NFTCollection as SdkNFTCollection

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultNFTRepositoryTest {

    private val nftPersistenceStoreFactory: NFTPersistenceStoreFactory = mockk()
    private val nftRuntimeStoreFactory: NFTRuntimeStoreFactory = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val context: Context = mockk()

    private val userWalletId = UserWalletId("011")
    private val userWallet = mockk<UserWallet.Hot> {
        every { walletId } returns userWalletId
    }

    private val network: Network = MockCryptoCurrencyFactory().ethereum.network

    @BeforeEach
    fun resetMocks() {
        clearMocks(nftPersistenceStoreFactory, nftRuntimeStoreFactory, walletManagersFacade, userWalletsListRepository)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { context.resources } returns mockk()
    }

    private fun createRepository() = DefaultNFTRepository(
        nftPersistenceStoreFactory = nftPersistenceStoreFactory,
        nftRuntimeStoreFactory = nftRuntimeStoreFactory,
        walletManagersFacade = walletManagersFacade,
        dispatchers = TestingCoroutineDispatcherProvider(),
        userWalletsListRepository = userWalletsListRepository,
        networkFactory = mockk(),
        excludedBlockchains = ExcludedBlockchains(),
        context = context,
    )

    @Test
    fun `GIVEN collections fetched WHEN persistence write fails THEN runtime keeps actual data`() = runTest {
        // Arrange
        val runtimeStore = FakeNFTRuntimeStore(network)
        val persistenceStore = mockk<NFTPersistenceStore> {
            coEvery { getCollectionsSync() } returns null
            coEvery { getSalePricesSync() } returns null
            coEvery { saveCollections(any()) } throws IOException("Failed to write to disk")
        }
        every { nftPersistenceStoreFactory.provide(userWalletId, network) } returns persistenceStore
        every { nftRuntimeStoreFactory.provide(network) } returns runtimeStore
        coEvery { walletManagersFacade.getNFTCollections(userWalletId, network) } returns listOf(createSdkCollection())

        // Act
        createRepository().refreshCollections(userWalletId, listOf(network))

        // Assert
        val content = runtimeStore.getCollectionsSync().content
        assertThat(content).isInstanceOf(NFTCollections.Content.Collections::class.java)
        content as NFTCollections.Content.Collections
        assertThat(content.source).isEqualTo(StatusSource.ACTUAL)
        assertThat(content.collections).hasSize(1)
    }

    @Test
    fun `GIVEN runtime has collection missing in persistence WHEN refreshAssets THEN assets saved to runtime`() =
        runTest {
            // Arrange
            val sdkCollection = createSdkCollection()
            val collectionId = NFTSdkCollectionIdentifierConverter.convert(sdkCollection.identifier)
            val runtimeStore = FakeNFTRuntimeStore(network)
            val persistenceStore = mockk<NFTPersistenceStore> {
                coEvery { getCollectionsSync() } returns null
                coEvery { getSalePricesSync() } returns null
                coEvery { saveCollections(any()) } returns Unit
            }
            every { nftPersistenceStoreFactory.provide(userWalletId, network) } returns persistenceStore
            every { nftRuntimeStoreFactory.provide(network) } returns runtimeStore
            coEvery {
                walletManagersFacade.getNFTCollections(userWalletId, network)
            } returns listOf(sdkCollection)
            coEvery {
                walletManagersFacade.getNFTAssets(userWalletId, network, sdkCollection.identifier)
            } returns listOf(createSdkAsset())
            coEvery { walletManagersFacade.getNFTSalePrice(userWalletId, network, any(), any()) } returns null

            val repository = createRepository()
            // seed runtime store with the fetched collection, persistence stays empty
            repository.refreshCollections(userWalletId, listOf(network))

            // Act
            repository.refreshAssets(userWalletId, network, collectionId)

            // Assert
            val content = runtimeStore.getCollectionsSync().content
            assertThat(content).isInstanceOf(NFTCollections.Content.Collections::class.java)
            content as NFTCollections.Content.Collections
            val assets = content.collections.orEmpty().single().assets
            assertThat(assets).isInstanceOf(NFTCollection.Assets.Value::class.java)
            assets as NFTCollection.Assets.Value
            assertThat(assets.items).hasSize(1)
        }

    @Test
    fun `GIVEN fetch returns no assets WHEN refreshAssets THEN empty loaded value saved to runtime`() = runTest {
        // Arrange
        val sdkCollection = createSdkCollection()
        val collectionId = NFTSdkCollectionIdentifierConverter.convert(sdkCollection.identifier)
        val runtimeStore = FakeNFTRuntimeStore(network)
        val persistenceStore = mockk<NFTPersistenceStore> {
            coEvery { getCollectionsSync() } returns null
            coEvery { getSalePricesSync() } returns null
            coEvery { saveCollections(any()) } returns Unit
        }
        every { nftPersistenceStoreFactory.provide(userWalletId, network) } returns persistenceStore
        every { nftRuntimeStoreFactory.provide(network) } returns runtimeStore
        coEvery {
            walletManagersFacade.getNFTCollections(userWalletId, network)
        } returns listOf(sdkCollection)
        coEvery {
            walletManagersFacade.getNFTAssets(userWalletId, network, sdkCollection.identifier)
        } returns emptyList()

        val repository = createRepository()
        repository.refreshCollections(userWalletId, listOf(network))

        // Act
        repository.refreshAssets(userWalletId, network, collectionId)

        // Assert
        val content = runtimeStore.getCollectionsSync().content
        assertThat(content).isInstanceOf(NFTCollections.Content.Collections::class.java)
        content as NFTCollections.Content.Collections
        val assets = content.collections.orEmpty().single().assets
        assertThat(assets).isInstanceOf(NFTCollection.Assets.Value::class.java)
        assets as NFTCollection.Assets.Value
        assertThat(assets.items).isEmpty()
    }

    @Test
    fun `GIVEN no cached collections WHEN fetch fails THEN error state saved to runtime`() = runTest {
        // Arrange
        val runtimeStore = FakeNFTRuntimeStore(network)
        val persistenceStore = mockk<NFTPersistenceStore> {
            coEvery { getCollectionsSync() } returns null
            coEvery { getSalePricesSync() } returns null
        }
        every { nftPersistenceStoreFactory.provide(userWalletId, network) } returns persistenceStore
        every { nftRuntimeStoreFactory.provide(network) } returns runtimeStore
        coEvery { walletManagersFacade.getNFTCollections(userWalletId, network) } throws IOException("HTTP 500")

        // Act
        createRepository().refreshCollections(userWalletId, listOf(network))

        // Assert
        val content = runtimeStore.getCollectionsSync().content
        assertThat(content).isInstanceOf(NFTCollections.Content.Error::class.java)
    }

    @Test
    fun `GIVEN cached collections WHEN fetch fails THEN cache marked as only cache`() = runTest {
        // Arrange
        val runtimeStore = FakeNFTRuntimeStore(network)
        val persistenceStore = mockk<NFTPersistenceStore> {
            coEvery { getCollectionsSync() } returns listOf(createSdkCollection())
            coEvery { getSalePricesSync() } returns null
        }
        every { nftPersistenceStoreFactory.provide(userWalletId, network) } returns persistenceStore
        every { nftRuntimeStoreFactory.provide(network) } returns runtimeStore
        coEvery { walletManagersFacade.getNFTCollections(userWalletId, network) } throws IOException("HTTP 500")

        // Act
        createRepository().refreshCollections(userWalletId, listOf(network))

        // Assert
        val content = runtimeStore.getCollectionsSync().content
        assertThat(content).isInstanceOf(NFTCollections.Content.Collections::class.java)
        content as NFTCollections.Content.Collections
        assertThat(content.source).isEqualTo(StatusSource.ONLY_CACHE)
        assertThat(content.collections).hasSize(1)
    }

    private fun createSdkCollection(assets: List<SdkNFTAsset> = emptyList()) = SdkNFTCollection(
        identifier = SdkNFTCollection.Identifier.EVM(tokenAddress = TOKEN_ADDRESS),
        blockchainId = Blockchain.Ethereum.id,
        name = "Test collection",
        description = null,
        logoUrl = null,
        count = 1,
        assets = assets,
    )

    private fun createSdkAsset() = SdkNFTAsset(
        identifier = SdkNFTAsset.Identifier.EVM(
            tokenId = BigInteger.ONE,
            tokenAddress = TOKEN_ADDRESS,
            contractType = SdkNFTAsset.Identifier.EVM.ContractType.ERC721,
        ),
        collectionIdentifier = SdkNFTCollection.Identifier.EVM(tokenAddress = TOKEN_ADDRESS),
        blockchainId = Blockchain.Ethereum.id,
        contractType = "ERC721",
        owner = null,
        name = "Test asset",
        description = null,
        amount = BigInteger.ONE,
        decimals = 0,
        salePrice = null,
        rarity = null,
        media = null,
        traits = emptyList(),
    )

    private class FakeNFTRuntimeStore(private val network: Network) : NFTRuntimeStore {

        private var collections: NFTCollections = NFTCollections.empty(network)
        private var prices: Map<NFTAsset.Identifier, NFTSalePrice> = emptyMap()

        override suspend fun initialize(collections: NFTCollections, prices: Map<NFTAsset.Identifier, NFTSalePrice>) {
            this.collections = collections
            this.prices = prices
        }

        override fun getCollections(): Flow<NFTCollections> = flowOf(collections)

        override suspend fun getCollectionsSync(): NFTCollections = collections

        override fun getAsset(
            collectionId: NFTCollection.Identifier,
            assetId: NFTAsset.Identifier,
        ): Flow<NFTAsset?> = flowOf(null)

        override fun getSalePrice(assetId: NFTAsset.Identifier): Flow<NFTSalePrice> =
            flowOf(prices[assetId] ?: NFTSalePrice.Empty(assetId))

        override suspend fun getSalePriceSync(assetId: NFTAsset.Identifier): NFTSalePrice =
            prices[assetId] ?: NFTSalePrice.Empty(assetId)

        override suspend fun saveCollections(collections: NFTCollections) {
            this.collections = collections
        }

        override suspend fun saveSalePrice(salePrice: NFTSalePrice) {
            prices = prices + (salePrice.assetId to salePrice)
        }

        override suspend fun clear() {
            collections = NFTCollections.empty(network)
            prices = emptyMap()
        }
    }

    private companion object {
        const val TOKEN_ADDRESS = "0x0000000000000000000000000000000000000001"
    }
}