package com.tangem.features.earn.yield.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyContribution
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.staking.earn.ResolveEarnRateUseCase
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.features.earn.yield.impl.fixtures.coin
import com.tangem.features.earn.yield.impl.fixtures.currencyStatus
import com.tangem.features.earn.yield.impl.fixtures.token
import com.tangem.features.earn.yield.impl.fixtures.yieldStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

internal class YieldModeRateMapperTest {

    private val mapper = YieldModeRateMapper()
    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val ethereumToken = token()
    private val key = ethereumToken.yieldSupplyKey()

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TokenInMap {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN token in the map WHEN entry THEN rate, activity, source and route per table`(model: ActivityModel) {
            // Arrange
            val status = currencyStatus(ethereumToken, model.yieldSupplyStatus)

            // Act
            val actual = mapper.entry(userWalletId, status, mapOf(key to BigDecimal("5.5")))

            // Assert
            val expected = EarnRateEntry(
                rate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY),
                isActive = model.expectedActive,
                source = StatusSource.ACTUAL,
                route = AppRoute.YieldSupplyEntry(userWalletId, ethereumToken, "5.5"),
            )
            assertThat(actual).isEqualTo(expected)
        }

        private fun provideTestModels() = listOf(
            ActivityModel(name = "active", yieldSupplyStatus = yieldStatus(isActive = true), expectedActive = true),
            ActivityModel(name = "inactive", yieldSupplyStatus = yieldStatus(isActive = false), expectedActive = false),
            ActivityModel(name = "no yield status", yieldSupplyStatus = null, expectedActive = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ActivityFromContribution {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an empty yield status field AND a yield contribution WHEN entry THEN activity follows it`(
            model: ContributionModel,
        ) {
            // Arrange
            val status = contributionOnlyStatus(isActive = model.isActive)

            // Act
            val actual = mapper.entry(userWalletId, status, mapOf(key to BigDecimal("5.5")))

            // Assert
            assertThat(actual?.isActive).isEqualTo(model.isActive)
        }

        private fun provideTestModels() = listOf(
            ContributionModel(name = "active contribution", isActive = true),
            ContributionModel(name = "inactive contribution", isActive = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SourceFromContribution {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN network and contribution sources WHEN entry THEN the entry source is the weaker of both`(
            model: SourceModel,
        ) {
            // Arrange
            val status = contributionOnlyStatus(
                isActive = true,
                contributionSource = model.contributionSource,
                networkSource = model.networkSource,
            )

            // Act
            val actual = mapper.entry(userWalletId, status, mapOf(key to BigDecimal("5.5")))

            // Assert
            assertThat(actual?.source).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SourceModel(StatusSource.ACTUAL, StatusSource.ACTUAL, expected = StatusSource.ACTUAL),
            SourceModel(StatusSource.ACTUAL, StatusSource.CACHE, expected = StatusSource.CACHE),
            SourceModel(StatusSource.ACTUAL, StatusSource.ONLY_CACHE, expected = StatusSource.ONLY_CACHE),
            SourceModel(StatusSource.CACHE, StatusSource.ACTUAL, expected = StatusSource.CACHE),
            SourceModel(StatusSource.CACHE, StatusSource.ONLY_CACHE, expected = StatusSource.ONLY_CACHE),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Coins {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a coin WHEN entry THEN null`(model: CoinModel) {
            // Arrange
            val status = currencyStatus(coin(model.blockchain))

            // Act
            val actual = mapper.entry(userWalletId, status, mapOf(key to BigDecimal("5.5")))

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels() = listOf(CoinModel(Blockchain.Ethereum), CoinModel(Blockchain.Solana))
    }

    @Test
    fun `GIVEN token missing from the map WHEN entry THEN null`() {
        // Act
        val actual = mapper.entry(userWalletId, currencyStatus(ethereumToken), mapOf("other_key" to BigDecimal("5.5")))

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN empty map WHEN entry THEN null`() {
        // Act
        val actual = mapper.entry(userWalletId, currencyStatus(ethereumToken), emptyMap())

        // Assert
        assertThat(actual).isNull()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class KeyCase {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN key in other case WHEN entry THEN match only on case-insensitive networks`(model: CaseModel) {
            // Arrange
            val status = currencyStatus(model.token)
            val upperCaseKey = model.token.yieldSupplyKey().uppercase()

            // Act
            val actual = mapper.entry(userWalletId, status, mapOf(upperCaseKey to BigDecimal("5.5")))

            // Assert
            assertThat(actual != null).isEqualTo(model.expectedMatch)
        }

        private fun provideTestModels() = listOf(
            CaseModel(name = "EVM token", token = token(), expectedMatch = true),
            CaseModel(
                name = "non-EVM token",
                token = token(Blockchain.Tron, "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"),
                expectedMatch = false,
            ),
        )
    }

    @Test
    fun `GIVEN percent with many digits WHEN entry THEN fraction keeps every digit and the route keeps the plain string`() {
        // Act
        val actual = mapper.entry(userWalletId, currencyStatus(ethereumToken), mapOf(key to BigDecimal("5.123456789")))

        // Assert
        assertThat(actual?.rate?.fraction).isEqualTo(BigDecimal("0.05123456789"))
        assertThat(actual?.route).isEqualTo(AppRoute.YieldSupplyEntry(userWalletId, ethereumToken, "5.123456789"))
    }

    @Test
    fun `GIVEN zero apy WHEN entry THEN entry with a zero fraction`() {
        // Act
        val actual = mapper.entry(userWalletId, currencyStatus(ethereumToken), mapOf(key to BigDecimal("0")))

        // Assert
        assertThat(actual?.rate?.fraction).isEquivalentAccordingToCompareTo(BigDecimal.ZERO)
        assertThat(actual?.route).isEqualTo(AppRoute.YieldSupplyEntry(userWalletId, ethereumToken, "0"))
    }

    @ParameterizedTest
    @EnumSource(StatusSource::class)
    fun `GIVEN any network source WHEN entry THEN entry source is the same`(source: StatusSource) {
        // Arrange
        val status = currencyStatus(ethereumToken, yieldStatus(isActive = true), source)

        // Act
        val actual = mapper.entry(userWalletId, status, mapOf(key to BigDecimal("5.5")))

        // Assert
        assertThat(actual?.source).isEqualTo(source)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Parity {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN same inputs WHEN mapper entry and ResolveEarnRateUseCase THEN rate and activity agree`(
            model: ParityModel,
        ) {
            // Act
            val legacy = ResolveEarnRateUseCase().invoke(
                currencyStatus = model.status,
                yieldSupplyApy = model.apy,
                stakingAvailability = emptyMap(),
            )
            val actual = mapper.entry(userWalletId, model.status, model.apy)

            // Assert
            if (legacy == null) {
                assertThat(actual).isNull()
            } else {
                assertThat(actual?.rate?.fraction).isEqualTo(legacy.apy)
                assertThat(actual?.isActive).isEqualTo(legacy.isActive)
            }
        }

        private fun provideTestModels(): List<ParityModel> {
            val apy = mapOf(key to BigDecimal("5.5"))
            return listOf(
                ParityModel("active token", currencyStatus(ethereumToken, yieldStatus(isActive = true)), apy),
                ParityModel("inactive token", currencyStatus(ethereumToken, yieldStatus(isActive = false)), apy),
                ParityModel("token without yield status", currencyStatus(ethereumToken), apy),
                ParityModel("active through the contribution only", contributionOnlyStatus(isActive = true), apy),
                ParityModel("coin", currencyStatus(coin(Blockchain.Ethereum)), apy),
                ParityModel("token missing from the map", currencyStatus(ethereumToken), mapOf("other_key" to BigDecimal.ONE)),
                ParityModel("uppercase key on Ethereum", currencyStatus(ethereumToken), mapOf(key.uppercase() to BigDecimal.TEN)),
                ParityModel("zero apy", currencyStatus(ethereumToken, yieldStatus(isActive = true)), mapOf(key to BigDecimal.ZERO)),
            )
        }
    }

    private fun contributionOnlyStatus(
        isActive: Boolean,
        contributionSource: StatusSource = StatusSource.ACTUAL,
        networkSource: StatusSource = StatusSource.ACTUAL,
    ): CryptoCurrencyStatus {
        val status = currencyStatus(ethereumToken, networkSource = networkSource)
        val contribution = YieldSupplyContribution(status = yieldStatus(isActive), source = contributionSource)
        val value = (status.value as CryptoCurrencyStatus.Loaded).copy(contributions = listOf(contribution))
        return status.copy(value = value)
    }

    internal data class ContributionModel(val name: String, val isActive: Boolean) {
        override fun toString(): String = name
    }

    internal data class SourceModel(
        val networkSource: StatusSource,
        val contributionSource: StatusSource,
        val expected: StatusSource,
    ) {
        override fun toString(): String = "network $networkSource AND contribution $contributionSource -> $expected"
    }

    internal data class ActivityModel(
        val name: String,
        val yieldSupplyStatus: YieldSupplyStatus?,
        val expectedActive: Boolean,
    ) {
        override fun toString(): String = name
    }

    internal data class CoinModel(val blockchain: Blockchain) {
        override fun toString(): String = blockchain.name
    }

    internal data class CaseModel(val name: String, val token: CryptoCurrency.Token, val expectedMatch: Boolean) {
        override fun toString(): String = name
    }

    internal data class ParityModel(val name: String, val status: CryptoCurrencyStatus, val apy: Map<String, BigDecimal>) {
        override fun toString(): String = name
    }
}