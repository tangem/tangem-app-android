package com.tangem.blockchainsdk.yield

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MerklClaimCallDataTest {

    @Test
    fun `GIVEN prefixed payload WHEN decoded THEN it carries the bytes and its selector`() {
        // Act
        val actual = MerklClaimCallData.fromHex("0xb4701e3edeadbeef")

        // Assert
        assertThat(actual).isEqualTo(MerklClaimCallData(byteArrayOf(-76, 112, 30, 62, -34, -83, -66, -17)))
        assertThat(actual.methodId).isEqualTo("0xb4701e3e")
    }

    @Test
    fun `GIVEN unprefixed payload WHEN decoded THEN it is the same call data`() {
        // Act
        val prefixed = MerklClaimCallData.fromHex("0xb4701e3e")
        val bare = MerklClaimCallData.fromHex("b4701e3e")

        // Assert
        assertThat(bare).isEqualTo(prefixed)
        assertThat(bare.hashCode()).isEqualTo(prefixed.hashCode())
    }

    @Test
    fun `GIVEN different payloads WHEN compared THEN they are not equal`() {
        // Act
        val actual = MerklClaimCallData.fromHex("0xb4701e3e")

        // Assert
        assertThat(actual).isNotEqualTo(MerklClaimCallData.fromHex("0xbebc896c"))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a blockchain WHEN validated THEN only evm is accepted`(model: ValidateModel) {
        // Arrange
        val callData = MerklClaimCallData.fromHex("0xb4701e3e")

        // Act
        val actual = callData.validate(model.blockchain)

        // Assert
        assertThat(actual).isEqualTo(model.isValid)
    }

    private fun provideTestModels() = listOf(
        ValidateModel(blockchain = Blockchain.Ethereum, isValid = true),
        ValidateModel(blockchain = Blockchain.Bitcoin, isValid = false),
    )

    data class ValidateModel(val blockchain: Blockchain, val isValid: Boolean)
}