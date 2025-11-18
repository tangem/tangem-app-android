package com.tangem.data.networks.converters

import com.google.common.truth.Truth
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.datasource.local.network.entity.NetworkStatusDM.CurrencyId
import com.tangem.domain.models.currency.CryptoCurrency.ID
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NetworkYieldSupplyStatusConverterTest {

    private val rawNetworkId = "ETH"
    private val derivationPath = Network.DerivationPath.Card(value = "m/44'/60'/0'/0/0")
    private val derivationPathHashCode = "-1843072795"
    private val converter = NetworkYieldSupplyStatusConverter(rawNetworkId, derivationPath)

    private val domainStatus = YieldSupplyStatus(
        isActive = true,
        isInitialized = true,
        isAllowedToSpend = true,
        effectiveProtocolBalance = BigDecimal.ONE,
    )

    @Test
    fun convert() {
        // Arrange
        val value = listOf(
            createDataStatus(id = CurrencyId.createCoinId("ethereum")),
            createDataStatus(id = CurrencyId.createTokenId("usdt", "0x1")),
        )

        // Act
        val actual = converter.convert(value)

        // Assert
        val expected = mapOf(
            ID.fromValue("coin⟨ETH→$derivationPathHashCode⟩ethereum") to domainStatus,
            ID.fromValue("token⟨ETH→$derivationPathHashCode⟩usdt⚓0x1") to domainStatus,
        )

        Truth.assertThat(actual).containsExactlyEntriesIn(expected)
    }

    @Test
    fun convertBack() {
        // Arrange
        val value = mapOf(
            ID.fromValue("coin⟨ETH→$derivationPathHashCode⟩ethereum") to domainStatus,
            ID.fromValue("token⟨ETH→$derivationPathHashCode⟩usdt⚓0x1") to domainStatus,
            ID.fromValue("token⟨ETH→$derivationPathHashCode⟩usdc⚓0x1") to null,
        )

        // Act
        val actual = converter.convertBack(value)

        // Assert
        val expected = listOf(
            createDataStatus(id = CurrencyId.createCoinId("ethereum")),
            createDataStatus(id = CurrencyId.createTokenId("usdt", "0x1")),
        )

        Truth.assertThat(actual).containsExactlyElementsIn(expected)
    }

    private fun createDataStatus(id: CurrencyId): NetworkStatusDM.YieldSupplyStatus {
        return NetworkStatusDM.YieldSupplyStatus(
            id = id,
            isActive = true,
            isInitialized = true,
            isAllowedToSpend = true,
            effectiveProtocolBalance = BigDecimal.ONE,
        )
    }
}