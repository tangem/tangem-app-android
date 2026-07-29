package com.tangem.data.polymarket.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Wallet
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.utils.extensions.hexToBytes
import org.junit.jupiter.api.Test
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.math.BigInteger

internal class PolymarketSignatureFormatterTest {

    private val formatter = PolymarketSignatureFormatter()

    private val privateKey = BigInteger.valueOf(SAMPLE_PRIVATE_KEY)
    private val keyPair = ECKeyPair.create(privateKey)
    private val hash = "0x" + "11".repeat(HASH_SIZE)

    @Test
    fun `GIVEN a card signature WHEN format THEN returns the same rsv as an independent signer`() {
        // Arrange
        val hashBytes = hash.hexToBytes()
        val reference = Sign.signMessage(hashBytes, keyPair, false)
        val rawSignature = reference.r + reference.s
        val publicKey = publicKeyOf(keyPair)

        // Act
        val actual = formatter.format(signature = rawSignature, hash = hashBytes, publicKey = publicKey)

        // Assert
        val expected = Numeric.toHexString(reference.r + reference.s + reference.v).lowercase()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a card signature WHEN format THEN output is 65 bytes with a legacy v`() {
        // Arrange
        val hashBytes = hash.hexToBytes()
        val reference = Sign.signMessage(hashBytes, keyPair, false)

        // Act
        val actual = formatter.format(
            signature = reference.r + reference.s,
            hash = hashBytes,
            publicKey = publicKeyOf(keyPair),
        )

        // Assert
        assertThat(actual).hasLength(RSV_HEX_LENGTH)
        assertThat(actual.takeLast(2)).isAnyOf("1b", "1c")
    }

    private fun publicKeyOf(keyPair: ECKeyPair): Wallet.PublicKey {
        val uncompressed = "04" + Numeric.toHexStringNoPrefix(keyPair.publicKey).padStart(UNCOMPRESSED_BODY, '0')
        val keyBytes = uncompressed.hexToBytes()
        return Wallet.PublicKey(
            seedKey = keyBytes,
            derivationType = Wallet.PublicKey.DerivationType.Plain(
                Wallet.HDKey(
                    extendedPublicKey = ExtendedPublicKey(publicKey = keyBytes, chainCode = ByteArray(CHAIN_CODE)),
                    path = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH),
                ),
            ),
        )
    }

    private companion object {
        const val SAMPLE_PRIVATE_KEY = 12345L
        const val HASH_SIZE = 32
        const val CHAIN_CODE = 32
        const val RSV_HEX_LENGTH = 132
        const val UNCOMPRESSED_BODY = 128
    }
}