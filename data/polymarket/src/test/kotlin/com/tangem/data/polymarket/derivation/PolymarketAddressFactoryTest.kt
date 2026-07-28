package com.tangem.data.polymarket.derivation

import com.google.common.truth.Truth.assertThat
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.utils.extensions.hexToBytes
import org.junit.jupiter.api.Test

internal class PolymarketAddressFactoryTest {

    private val factory = PolymarketAddressFactory()

    @Test
    fun `GIVEN known secp256k1 extended public key WHEN createAddress THEN returns canonical ERC-55 address`() {
        // Arrange
        val extendedPublicKey = ExtendedPublicKey(
            publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
            chainCode = "0000000000000000000000000000000000000000000000000000000000000000".hexToBytes(),
        )

        // Act
        val address = factory.createAddress(extendedPublicKey)

        // Assert
        assertThat(address).isEqualTo("0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf")
    }
}