package com.tangem.domain.models.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.BalanceContribution
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyContributionExtTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN contributions WHEN yieldSupplySource THEN source of the yield contribution or null`(model: SourceModel) {
        // Act
        val actual = value(model.contributions).yieldSupplySource

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SourceModel(contributions = emptyList(), expected = null),
        SourceModel(contributions = listOf(OtherContribution), expected = null),
        SourceModel(contributions = listOf(yieldSupply(StatusSource.CACHE)), expected = StatusSource.CACHE),
        SourceModel(
            contributions = listOf(OtherContribution, yieldSupply(StatusSource.ONLY_CACHE)),
            expected = StatusSource.ONLY_CACHE,
        ),
    )

    data class SourceModel(val contributions: List<BalanceContribution>, val expected: StatusSource?)

    private fun yieldSupply(source: StatusSource) = YieldSupplyContribution(
        status = YieldSupplyStatus(
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal.ONE,
        ),
        source = source,
    )

    private fun value(contributions: List<BalanceContribution>): CryptoCurrencyStatus.Value {
        return CryptoCurrencyStatus.Loaded(
            amount = BigDecimal.TEN,
            fiatAmount = BigDecimal.TEN,
            fiatRate = BigDecimal.ONE,
            priceChange = BigDecimal.ZERO,
            yieldSupplyStatus = null,
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
}