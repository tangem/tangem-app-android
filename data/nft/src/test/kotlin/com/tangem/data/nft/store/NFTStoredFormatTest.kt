package com.tangem.data.nft.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.data.nft.custom.NFTPriceId
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.legacyDataStoreMoshi
import com.tangem.test.core.datastore.readStoredJson
import com.tangem.test.core.datastore.writeStoredJson
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Guards the stored format of the NFT collections and prices: files written by the Moshi serializer must read back
 * unchanged after the stores moved to kotlinx, and new writes keep that format, so the Moshi serializer that is still
 * used while the kotlinx DataStore toggle is off reads them too.
 */
@OptIn(ExperimentalStdlibApi::class)
internal class NFTStoredFormatTest {

    /** The production Moshi tags NFT identifiers by `"bc"`, as registered in `BlockchainSDKFactoryModule`. */
    private val moshi: Moshi = legacyDataStoreMoshi.newBuilder()
        .add(
            PolymorphicJsonAdapterFactory.of(NFTCollection.Identifier::class.java, "bc")
                .withSubtype(NFTCollection.Identifier.EVM::class.java, "evm")
                .withSubtype(NFTCollection.Identifier.TON::class.java, "ton")
                .withSubtype(NFTCollection.Identifier.Solana::class.java, "sol")
                .withDefaultValue(NFTCollection.Identifier.Unknown),
        )
        .add(
            PolymorphicJsonAdapterFactory.of(NFTAsset.Identifier::class.java, "bc")
                .withSubtype(NFTAsset.Identifier.EVM::class.java, "evm")
                .withSubtype(NFTAsset.Identifier.TON::class.java, "ton")
                .withSubtype(NFTAsset.Identifier.Solana::class.java, "sol")
                .withDefaultValue(NFTAsset.Identifier.Unknown),
        )
        .build()

