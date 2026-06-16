package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.Secp256k1
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.VerifyMessagesError
import com.tangem.utils.extensions.hexToBytes
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.MessageDigest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VerifySecp256k1MessagesUseCaseTest {

    private val useCase = VerifySecp256k1MessagesUseCase()

    // A valid secp256k1 key pair. The card signs the raw SHA-256 digest of each message.
    private val privateKey = "FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632550".hexToBytes()
    private lateinit var publicKey: ByteArray

    @BeforeAll
    fun initCrypto() {
        CryptoUtils.initCrypto()
        publicKey = CryptoUtils.generatePublicKey(privateKey, EllipticCurve.Secp256k1)
    }

    @Test
    fun `GIVEN every signature matches its message WHEN invoke THEN all results are true`() {
        // Arrange
        val messages = listOf("first".toByteArray(), "second".toByteArray())
        val signatures = messages.map(::sign)

        // Act
        val result = useCase(walletWithKey(publicKey), messages, signatures)

        // Assert
        assertThat(result.getOrNull()).containsExactly(true, true).inOrder()
    }

    @Test
    fun `GIVEN one signature is for a different message WHEN invoke THEN only that result is false`() {
        // Arrange
        val messages = listOf("first".toByteArray(), "second".toByteArray())
        val signatures = listOf(sign(messages[0]), sign("tampered".toByteArray()))

        // Act
        val result = useCase(walletWithKey(publicKey), messages, signatures)

        // Assert
        assertThat(result.getOrNull()).containsExactly(true, false).inOrder()
    }

    @Test
    fun `GIVEN signature was made by another wallet WHEN invoke THEN result is false`() {
        // Arrange
        val message = "first".toByteArray()
        val signatures = listOf(sign(message))
        val otherPublicKey = CryptoUtils.generatePublicKey(
            "589AEAE0EF93D7A0D7DAA8EB67E96AB02C2D8E5C0FB3D5F8BB2A03B6B2C2DF89".hexToBytes(),
            EllipticCurve.Secp256k1,
        )

        // Act
        val result = useCase(walletWithKey(otherPublicKey), listOf(message), signatures)

        // Assert
        assertThat(result.getOrNull()).containsExactly(false)
    }

    @Test
    fun `GIVEN fewer signatures than messages WHEN invoke THEN missing ones are false`() {
        // Arrange
        val messages = listOf("first".toByteArray(), "second".toByteArray())
        val signatures = listOf(sign(messages[0]))

        // Act
        val result = useCase(walletWithKey(publicKey), messages, signatures)

        // Assert
        assertThat(result.getOrNull()).containsExactly(true, false).inOrder()
    }

    @Test
    fun `GIVEN no messages WHEN invoke THEN returns empty list`() {
        // Act
        val result = useCase(walletWithKey(publicKey), messages = emptyList(), signatures = emptyList())

        // Assert
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `GIVEN locked wallet without signing key WHEN invoke THEN returns NoSigningKey`() {
        // Arrange
        val lockedWallet = mockk<UserWallet.Hot> { every { wallets } returns null }

        // Act
        val result = useCase(lockedWallet, listOf("first".toByteArray()), listOf(byteArrayOf(1)))

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(VerifyMessagesError.NoSigningKey)
    }

    /** Signs the raw SHA-256 digest of [message], mirroring what a Tangem card produces. */
    private fun sign(message: ByteArray): ByteArray {
        val hash = MessageDigest.getInstance("SHA-256").digest(message)
        return Secp256k1.ecdsaSignDigest(hash, privateKey)
    }

    private fun walletWithKey(key: ByteArray): UserWallet.Hot = mockk {
        every { wallets } returns listOf(
            MobileWallet(publicKey = key, chainCode = null, curve = EllipticCurve.Secp256k1, derivedKeys = emptyMap()),
        )
    }
}