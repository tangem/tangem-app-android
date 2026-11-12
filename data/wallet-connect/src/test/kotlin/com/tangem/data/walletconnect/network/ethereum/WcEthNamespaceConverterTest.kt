package com.tangem.data.walletconnect.network.ethereum

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcEthNamespaceConverterTest {

    private val converter = WcEthNetwork.NamespaceConverter(excludedBlockchains = mockk())

    @ParameterizedTest
    @ProvideTestModels
    fun toBlockchain(model: ToBlockchainModel) {
        // Act
        val actual = converter.toBlockchain(model.chainId)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ToBlockchainModel(val chainId: CAIP2, val expected: Blockchain?)

    private fun provideTestModels() = listOf(
        ToBlockchainModel(chainId = CAIP2(namespace = "eip155", reference = "1"), expected = Blockchain.Ethereum),
        ToBlockchainModel(
            chainId = CAIP2(namespace = "eip155", reference = "383414847825"),
            expected = Blockchain.Zeniq,
        ),
        ToBlockchainModel(chainId = CAIP2(namespace = "eip155", reference = "not-a-number"), expected = null),
        ToBlockchainModel(
            chainId = CAIP2(namespace = "solana", reference = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"),
            expected = null,
        ),
    )
}