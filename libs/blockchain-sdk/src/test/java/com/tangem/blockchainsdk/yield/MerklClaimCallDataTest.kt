package com.tangem.blockchainsdk.yield

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class MerklClaimCallDataTest {

    @Test
    fun `GIVEN prefixed payload WHEN decoded THEN method id is its selector`() {
        // Act
        val actual = MerklClaimCallData.fromHex("0xc65e6dcfdeadbeef")

        // Assert
        assertThat(actual.methodId).isEqualTo("0xc65e6dcf")
        assertThat(actual.data).isEqualTo(byteArrayOf(-58, 94, 109, -49, -34, -83, -66, -17))
    }

    @Test
    fun `GIVEN unprefixed payload WHEN decoded THEN it is the same call data`() {
        // Act
        val prefixed = MerklClaimCallData.fromHex("0xc65e6dcf")
        val bare = MerklClaimCallData.fromHex("c65e6dcf")

        // Assert
        assertThat(bare).isEqualTo(prefixed)
        assertThat(bare.hashCode()).isEqualTo(prefixed.hashCode())
    }

    /** The history converter in the wallet-manager layer tells a claim apart by equality, not by the bytes alone. */
    @Test
    fun `GIVEN different payloads WHEN compared THEN they are not equal`() {
        // Act
        val actual = MerklClaimCallData.fromHex("0xc65e6dcf")

        // Assert
        assertThat(actual).isNotEqualTo(MerklClaimCallData.fromHex("0xc65e6dce"))
    }

    @Test
    fun `GIVEN evm blockchain WHEN validated THEN only evm is accepted`() {
        // Arrange
        val callData = MerklClaimCallData.fromHex("0xc65e6dcf")

        // Act / Assert
        assertThat(callData.validate(Blockchain.Ethereum)).isTrue()
        assertThat(callData.validate(Blockchain.Bitcoin)).isFalse()
    }
}