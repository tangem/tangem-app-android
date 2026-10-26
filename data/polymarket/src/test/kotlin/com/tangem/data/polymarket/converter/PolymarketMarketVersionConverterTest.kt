package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketMarketVersionConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a wire market version WHEN converted THEN only v1 and v2 are recognised`(model: VersionModel) {
        // Act
        val actual = PolymarketMarketVersionConverter.convert(value = model.wireVersion)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels(): List<VersionModel> = listOf(
        VersionModel(wireVersion = "v1", expected = PolymarketMarketVersion.V1),
        VersionModel(wireVersion = "v2", expected = PolymarketMarketVersion.V2),
        VersionModel(wireVersion = "unknown", expected = PolymarketMarketVersion.UNKNOWN),
        VersionModel(wireVersion = "V2", expected = PolymarketMarketVersion.UNKNOWN),
        VersionModel(wireVersion = "v3", expected = PolymarketMarketVersion.UNKNOWN),
        VersionModel(wireVersion = null, expected = PolymarketMarketVersion.UNKNOWN),
    )

    internal data class VersionModel(val wireVersion: String?, val expected: PolymarketMarketVersion)
}