    private val collectionsSerializer = ListSerializer(NFTCollectionSerializer)

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Collections {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN collections WHEN encoded by Moshi THEN matches the stored format`(model: CollectionsModel) {
            // Act
            val actual = moshi.adapter<List<NFTCollection>>().toJson(model.value)

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN decoded by Moshi THEN collections are kept`(model: CollectionsModel) {
            // Act
            val actual = moshi.adapter<List<NFTCollection>>().fromJson(model.json)

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN read from DataStore THEN collections are kept`(model: CollectionsModel) {
            // Act
            val actual = readStoredJson(
                json = model.json,
                defaultValue = emptyList(),
                serializer = collectionsSerializer,
            )

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN collections WHEN written to DataStore THEN keeps the stored format`(model: CollectionsModel) {
            // Act
            val actual = writeStoredJson(
                value = model.value,
                defaultValue = emptyList(),
                serializer = collectionsSerializer,
            )

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        @Test
        fun `GIVEN identifier with an unknown label WHEN read from DataStore THEN it reads as unknown`() {
            // Arrange
            val json = """[{"identifier":{"bc":"aptos"},"blockchainId":"aptos","count":0}]"""

            // Act
            val actual = readStoredJson(json = json, defaultValue = emptyList(), serializer = collectionsSerializer)

            // Assert
            assertThat(actual.single().identifier).isEqualTo(NFTCollection.Identifier.Unknown)
        }

        private fun provideTestModels() = listOf(
            CollectionsModel(value = listOf(EVM_COLLECTION), json = "[$EVM_COLLECTION_JSON]"),
            CollectionsModel(
                value = listOf(TON_COLLECTION, SOLANA_COLLECTION),
                json = "[$TON_COLLECTION_JSON,$SOLANA_COLLECTION_JSON]",
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Prices {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN prices WHEN encoded by Moshi THEN matches the stored format`(model: PricesModel) {
            // Act
            val actual = moshi.adapter<List<NFTPriceId>>().toJson(model.value)

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN decoded by Moshi THEN prices are kept`(model: PricesModel) {
            // Act
            val actual = moshi.adapter<List<NFTPriceId>>().fromJson(model.json)

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stored format WHEN read from DataStore THEN prices are kept`(model: PricesModel) {
            // Act
            val actual = readStoredJson(json = model.json, defaultValue = emptyList<NFTPriceId>())

            // Assert
            assertThat(actual).isEqualTo(model.value)
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN prices WHEN written to DataStore THEN keeps the stored format`(model: PricesModel) {
            // Act
            val actual = writeStoredJson(value = model.value, defaultValue = emptyList())

            // Assert
            assertThat(actual).isEqualTo(model.json)
        }

        private fun provideTestModels() = listOf(
            PricesModel(
                value = listOf(NFTPriceId(assetId = EVM_ASSET.identifier, price = SALE_PRICE)),
                json = """[{"assetId":$EVM_ASSET_ID_JSON,"price":$SALE_PRICE_JSON}]""",
            ),
            PricesModel(
                value = listOf(
                    NFTPriceId(
                        assetId = SOLANA_ASSET.identifier,
                        price = NFTAsset.SalePrice(value = BigDecimal("1"), symbol = null, decimals = null),
                    ),
                ),
                json = """[{"assetId":$SOLANA_ASSET_ID_JSON,"price":{"value":"1"}}]""",
            ),
        )
    }

    data class CollectionsModel(val value: List<NFTCollection>, val json: String)

    data class PricesModel(val value: List<NFTPriceId>, val json: String)

    private companion object {
        val SALE_PRICE = NFTAsset.SalePrice(value = BigDecimal("0.00000001"), symbol = "ETH", decimals = 18)
        val EVM_ASSET = NFTAsset(
            identifier = NFTAsset.Identifier.EVM(
                tokenId = BigInteger("123456789012345678901234567890"),
                tokenAddress = "0xcollection",
                contractType = NFTAsset.Identifier.EVM.ContractType.ERC1155,
            ),
            collectionIdentifier = NFTCollection.Identifier.EVM(tokenAddress = "0xcollection"),
            blockchainId = "ethereum",
            contractType = "ERC1155",
            owner = "0xowner",
            name = "Ape",
            description = "An ape",
            amount = BigInteger("2"),
            decimals = 0,
            salePrice = SALE_PRICE,
            rarity = NFTAsset.Rarity(rank = "1", label = "Legendary"),
            media = NFTAsset.Media(animationUrl = "https://animation", imageUrl = "https://image"),
            traits = listOf(NFTAsset.Trait(name = "Fur", value = "Gold")),
        )
        val TON_ASSET = NFTAsset(
            identifier = NFTAsset.Identifier.TON(tokenAddress = "EQtoken"),
            collectionIdentifier = NFTCollection.Identifier.TON(contractAddress = null),
            blockchainId = "the-open-network",
            contractType = "unknown",
            owner = null,
            name = null,
            description = null,
            amount = null,
            decimals = null,
            salePrice = null,
            rarity = null,
            media = null,
            traits = emptyList(),
        )
        val SOLANA_ASSET = TON_ASSET.copy(
            identifier = NFTAsset.Identifier.Solana(tokenAddress = "SoLtoken", tokenStandard = 4),
            collectionIdentifier = NFTCollection.Identifier.Solana(collectionAddress = "SoLcollection"),
            blockchainId = "solana",
        )
        val EVM_COLLECTION = NFTCollection(
            identifier = NFTCollection.Identifier.EVM(tokenAddress = "0xcollection"),
            blockchainId = "ethereum",
            name = "Apes",
            description = "Collection",
            logoUrl = "https://logo",
            count = 1,
            assets = listOf(EVM_ASSET),
        )
        val TON_COLLECTION = NFTCollection(
            identifier = NFTCollection.Identifier.TON(contractAddress = null),
            blockchainId = "the-open-network",
            name = null,
            description = null,
            logoUrl = null,
            count = 1,
            assets = listOf(TON_ASSET),
        )
        val SOLANA_COLLECTION = TON_COLLECTION.copy(
            identifier = NFTCollection.Identifier.Solana(collectionAddress = "SoLcollection"),
            blockchainId = "solana",
            assets = listOf(SOLANA_ASSET),
        )

        const val SALE_PRICE_JSON = """{"value":"1E-8","symbol":"ETH","decimals":18}"""
        const val EVM_ASSET_ID_JSON = """{"bc":"evm","tokenId":"123456789012345678901234567890",""" +
            """"tokenAddress":"0xcollection","contractType":"ERC1155"}"""
        const val SOLANA_ASSET_ID_JSON = """{"bc":"sol","tokenAddress":"SoLtoken","tokenStandard":4}"""
        const val EVM_COLLECTION_JSON = """{"identifier":{"bc":"evm","tokenAddress":"0xcollection"},""" +
            """"blockchainId":"ethereum","name":"Apes","description":"Collection","logoUrl":"https://logo",""" +
            """"count":1,"assets":[{"identifier":$EVM_ASSET_ID_JSON,""" +
            """"collectionIdentifier":{"bc":"evm","tokenAddress":"0xcollection"},"blockchainId":"ethereum",""" +
            """"contractType":"ERC1155","owner":"0xowner","name":"Ape","description":"An ape","amount":"2",""" +
            """"decimals":0,"salePrice":$SALE_PRICE_JSON,"rarity":{"rank":"1","label":"Legendary"},""" +
            """"media":{"animationUrl":"https://animation","imageUrl":"https://image"},""" +
            """"traits":[{"name":"Fur","value":"Gold"}]}]}"""
        const val TON_COLLECTION_JSON = """{"identifier":{"bc":"ton"},"blockchainId":"the-open-network",""" +
            """"count":1,"assets":[{"identifier":{"bc":"ton","tokenAddress":"EQtoken"},""" +
            """"collectionIdentifier":{"bc":"ton"},"blockchainId":"the-open-network","contractType":"unknown",""" +
            """"traits":[]}]}"""
        const val SOLANA_COLLECTION_JSON = """{"identifier":{"bc":"sol","collectionAddress":"SoLcollection"},""" +
            """"blockchainId":"solana","count":1,"assets":[{"identifier":$SOLANA_ASSET_ID_JSON,""" +
            """"collectionIdentifier":{"bc":"sol","collectionAddress":"SoLcollection"},"blockchainId":"solana",""" +
            """"contractType":"unknown","traits":[]}]}"""
    }
}