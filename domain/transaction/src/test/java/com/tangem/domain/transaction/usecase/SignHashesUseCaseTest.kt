package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemError
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignHashesError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SignHashesUseCaseTest {

    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val getHotTransactionSigner: (UserWallet.Hot) -> TransactionSigner = mockk()

    private val useCase = SignHashesUseCase(
        cardSdkConfigRepository = cardSdkConfigRepository,
        getHotTransactionSigner = getHotTransactionSigner,
    )

    private val hashes = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
    private val signatures = listOf(byteArrayOf(7, 8, 9), byteArrayOf(10, 11, 12))

    @Test
    fun `GIVEN cold wallet with secp256k1 key WHEN invoke THEN signs hashes with common signer`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()
        val signer: TransactionSigner = mockk()
        val publicKeySlot = slot<Wallet.PublicKey>()

        every { cardSdkConfigRepository.getCommonSigner(any(), any()) } returns signer
        coEvery { signer.sign(eq(hashes), capture(publicKeySlot)) } returns CompletionResult.Success(signatures)

        // Act
        val result = useCase(coldWallet, hashes)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(signatures)
        // Wallet master secp256k1 key is used, without network derivation
        assertThat(publicKeySlot.captured.seedKey).isEqualTo(EllipticCurve.Secp256k1.name.toByteArray())
        assertThat(publicKeySlot.captured.derivationType).isNull()
        // Card is not backed up (backupStatus == null) and not a twin, so its id is passed to the signer
        verify(exactly = 1) { cardSdkConfigRepository.getCommonSigner(cardId = coldWallet.cardId, twinKey = null) }
    }

    @Test
    fun `GIVEN hot wallet with secp256k1 key WHEN invoke THEN signs hashes with hot signer`() = runTest {
        // Arrange
        val hotWallet = mockk<UserWallet.Hot> {
            every { wallets } returns listOf(mobileWallet(curve = EllipticCurve.Secp256k1, publicKey = byteArrayOf(42)))
        }
        val signer: TransactionSigner = mockk()
        val publicKeySlot = slot<Wallet.PublicKey>()

        every { getHotTransactionSigner(hotWallet) } returns signer
        coEvery { signer.sign(eq(hashes), capture(publicKeySlot)) } returns CompletionResult.Success(signatures)

        // Act
        val result = useCase(hotWallet, hashes)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(signatures)
        assertThat(publicKeySlot.captured.seedKey).isEqualTo(byteArrayOf(42))
        assertThat(publicKeySlot.captured.derivationType).isNull()
        verify(exactly = 1) { getHotTransactionSigner(hotWallet) }
    }

    @Test
    fun `GIVEN locked wallet without signing key WHEN invoke THEN returns NoSigningKey`() = runTest {
        // Arrange
        val lockedWallet = mockk<UserWallet.Hot> {
            every { wallets } returns null
        }

        // Act
        val result = useCase(lockedWallet, hashes)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SignHashesError.NoSigningKey)
        verify(exactly = 0) { getHotTransactionSigner(any()) }
    }

    @Test
    fun `GIVEN signer fails WHEN invoke THEN returns SigningFailed with error message`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()
        val signer: TransactionSigner = mockk()
        val error: TangemError = mockk { every { message } returns "Signing canceled" }

        every { cardSdkConfigRepository.getCommonSigner(any(), any()) } returns signer
        coEvery { signer.sign(any<List<ByteArray>>(), any()) } returns CompletionResult.Failure(error)

        // Act
        val result = useCase(coldWallet, hashes)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SignHashesError.SigningFailed(message = "Signing canceled"))
    }

    @Test
    fun `GIVEN empty hashes WHEN invoke THEN returns empty list without signing`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()

        // Act
        val result = useCase(coldWallet, hashes = emptyList())

        // Assert
        assertThat(result.getOrNull()).isEmpty()
        verify(exactly = 0) { cardSdkConfigRepository.getCommonSigner(any(), any()) }
        verify(exactly = 0) { getHotTransactionSigner(any()) }
    }

    private fun mobileWallet(curve: EllipticCurve, publicKey: ByteArray): MobileWallet = MobileWallet(
        publicKey = publicKey,
        chainCode = null,
        curve = curve,
        derivedKeys = emptyMap(),
    )
}