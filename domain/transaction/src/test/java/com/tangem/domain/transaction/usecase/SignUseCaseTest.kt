package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SignUseCaseTest {

    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val getHotTransactionSigner: (UserWallet.Hot) -> TransactionSigner = mockk()

    private val useCase = SignUseCase(
        cardSdkConfigRepository = cardSdkConfigRepository,
        walletManagersFacade = walletManagersFacade,
        getHotTransactionSigner = getHotTransactionSigner,
    )

    private val publicKey = byteArrayOf(42, 43, 44)
    private val hashes = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
    private val signatures = listOf(byteArrayOf(7, 8, 9), byteArrayOf(10, 11, 12))

    @Test
    fun `GIVEN cold wallet WHEN sign single hash THEN signs with the wallet-manager key for the network`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()
        val network: Network = mockk()
        val signer: TransactionSigner = mockk()
        val walletManagerKey = Wallet.PublicKey(seedKey = byteArrayOf(50, 51), derivationType = null)
        val walletManager: WalletManager = mockk { every { wallet } returns mockk { every { publicKey } returns walletManagerKey } }
        val hash = byteArrayOf(1, 2, 3)
        val signature = byteArrayOf(9, 9)

        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any()) } returns signer
        coEvery { walletManagersFacade.getOrCreateWalletManager(coldWallet.walletId, network) } returns walletManager
        coEvery { signer.sign(eq(hash), eq(walletManagerKey)) } returns CompletionResult.Success(signature)

        // Act
        val result = useCase(hash = hash, userWallet = coldWallet, network = network)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(signature)
    }

    @Test
    fun `GIVEN signer fails WHEN sign single hash THEN returns the TangemError`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()
        val network: Network = mockk()
        val signer: TransactionSigner = mockk()
        val walletManagerKey = Wallet.PublicKey(seedKey = byteArrayOf(50, 51), derivationType = null)
        val walletManager: WalletManager = mockk { every { wallet } returns mockk { every { publicKey } returns walletManagerKey } }
        val error: TangemError = mockk()

        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any()) } returns signer
        coEvery { walletManagersFacade.getOrCreateWalletManager(coldWallet.walletId, network) } returns walletManager
        coEvery { signer.sign(any<ByteArray>(), any()) } returns CompletionResult.Failure(error)

        // Act
        val result = useCase(hash = byteArrayOf(1), userWallet = coldWallet, network = network)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(error)
    }

    @Test
    fun `GIVEN cold wallet WHEN sign hashes THEN signs with common signer and given key`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()
        val signer: TransactionSigner = mockk()
        val publicKeySlot = slot<Wallet.PublicKey>()

        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any()) } returns signer
        coEvery { signer.sign(eq(hashes), capture(publicKeySlot)) } returns CompletionResult.Success(signatures)

        // Act
        val result = useCase(hashes = hashes, publicKey = publicKey, userWallet = coldWallet)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(signatures)
        // The caller-provided key is used verbatim, with no network derivation
        assertThat(publicKeySlot.captured.seedKey).isEqualTo(publicKey)
        assertThat(publicKeySlot.captured.derivationType).isNull()
        // Card is not backed up (backupStatus == null) and not a twin, so its id is passed to the signer
        verify(exactly = 1) { cardSdkConfigRepository.getCommonSigner(cardId = coldWallet.cardId, twinKey = null, any()) }
    }

    @Test
    fun `GIVEN hot wallet WHEN sign hashes THEN signs with hot signer and given key`() = runTest {
        // Arrange
        val hotWallet = mockk<UserWallet.Hot>()
        val signer: TransactionSigner = mockk()
        val publicKeySlot = slot<Wallet.PublicKey>()

        every { getHotTransactionSigner(hotWallet) } returns signer
        coEvery { signer.sign(eq(hashes), capture(publicKeySlot)) } returns CompletionResult.Success(signatures)

        // Act
        val result = useCase(hashes = hashes, publicKey = publicKey, userWallet = hotWallet)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(signatures)
        assertThat(publicKeySlot.captured.seedKey).isEqualTo(publicKey)
        verify(exactly = 1) { getHotTransactionSigner(hotWallet) }
    }

    @Test
    fun `GIVEN signer fails WHEN sign hashes THEN returns SigningFailed with error message`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()
        val signer: TransactionSigner = mockk()
        val error: TangemError = mockk { every { message } returns "Signing canceled" }

        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any()) } returns signer
        coEvery { signer.sign(any<List<ByteArray>>(), any()) } returns CompletionResult.Failure(error)

        // Act
        val result = useCase(hashes = hashes, publicKey = publicKey, userWallet = coldWallet)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SignHashesError.SigningFailed(message = "Signing canceled"))
    }

    @Test
    fun `GIVEN empty hashes WHEN sign hashes THEN returns empty list without signing`() = runTest {
        // Arrange
        val coldWallet = MockUserWalletFactory.create()

        // Act
        val result = useCase(hashes = emptyList(), publicKey = publicKey, userWallet = coldWallet)

        // Assert
        assertThat(result.getOrNull()).isEmpty()
        verify(exactly = 0) { cardSdkConfigRepository.getCommonSigner(any(), any(), any()) }
        verify(exactly = 0) { getHotTransactionSigner(any()) }
    }
}