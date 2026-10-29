package com.tangem.data.nft

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.nft.models.SolanaNFTTokenStandard
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.lang.reflect.Modifier

/** The domain can not see the SDK, so its list of transferable Solana standards is pinned to the SDK names here. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SolanaNFTTokenStandardTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN sdk token standard WHEN isTransferSupported THEN only compressed is refused`(model: StandardModel) {
        // Act
        val actual = NFTAsset.Identifier.Solana(tokenAddress = "mint", tokenStandard = model.standard)
            .isTransferSupported

        // Assert
        assertThat(actual).isEqualTo(model.isTransferSupported)
    }

    private fun provideTestModels(): List<StandardModel> {
        val sdkStandards = SolanaNFTTokenStandard::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.get(null) as String }
        check(SolanaNFTTokenStandard.MPL_CORE in sdkStandards) { "SDK standards were not found: $sdkStandards" }

        return sdkStandards.map { StandardModel(it, isTransferSupported = it != SolanaNFTTokenStandard.COMPRESSED) } +
            StandardModel(standard = null, isTransferSupported = true)
    }

    data class StandardModel(val standard: String?, val isTransferSupported: Boolean)
}