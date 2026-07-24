package com.tangem.datasource.local.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTCollection
import com.tangem.domain.nft.models.NFTCollections
import com.tangem.domain.nft.models.NFTSalePrice
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigInteger

class DefaultNFTRuntimeStoreTest {

    private val network = createNetwork()

    private val store = DefaultNFTRuntimeStore(
        network = network,
        collectionsRuntimeStore = RuntimeSharedStore(),
        pricesRuntimeStore = RuntimeSharedStore(),
    )

    @Test
    fun `GIVEN collection with empty loaded assets WHEN getCollections THEN collection is kept`() = runTest {
        // Arrange
        val collection = createCollection(
            count = 1,
            assets = NFTCollection.Assets.Value(items = emptyList(), source = StatusSource.ACTUAL),
        )
        store.initialize(collections = createCollections(collection), prices = emptyMap())

        // Act
        val content = store.getCollections().first().content

        // Assert
        assertThat(content).isInstanceOf(NFTCollections.Content.Collections::class.java)
        content as NFTCollections.Content.Collections
        val actual = content.collections.orEmpty().single()
        assertThat(actual.count).isEqualTo(1)
        assertThat(actual.assets).isInstanceOf(NFTCollection.Assets.Value::class.java)
    }

    @Test
    fun `GIVEN collection with loaded assets WHEN getCollections THEN count recalculated from assets`() = runTest {
        // Arrange
        val collection = createCollection(
            count = 5,
            assets = NFTCollection.Assets.Value(items = listOf(createAsset()), source = StatusSource.ACTUAL),
        )
        store.initialize(collections = createCollections(collection), prices = emptyMap())

        // Act
        val content = store.getCollections().first().content

        // Assert
        content as NFTCollections.Content.Collections
        assertThat(content.collections.orEmpty().single().count).isEqualTo(1)
    }

    @Test
    fun `GIVEN collection with zero count and not loaded assets WHEN getCollections THEN collection filtered out`() =
        runTest {
            // Arrange
            val collection = createCollection(count = 0, assets = NFTCollection.Assets.Empty)
            store.initialize(collections = createCollections(collection), prices = emptyMap())

            // Act
            val content = store.getCollections().first().content

            // Assert
            content as NFTCollections.Content.Collections
            assertThat(content.collections.orEmpty()).isEmpty()
        }

    private fun createCollections(vararg collections: NFTCollection) = NFTCollections(
        network = network,
        content = NFTCollections.Content.Collections(
            collections = collections.toList(),
            source = StatusSource.ACTUAL,
        ),
    )

    private fun createCollection(count: Int, assets: NFTCollection.Assets) = NFTCollection(
        id = NFTCollection.Identifier.EVM(tokenAddress = TOKEN_ADDRESS),
        network = network,
        name = "Test collection",
        description = null,
        logoUrl = null,
        count = count,
        assets = assets,
    )

    private fun createAsset(): NFTAsset {
        val assetId = NFTAsset.Identifier.EVM(
            tokenAddress = TOKEN_ADDRESS,
            tokenId = BigInteger.ONE,
            contractType = NFTAsset.Identifier.EVM.ContractType.ERC721,
        )
        return NFTAsset(
            id = assetId,
            collectionId = NFTCollection.Identifier.EVM(tokenAddress = TOKEN_ADDRESS),
            network = network,
            contractType = "ERC721",
            owner = null,
            name = "Test asset",
            description = null,
            amount = null,
            decimals = 0,
            salePrice = NFTSalePrice.Empty(assetId),
            rarity = null,
            media = null,
            traits = emptyList(),
            source = StatusSource.ACTUAL,
        )
    }

    private fun createNetwork() = Network(
        id = Network.ID(rawId = Network.RawID("ethereum"), derivationPath = Network.DerivationPath.None),
        name = "Ethereum",
        currencySymbol = "ETH",
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = Network.StandardType.ERC20,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )

    private companion object {
        const val TOKEN_ADDRESS = "0x0000000000000000000000000000000000000001"
    }
}