package com.tangem.domain.polymarket.model

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketExchangeDomainTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a market version and neg-risk flag WHEN the domain is looked up THEN it is the documented one`(
        model: DomainModel,
    ) {
        // Act
        val actual = PolymarketExchangeDomain.of(version = model.version, isNegRisk = model.isNegRisk)

        // Assert
        assertThat(actual?.exchange to actual?.version).isEqualTo(model.expectedExchange to model.expectedVersion)
    }

    @Test
    fun `GIVEN a V1 market WHEN the domain is taken directly THEN it matches the table`() {
        // Assert
        assertThat(PolymarketExchangeDomain.v1(isNegRisk = false))
            .isEqualTo(PolymarketExchangeDomain.of(PolymarketMarketVersion.V1, isNegRisk = false))
        assertThat(PolymarketExchangeDomain.v1(isNegRisk = true))
            .isEqualTo(PolymarketExchangeDomain.of(PolymarketMarketVersion.V1, isNegRisk = true))
    }

    private fun provideTestModels(): List<DomainModel> = listOf(
        DomainModel(
            version = PolymarketMarketVersion.V1,
            isNegRisk = false,
            expectedExchange = "0xE111180000d2663C0091e4f400237545B87B996B",
            expectedVersion = "2",
        ),
        DomainModel(
            version = PolymarketMarketVersion.V1,
            isNegRisk = true,
            expectedExchange = "0xe2222d279d744050d28e00520010520000310F59",
            expectedVersion = "2",
        ),
        DomainModel(
            version = PolymarketMarketVersion.V2,
            isNegRisk = false,
            expectedExchange = "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa",
            expectedVersion = "3",
        ),
        DomainModel(
            version = PolymarketMarketVersion.V2,
            isNegRisk = true,
            expectedExchange = "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa",
            expectedVersion = "3",
        ),
        DomainModel(
            version = PolymarketMarketVersion.UNKNOWN,
            isNegRisk = false,
            expectedExchange = null,
            expectedVersion = null,
        ),
        DomainModel(
            version = PolymarketMarketVersion.UNKNOWN,
            isNegRisk = true,
            expectedExchange = null,
            expectedVersion = null,
        ),
    )

    internal data class DomainModel(
        val version: PolymarketMarketVersion,
        val isNegRisk: Boolean,
        val expectedExchange: String?,
        val expectedVersion: String?,
    )
}