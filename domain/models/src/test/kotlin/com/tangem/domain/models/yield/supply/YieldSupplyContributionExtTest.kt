package com.tangem.domain.models.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class YieldSupplyContributionExtTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldSupply {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN field and contributions WHEN yieldSupply THEN field first then an active yield contribution`(
            model: YieldSupplyModel,
        ) {
            // Act
            val actual = value(contributions = model.contributions, field = model.field).yieldSupply

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            YieldSupplyModel(
                name = "field only",
                field = networkStatus,
                contributions = emptyList(),
                expected = networkStatus,
            ),
            YieldSupplyModel(
                name = "contribution only",
                field = null,
                contributions = listOf(yieldSupply(positionStatus)),
                expected = positionStatus,
            ),
            YieldSupplyModel(
                name = "field and contribution: field wins",
                field = networkStatus,
                contributions = listOf(yieldSupply(positionStatus)),
                expected = networkStatus,
            ),
            YieldSupplyModel(name = "neither", field = null, contributions = emptyList(), expected = null),
            YieldSupplyModel(
                name = "inactive contribution, initialized",
                field = null,
                contributions = listOf(yieldSupply(inactiveStatus(isInitialized = true))),
                expected = null,
            ),
            YieldSupplyModel(
                name = "inactive contribution, no module",
                field = null,
                contributions = listOf(yieldSupply(inactiveStatus(isInitialized = false))),
                expected = null,
            ),
            YieldSupplyModel(
                name = "another contribution type only",
                field = null,
                contributions = listOf(OtherContribution),
                expected = null,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class YieldSupplySource {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN contributions WHEN yieldSupplySource THEN source of the yield contribution or null`(
            model: SourceModel,
        ) {
            // Act
            val actual = value(model.contributions).yieldSupplySource

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SourceModel(contributions = emptyList(), expected = null),
            SourceModel(contributions = listOf(OtherContribution), expected = null),
            SourceModel(
                contributions = listOf(yieldSupply(positionStatus, StatusSource.CACHE)),
                expected = StatusSource.CACHE,
            ),
            SourceModel(
                contributions = listOf(OtherContribution, yieldSupply(positionStatus, StatusSource.ONLY_CACHE)),
                expected = StatusSource.ONLY_CACHE,
            ),
            SourceModel(
                contributions = listOf(yieldSupply(inactiveStatus(isInitialized = true), StatusSource.CACHE)),
                expected = StatusSource.CACHE,
            ),
        )
    }

    data class YieldSupplyModel(
        val name: String,
        val field: YieldSupplyStatus?,
        val contributions: List<BalanceContribution>,
        val expected: YieldSupplyStatus?,
    ) {
        override fun toString(): String = name
    }

    data class SourceModel(val contributions: List<BalanceContribution>, val expected: StatusSource?)

    private fun yieldSupply(status: YieldSupplyStatus, source: StatusSource = StatusSource.ACTUAL) =
        YieldSupplyContribution(status = status, source = source)

    private fun value(
        contributions: List<BalanceContribution>,
        field: YieldSupplyStatus? = null,
    ): CryptoCurrencyStatus.Value {
        return CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.TEN,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = field,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
            contributions = contributions,
        )
    }

    private object OtherContribution : BalanceContribution {
        override val kind: String = "other"
        override val source: StatusSource = StatusSource.CACHE
        override fun totalDeltaCryptoAmount(): BigDecimal = BigDecimal.ZERO
    }

    private companion object {

        val networkStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal("4"),
        )

        val positionStatus = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = false,
            effectiveProtocolBalance = BigDecimal("7"),
        )

        fun inactiveStatus(isInitialized: Boolean) = YieldSupplyStatus(
            isActive = false,
            isInitialized = isInitialized,
            isAllowedToSpend = isInitialized,
            effectiveProtocolBalance = null,
        )
    }
}