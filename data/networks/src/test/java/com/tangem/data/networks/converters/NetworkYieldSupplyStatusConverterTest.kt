package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.currency.CryptoCurrency.ID.Body
import com.tangem.domain.models.currency.CryptoCurrency.ID.Prefix
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import org.junit.jupiter.api.Test

internal class NetworkYieldSupplyStatusConverterTest {

    @Test
    fun convert() {
        // Arrange
        val value = mapOf(
            "coin⟨ETH⟩ethereum" to NetworkStatusDM.YieldSupplyStatus(
                isActive = false,
                isInitialized = false,
                isAllowedToSpend = false,
            ),
            "coin⟨ETH→12367123⟩ethereum" to null,
        )

        // Act
        val actual = NetworkYieldSupplyStatusConverter.convert(value)

        // Assert
        val expected = mapOf(
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkId(rawId = "ETH"),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to YieldSupplyStatus(
                isActive = false,
                isInitialized = false,
                isAllowedToSpend = false,
            ),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkIdWithDerivationPath(rawId = "ETH", derivationPathHashCode = 12367123),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to null,
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun convertBack() {
        // Arrange
        val value = mapOf(
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkId(rawId = "ETH"),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to YieldSupplyStatus(
                isActive = false,
                isInitialized = false,
                isAllowedToSpend = false,
            ),
            ID(
                prefix = Prefix.COIN_PREFIX,
                body = Body.NetworkIdWithDerivationPath(rawId = "ETH", derivationPathHashCode = 12367123),
                suffix = ID.Suffix.RawID(rawId = "ethereum"),
            ) to null,
        )

        // Act
        val actual = NetworkYieldSupplyStatusConverter.convertBack(value)

        // Assert
        val expected = mapOf(
            "coin⟨ETH⟩ethereum" to NetworkStatusDM.YieldSupplyStatus(
                isActive = false,
                isInitialized = false,
                isAllowedToSpend = false,
            ),
        )

        Truth.assertThat(actual).isEqualTo(expected)
    }
}