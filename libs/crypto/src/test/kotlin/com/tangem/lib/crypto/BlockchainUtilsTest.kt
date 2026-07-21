package com.tangem.lib.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.lib.crypto.BlockchainUtils.isPsbtSwapSupported
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.test.core.ProvideTestModels

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BlockchainUtilsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isPsbtSwapSupported(model: PsbtSwapModel) {
        // Act
        val actual = isPsbtSwapSupported(model.blockchain.toNetworkId())

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class PsbtSwapModel(val blockchain: Blockchain, val expected: Boolean)

    private fun provideTestModels() = listOf(
        // UTXO chains whose DEX swaps go through the SDK's PsbtProvider ([REDACTED_TASK_KEY] + [REDACTED_TASK_KEY])
        PsbtSwapModel(Blockchain.Bitcoin, expected = true),
        PsbtSwapModel(Blockchain.BitcoinTestnet, expected = true),
        PsbtSwapModel(Blockchain.Litecoin, expected = true),
        PsbtSwapModel(Blockchain.Dogecoin, expected = true),
        PsbtSwapModel(Blockchain.Dash, expected = true),
        PsbtSwapModel(Blockchain.BitcoinCash, expected = true),
        PsbtSwapModel(Blockchain.BitcoinCashTestnet, expected = true),
        // Other UTXO chains without PsbtProvider support must NOT be gated in
        PsbtSwapModel(Blockchain.Ravencoin, expected = false),
        PsbtSwapModel(Blockchain.Ducatus, expected = false),
        PsbtSwapModel(Blockchain.Clore, expected = false),
        PsbtSwapModel(Blockchain.Pepecoin, expected = false),
        // Non-UTXO chains
        PsbtSwapModel(Blockchain.Ethereum, expected = false),
        PsbtSwapModel(Blockchain.Solana, expected = false),
    )
}