package com.tangem.domain.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.spongycastle.crypto.digests.KeccakDigest

/**
 * Vectors produced by the backend itself cross-checked against `predictSafeAddress` from `@safe-global/protocol-kit`
 * on a live node.
 *
 * The owners are joint owner keys (`m/44'/60'/888888'/0/{index}`) of the standard test mnemonic
 * "test test test test test test test test test test test junk". They are mixed-case on purpose:
 * a case-sensitive sort reorders them, because 'B' (0x42) sorts before 'a' (0x61) while 'b' sorts
 * after it.
 *
 * If a vector fails, the produced value tells which step is wrong:
 * - `0x4C3C6b28…` / `0xB6097024…` / `0xCdd466ce…` / `0xFa05Fd83…` — owners sorted without lowercasing first;
 * - `0x908003D4…` (3-of-5) — owners not sorted at all;
 * - the address from the other singleton's column — the singleton is mixed up.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SafeAddressTest {

    @ParameterizedTest
    @ProvideTestModels
    fun compute(model: VectorModel) {
        // Act
        val actual = SafeAddress.compute(
            owners = OWNERS.take(model.members),
            threshold = model.threshold,
            singleton = model.singleton,
        )

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        VectorModel(members = 2, threshold = 2, SafeSingleton.SAFE, "0xe7411b2aB7b76a11ca0E6174697714983fC09Cb2"),
        VectorModel(members = 2, threshold = 1, SafeSingleton.SAFE, "0xb8Ec0Febbcee24b2AAB1F04FB8E569b108A51160"),
        VectorModel(members = 3, threshold = 2, SafeSingleton.SAFE, "0x9295a5836685C5F5f42242f9B64d23E46F8c259F"),
        VectorModel(members = 5, threshold = 3, SafeSingleton.SAFE, "0x735F25C3e27b370a8424A8935B9B08a5569F1145"),
        VectorModel(members = 5, threshold = 5, SafeSingleton.SAFE, "0x29502c6147A7aA296253E297F62f603aACb16429"),
        VectorModel(members = 2, threshold = 2, SafeSingleton.SAFE_L2, "0x01460aBb9BE7084b817849e856F249e4cdD73E84"),
        VectorModel(members = 2, threshold = 1, SafeSingleton.SAFE_L2, "0x13a6c159fbe94D29Ce337f16D810174BbDfC8368"),
        VectorModel(members = 3, threshold = 2, SafeSingleton.SAFE_L2, "0x30bE590176A93C6Ef687F3b07421A3624deaf476"),
        VectorModel(members = 5, threshold = 3, SafeSingleton.SAFE_L2, "0x6691785f8458a46b5f080F610f86c4a5B3B0A86b"),
        VectorModel(members = 5, threshold = 5, SafeSingleton.SAFE_L2, "0x151bd350ec4dA327a2eF9d711E855614D57F1fb1"),
    )

    @Test
    fun `GIVEN 2-of-2 vector WHEN build initializer THEN bytes and salt match the backend intermediates`() {
        // Act
        val initializer = SafeAddress.initializer(owners = OWNERS.take(2), threshold = 2)
        val salt = (initializer.keccak() + ByteArray(size = 32)).keccak()

        // Assert
        assertThat(initializer.toHexString().lowercase()).isEqualTo(EXPECTED_INITIALIZER_2_OF_2)
        assertThat(initializer.keccak().toHexString().lowercase())
            .isEqualTo("243e7295bbdb34d1fb79dc8de1012aae3b8272dcfad97252eb1d88b01b68af80")
        assertThat(salt.toHexString().lowercase())
            .isEqualTo("9218b0c85d8557a14eed0ca93a9abf7f67db02b365524658a80e2fd0c211b307")
    }

    @Test
    fun `GIVEN shuffled uppercase owners WHEN compute THEN address is the same`() {
        // Arrange
        val shuffled = OWNERS.reversed().map { it.uppercase().replace(oldValue = "0X", newValue = "0x") }

        // Act
        val actual = SafeAddress.compute(owners = shuffled, threshold = 3, singleton = SafeSingleton.SAFE)

        // Assert
        val expected = SafeAddress.compute(owners = OWNERS, threshold = 3, singleton = SafeSingleton.SAFE)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN proxy creation code WHEN hash THEN matches the factory blob hash`() {
        // Act
        val actual = SafeAddress.PROXY_CREATION_CODE.hexToBytes().keccak()

        // Assert
        assertThat(actual.toHexString().lowercase())
            .isEqualTo("941b3e88811b2f33b8e26783c7407d2e978404581a21c9f07abf2cad9cb87e12")
    }

    private fun ByteArray.keccak(): ByteArray {
        val digest = KeccakDigest(KECCAK_BITS)
        digest.update(this, 0, size)
        return ByteArray(digest.digestSize).also { digest.doFinal(it, 0) }
    }

    internal data class VectorModel(
        val members: Int,
        val threshold: Int,
        val singleton: SafeSingleton,
        val expected: String,
    )

    private companion object {

        const val KECCAK_BITS = 256

        val OWNERS = listOf(
            "0xa9936482d7d056d6f843f5C6088Ff0123b796bCC", // m/44'/60'/888888'/0/3
            "0xB8C09e7abE7e0F70f4F4cAf48eED46cc8BfdA120", // .../30
            "0xD048326c01787c475Af00C51eb0221d2557bfEc2", // .../24
            "0xb79e9A68F9cB4549E026c4c8fa7F1b1A548ae1Aa", // .../7
            "0xF29F3B603773b3d5cC91cD77dEDA1E656381C7C1", // .../47
        )

        const val EXPECTED_INITIALIZER_2_OF_2 =
            "b63e800d00000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000" +
                "0000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000001600000000000000000000000003efcbb83a4a7" +
                "afcb4f68d501e2c2203a38be77f4000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000" +
                "00000000a9936482d7d056d6f843f5c6088ff0123b796bcc000000000000000000000000b8c09e7abe7e0f70f4f4caf48eed" +
                "46cc8bfda1200000000000000000000000000000000000000000000000000000000000000000"
    }
}