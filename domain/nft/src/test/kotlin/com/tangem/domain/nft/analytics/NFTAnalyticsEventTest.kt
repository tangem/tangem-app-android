package com.tangem.domain.nft.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

internal class NFTAnalyticsEventTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DetailsScreenOpened {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN nft asset id WHEN details screen opened THEN legacy solana standard sent as metaplex index`(model: ScreenOpenedModel) {
            // Act
            val event = NFTAnalyticsEvent.Details.ScreenOpened(blockchain = model.blockchain, assetId = model.assetId)

            // Assert
            assertThat(event.id).isEqualTo("[NFT] NFT Details Screen Opened")
            assertThat(event.params).isEqualTo(model.expectedParams)
        }

        private fun provideTestModels(): List<ScreenOpenedModel> = listOf(
            ScreenOpenedModel(
                name = "EVM ERC721",
                blockchain = "Ethereum",
                assetId = evm(NFTAsset.Identifier.EVM.ContractType.ERC721),
                expectedParams = mapOf("Blockchain" to "Ethereum", "Standard" to "ERC721"),
            ),
            ScreenOpenedModel(
                name = "EVM ERC1155",
                blockchain = "Ethereum",
                assetId = evm(NFTAsset.Identifier.EVM.ContractType.ERC1155),
                expectedParams = mapOf("Blockchain" to "Ethereum", "Standard" to "ERC1155"),
            ),
            ScreenOpenedModel(
                name = "Solana Metaplex NFT",
                blockchain = "Solana",
                assetId = solana("Metaplex"),
                expectedParams = mapOf("Blockchain" to "Solana", "Standard" to "0"),
            ),
            ScreenOpenedModel(
                name = "Solana Metaplex edition",
                blockchain = "Solana",
                assetId = solana("MetaplexEdition"),
                expectedParams = mapOf("Blockchain" to "Solana", "Standard" to "3"),
            ),
            ScreenOpenedModel(
                name = "Solana pNFT",
                blockchain = "Solana",
                assetId = solana("ProgrammableNft"),
                expectedParams = mapOf("Blockchain" to "Solana", "Standard" to "4"),
            ),
            ScreenOpenedModel(
                name = "Solana pNFT edition",
                blockchain = "Solana",
                assetId = solana("ProgrammableNftEdition"),
                expectedParams = mapOf("Blockchain" to "Solana", "Standard" to "5"),
            ),
            ScreenOpenedModel(
                name = "Solana Core",
                blockchain = "Solana",
                assetId = solana("MplCore"),
                expectedParams = mapOf("Blockchain" to "Solana", "Standard" to "MplCore"),
            ),
            ScreenOpenedModel(
                name = "Solana Token-2022",
                blockchain = "Solana",
                assetId = solana("Token2022"),
                expectedParams = mapOf("Blockchain" to "Solana", "Standard" to "Token2022"),
            ),
            ScreenOpenedModel(
                name = "Solana without standard",
                blockchain = "Solana",
                assetId = solana(tokenStandard = null),
                expectedParams = mapOf("Blockchain" to "Solana"),
            ),
            ScreenOpenedModel(
                name = "TON",
                blockchain = "TON",
                assetId = NFTAsset.Identifier.TON(tokenAddress = "ton-address"),
                expectedParams = mapOf("Blockchain" to "TON"),
            ),
            ScreenOpenedModel(
                name = "Unknown",
                blockchain = "Ethereum",
                assetId = NFTAsset.Identifier.Unknown,
                expectedParams = mapOf("Blockchain" to "Ethereum"),
            ),
        )

        private fun evm(contractType: NFTAsset.Identifier.EVM.ContractType) = NFTAsset.Identifier.EVM(
            tokenAddress = "0xcontract",
            tokenId = BigInteger.ONE,
            contractType = contractType,
        )

        private fun solana(tokenStandard: String?) = NFTAsset.Identifier.Solana(
            tokenAddress = "mint",
            tokenStandard = tokenStandard,
        )
    }

    internal data class ScreenOpenedModel(
        val name: String,
        val blockchain: String,
        val assetId: NFTAsset.Identifier,
        val expectedParams: Map<String, String>,
    ) {
        override fun toString(): String = name
    }
}