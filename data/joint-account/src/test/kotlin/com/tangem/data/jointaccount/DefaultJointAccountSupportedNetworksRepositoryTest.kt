package com.tangem.data.jointaccount

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import org.junit.jupiter.api.Test

internal class DefaultJointAccountSupportedNetworksRepositoryTest {

    private val repository = DefaultJointAccountSupportedNetworksRepository()

    @Test
    fun `GIVEN repository WHEN getSupportedNetworks THEN returns the 7 mainnet EVM networks`() {
        // Act
        val actual = repository.getSupportedNetworks()

        // Assert
        assertThat(actual).containsExactly(
            Network.RawID(value = "ethereum"),
            Network.RawID(value = "avalanche"),
            Network.RawID(value = "arbitrum-one"),
            Network.RawID(value = "optimistic-ethereum"),
            Network.RawID(value = "base"),
            Network.RawID(value = "binance-smart-chain"),
            Network.RawID(value = "polygon-pos"),
        ).inOrder()
    }

    @Test
    fun `GIVEN repository WHEN getSupportedNetworks THEN excludes testnet and zkSync Era`() {
        // Act
        val actual = repository.getSupportedNetworks()

        // Assert
        assertThat(actual).doesNotContain(Network.RawID(value = "ethereum/test"))
        assertThat(actual).doesNotContain(Network.RawID(value = "zksync"))
    }
}