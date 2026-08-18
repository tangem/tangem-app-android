package com.tangem.domain.jointaccount

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.BIP32
import com.tangem.crypto.hdWallet.bip32.ExtendedPrivateKey
import com.tangem.domain.jointaccount.model.JointAccountCreationPayload
import com.tangem.domain.jointaccount.signing.CanonicalJson
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.crypto.params.ECDomainParameters
import org.spongycastle.crypto.params.ECPublicKeyParameters
import org.spongycastle.crypto.signers.ECDSASigner
import java.math.BigInteger
import java.text.Normalizer
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cross-platform test vector for joint account creation, to be run against the same constants on iOS and the
 * backend: one mnemonic in — the owner addresses, the canonical payload bytes and the EIP-191 digest must match on
 * every platform byte for byte. Any divergence here means the backend will reject 100% of that platform's
 * signatures.
 *
 * The BIP32/BIP39 helpers are deliberately implemented inside the test (hardened derivation is done by the card or
 * the hot SDK in production and has no pure-JVM API): an independent implementation converging with the SDK's
 * CKDpub — asserted below — is exactly what a vector needs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountCrossPlatformVectorTest {

    @BeforeAll
    fun initCrypto() {
        CryptoUtils.initCrypto()
    }

    @Test
    fun `GIVEN vector mnemonic WHEN derive seed THEN matches BIP39`() {
        // Act
        val seed = bip39Seed(MNEMONIC, PASSPHRASE)

        // Assert
        assertThat(seed.toHex()).isEqualTo(EXPECTED_SEED_HEX)
    }

    @Test
    fun `GIVEN vector mnemonic WHEN derive owner addresses THEN match the cross-platform constants`() {
        // Act
        val address0 = evmAddress(uncompressedPublicKey(ownerPrivateKey(index = 0)))
        val address1 = evmAddress(uncompressedPublicKey(ownerPrivateKey(index = 1)))

        // Assert
        assertThat(address0).isEqualTo(EXPECTED_OWNER_ADDRESS_0)
        assertThat(address1).isEqualTo(EXPECTED_OWNER_ADDRESS_1)
    }

    @Test
    fun `GIVEN account xpub WHEN derive last two nodes via SDK CKDpub THEN keys converge with CKDpriv`() {
        // Arrange
        val accountLevel = hardenedAccountLevelKey()

        // Act
        val sdkDerived = accountLevel.makePublicKey(EllipticCurve.Secp256k1)
            .derivePublicKey(DerivationPath(rawPath = "m/0/0"))
        val testDerived = compressedPublicKey(ownerPrivateKey(index = 0))

        // Assert
        assertThat(sdkDerived.publicKey.toHex()).isEqualTo(testDerived.toHex())
    }

    @Test
    fun `GIVEN vector payload WHEN canonicalize and hash THEN match the cross-platform constants`() {
        // Act
        val canonical = CanonicalJson.canonicalize(vectorPayload().toCanonicalMap())
        val digest = eip191Digest(canonical)

        // Assert
        assertThat(canonical.toString(Charsets.UTF_8)).isEqualTo(EXPECTED_CANONICAL_PAYLOAD)
        assertThat(digest.toHex()).isEqualTo(EXPECTED_EIP191_DIGEST_HEX)
    }

    @Test
    fun `GIVEN vector digest WHEN sign with owner key THEN signature verifies against owner public key`() {
        // Arrange
        val privateKey = ownerPrivateKey(index = 0)
        val digest = eip191Digest(CanonicalJson.canonicalize(vectorPayload().toCanonicalMap()))

        // Act
        val signature = Secp256k1.ecdsaSignDigest(digest, privateKey)

        // Assert
        assertThat(signature).hasLength(64)
        assertThat(verifyDigest(compressedPublicKey(privateKey), digest, signature)).isTrue()
    }

    private fun vectorPayload() = JointAccountCreationPayload(
        config = JointAccountCreationPayload.Config(
            name = "Family",
            icon = "Family",
            iconColor = "Azure",
            membersCount = 3,
            threshold = 2,
        ),
        creator = JointAccountCreationPayload.Creator(
            walletId = VECTOR_WALLET_ID,
            name = "Alice",
            address = EXPECTED_OWNER_ADDRESS_0,
            derivation = 0,
        ),
    )

    // region BIP39/BIP32 test-only implementation

    private fun bip39Seed(mnemonic: String, passphrase: String): ByteArray {
        val normalizedMnemonic = Normalizer.normalize(mnemonic, Normalizer.Form.NFKD)
        val salt = Normalizer.normalize("mnemonic$passphrase", Normalizer.Form.NFKD)
        val spec = PBEKeySpec(normalizedMnemonic.toCharArray(), salt.toByteArray(), PBKDF2_ITERATIONS, SEED_BITS)

        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
    }

    /** The `m/44'/60'/888888'` level — everything below it is non-hardened and CKDpub-derivable. */
    private fun hardenedAccountLevelKey(): ExtendedPrivateKey {
        val master = BIP32.makeMasterKey(bip39Seed(MNEMONIC, PASSPHRASE), EllipticCurve.Secp256k1)

        return listOf(44L, 60L, 888888L).fold(master) { parent, index ->
            ckdPriv(parent, index + HARDENED_OFFSET)
        }
    }

    private fun ownerPrivateKey(index: Long): ByteArray {
        return listOf(0L, index).fold(hardenedAccountLevelKey()) { parent, node -> ckdPriv(parent, node) }
            .privateKey
    }

    private fun ckdPriv(parent: ExtendedPrivateKey, index: Long): ExtendedPrivateKey {
        val indexBytes = index.toUInt32Bytes()
        val data = if (index >= HARDENED_OFFSET) {
            byteArrayOf(0) + parent.privateKey + indexBytes
        } else {
            compressedPublicKey(parent.privateKey) + indexBytes
        }

        val i = hmacSha512(key = parent.chainCode, data = data)
        val childKey = (BigInteger(1, i.copyOfRange(0, 32)) + BigInteger(1, parent.privateKey)).mod(SECP256K1_N)
        check(childKey.signum() != 0) { "Invalid child key" }

        return ExtendedPrivateKey(privateKey = childKey.to32Bytes(), chainCode = i.copyOfRange(32, 64))
    }

    private fun compressedPublicKey(privateKey: ByteArray): ByteArray {
        return CryptoUtils.generatePublicKey(privateKey, EllipticCurve.Secp256k1, compressed = true)
    }

    private fun uncompressedPublicKey(privateKey: ByteArray): ByteArray {
        return CryptoUtils.generatePublicKey(privateKey, EllipticCurve.Secp256k1, compressed = false)
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        return Mac.getInstance("HmacSHA512")
            .apply { init(SecretKeySpec(key, "HmacSHA512")) }
            .doFinal(data)
    }

    // endregion

    // region EVM address + EIP-191 test-only implementation

    private fun evmAddress(uncompressedPublicKey: ByteArray): String {
        val addressBytes = keccak256(uncompressedPublicKey.copyOfRange(1, uncompressedPublicKey.size))
            .copyOfRange(12, 32)

        return eip55Checksum(addressBytes.toHex())
    }

    private fun eip55Checksum(addressHexLowercase: String): String {
        val checksumDigest = keccak256(addressHexLowercase.toByteArray(Charsets.US_ASCII)).toHex()

        val checksummed = addressHexLowercase
            .mapIndexed { i, char -> if (checksumDigest[i].digitToInt(radix = 16) >= 8) char.uppercaseChar() else char }
            .joinToString(separator = "")

        return "0x$checksummed"
    }

    private fun eip191Digest(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n${message.size}".toByteArray()

        return keccak256(prefix + message)
    }

    private fun keccak256(data: ByteArray): ByteArray {
        val digest = KeccakDigest(KECCAK_BITS)
        digest.update(data, 0, data.size)

        return ByteArray(digest.digestSize).also { digest.doFinal(it, 0) }
    }

    private fun verifyDigest(compressedPublicKey: ByteArray, digest: ByteArray, signature: ByteArray): Boolean {
        val params = SECNamedCurves.getByName("secp256k1")
        val domain = ECDomainParameters(params.curve, params.g, params.n, params.h)
        val publicKeyParams = ECPublicKeyParameters(params.curve.decodePoint(compressedPublicKey), domain)
        val verifier = ECDSASigner().apply { init(false, publicKeyParams) }

        return verifier.verifySignature(
            digest,
            BigInteger(1, signature.copyOfRange(0, 32)),
            BigInteger(1, signature.copyOfRange(32, 64)),
        )
    }

    // endregion

    private fun ByteArray.toHex(): String = joinToString(separator = "") { "%02x".format(it) }

    private fun Long.toUInt32Bytes(): ByteArray {
        return byteArrayOf((this shr 24).toByte(), (this shr 16).toByte(), (this shr 8).toByte(), this.toByte())
    }

    private fun BigInteger.to32Bytes(): ByteArray {
        val raw = toByteArray()

        return when {
            raw.size == 32 -> raw
            raw.size > 32 -> raw.copyOfRange(raw.size - 32, raw.size)
            else -> ByteArray(32 - raw.size) + raw
        }
    }

    private companion object {

        // ===== The shared cross-platform vector: iOS and the backend must reproduce every constant below =====

        const val MNEMONIC = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon " +
            "abandon about"
        const val PASSPHRASE = ""
        const val VECTOR_WALLET_ID = "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"

        const val EXPECTED_SEED_HEX = "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc1" +
            "9a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4"
        const val EXPECTED_OWNER_ADDRESS_0 = "0xE31C6A9eE83A0f6f2e44dDcb9837B162802DeC12"
        const val EXPECTED_OWNER_ADDRESS_1 = "0x6eea5039392f53AB7E6C07D9Cf1f6fe2b753144F"
        const val EXPECTED_CANONICAL_PAYLOAD = """{"config":{"icon":"Family","iconColor":"Azure",""" +
            """"membersCount":3,"name":"Family","threshold":2},""" +
            """"creator":{"address":"0xE31C6A9eE83A0f6f2e44dDcb9837B162802DeC12","derivation":0,""" +
            """"name":"Alice","walletId":"4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6"}}"""
        const val EXPECTED_EIP191_DIGEST_HEX = "e6d9ac30fb18e01faba90cda8249f91ca17e427be57390f8f50964731eb753c2"

        const val PBKDF2_ITERATIONS = 2048
        const val SEED_BITS = 512
        const val KECCAK_BITS = 256
        const val HARDENED_OFFSET = 0x80000000L
        val SECP256K1_N = BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16)
    }
}