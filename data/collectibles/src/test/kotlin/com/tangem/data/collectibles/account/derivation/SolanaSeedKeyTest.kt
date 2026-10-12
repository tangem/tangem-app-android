package com.tangem.data.collectibles.account.derivation

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.config.CurvesConfig
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.utils.extensions.hexToBytes
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The owner is derived from this key and the vault's `cold_owner` is signed with it, so the two callers must read
 * the very same one — see [solanaSeedKey].
 */
internal class SolanaSeedKeyTest {

    private val solanaKey = "02AABB".hexToBytes()
    private val otherKey = "02CCDD".hexToBytes()

    @BeforeEach
    fun mockCurvesConfig() {
        mockkStatic("com.tangem.domain.wallets.config.CurvesConfigKt")
    }

    @AfterEach
    fun unmockCurvesConfig() {
        unmockkStatic("com.tangem.domain.wallets.config.CurvesConfigKt")
    }

    @Test
    fun `GIVEN a cold wallet WHEN solanaSeedKey THEN the key on the config's curve is taken`() {
        // Arrange
        val wallet = coldWallet(
            wallets = listOf(
                cardWallet(curve = EllipticCurve.Secp256k1, publicKey = otherKey),
                cardWallet(curve = EllipticCurve.Ed25519Slip0010, publicKey = solanaKey),
            ),
        )
        stubPrimaryCurve(wallet, EllipticCurve.Ed25519Slip0010)

        // Act
        val actual = wallet.solanaSeedKey()

        // Assert
        assertThat(actual).isEqualTo(solanaKey)
    }

    @Test
    fun `GIVEN an older cold wallet WHEN solanaSeedKey THEN the Ed25519 key is taken`() {
        // Arrange
        val wallet = coldWallet(
            wallets = listOf(
                cardWallet(curve = EllipticCurve.Ed25519, publicKey = solanaKey),
                cardWallet(curve = EllipticCurve.Ed25519Slip0010, publicKey = otherKey),
            ),
        )
        stubPrimaryCurve(wallet, EllipticCurve.Ed25519)

        // Act
        val actual = wallet.solanaSeedKey()

        // Assert
        assertThat(actual).isEqualTo(solanaKey)
    }

    @Test
    fun `GIVEN no key on the curve WHEN solanaSeedKey THEN it is null`() {
        // Arrange
        val wallet = coldWallet(wallets = listOf(cardWallet(EllipticCurve.Secp256k1, otherKey)))
        stubPrimaryCurve(wallet, EllipticCurve.Ed25519Slip0010)

        // Act
        val actual = wallet.solanaSeedKey()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN the config has no Solana curve WHEN solanaSeedKey THEN it is null`() {
        // Arrange
        val wallet = coldWallet(wallets = listOf(cardWallet(EllipticCurve.Ed25519Slip0010, solanaKey)))
        stubPrimaryCurve(wallet, curve = null)

        // Act
        val actual = wallet.solanaSeedKey()

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a hot wallet WHEN solanaSeedKey THEN the key on the config's curve is taken`() {
        // Arrange
        val wallet = hotWallet(
            wallets = listOf(
                mobileWallet(curve = EllipticCurve.Secp256k1, publicKey = otherKey),
                mobileWallet(curve = EllipticCurve.Ed25519Slip0010, publicKey = solanaKey),
            ),
        )
        stubPrimaryCurve(wallet, EllipticCurve.Ed25519Slip0010)

        // Act
        val actual = wallet.solanaSeedKey()

        // Assert
        assertThat(actual).isEqualTo(solanaKey)
    }

    @Test
    fun `GIVEN a locked hot wallet WHEN solanaSeedKey THEN it is null`() {
        // Arrange
        val wallet = hotWallet(wallets = null)
        stubPrimaryCurve(wallet, EllipticCurve.Ed25519Slip0010)

        // Act
        val actual = wallet.solanaSeedKey()

        // Assert
        assertThat(actual).isNull()
    }

    private fun stubPrimaryCurve(wallet: UserWallet, curve: EllipticCurve?) {
        every { wallet.curvesConfig } returns mockk<CurvesConfig> {
            every { primaryCurve(any()) } returns curve
        }
    }

    private fun coldWallet(wallets: List<CardDTO.Wallet>): UserWallet.Cold = mockk {
        every { scanResponse } returns mockk {
            every { card } returns mockk {
                every { this@mockk.wallets } returns wallets
            }
        }
    }

    private fun hotWallet(wallets: List<MobileWallet>?): UserWallet.Hot = mockk {
        every { this@mockk.wallets } returns wallets
    }

    private fun cardWallet(curve: EllipticCurve, publicKey: ByteArray): CardDTO.Wallet = mockk {
        every { this@mockk.curve } returns curve
        every { this@mockk.publicKey } returns publicKey
    }

    private fun mobileWallet(curve: EllipticCurve, publicKey: ByteArray) = MobileWallet(
        publicKey = publicKey,
        chainCode = null,
        curve = curve,
        derivedKeys = emptyMap(),
    )
}