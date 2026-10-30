package com.tangem.domain.nft.models

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NFTAssetSolanaIdentifierTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN token standard WHEN isTransferSupported THEN only known transferable standards`(model: Model) {
        // Act
        val actual = NFTAsset.Identifier.Solana(tokenAddress = "Asset", tokenStandard = model.tokenStandard)
            .isTransferSupported

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    data class Model(val tokenStandard: String?, val expected: Boolean)

    private fun provideTestModels() = listOf(
        Model(tokenStandard = null, expected = true),
        Model(tokenStandard = "Metaplex", expected = true),
        Model(tokenStandard = "MetaplexEdition", expected = true),
        Model(tokenStandard = "Token2022", expected = true),
        Model(tokenStandard = "ProgrammableNft", expected = true),
        Model(tokenStandard = "ProgrammableNftEdition", expected = true),
        Model(tokenStandard = "MplCore", expected = true),
        Model(tokenStandard = "Compressed", expected = false),
        Model(tokenStandard = "SomethingNew", expected = false),
    )
}