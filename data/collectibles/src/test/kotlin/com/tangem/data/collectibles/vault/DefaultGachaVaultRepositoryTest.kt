package com.tangem.data.collectibles.vault

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.collectibles.models.CollectiblesVaultUsdc
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.common.core.TangemSdkError
import com.tangem.data.collectibles.vault.transaction.GachaVaultSigners
import com.tangem.data.collectibles.vault.transaction.GaslessGachaVaultTransactionSender
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.delegate.CollectiblesSessionKey
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.test.mock.collectibles.MockCollectibles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Base64

internal class DefaultGachaVaultRepositoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val networkProvider: CollectiblesOwnerNetworkProvider = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val hotWalletSignerFactory: TangemHotWalletSigner.Factory = mockk()
    private val gaslessSender: GaslessGachaVaultTransactionSender = mockk()
    private val sessionKey: CollectiblesSessionKey = mockk()

    private val currencyFactory = CollectiblesVaultCurrencyFactory(MockCollectibles::usdc)

    private val walletManager: WalletManager = mockk()
    private val signer: TangemHotWalletSigner = mockk()
    private val network: Network = mockk(relaxed = true)

    private val userWallet: UserWallet.Hot = mockk { every { walletId } returns WALLET_ID }

    private val repository = DefaultGachaVaultRepository(
        walletManagersFacade = walletManagersFacade,
        networkProvider = networkProvider,
        currencyFactory = currencyFactory,
        ownerDeriver = ownerDeriver,
        signers = GachaVaultSigners(cardSdkConfigRepository, hotWalletSignerFactory),
        gaslessSender = gaslessSender,
        sessionKey = sessionKey,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetUpVault {

        @BeforeEach
        fun resetMocks() {
            clearMocks(gaslessSender, ownerDeriver)
            coEvery { gaslessSender.initOrUpdateVault(any(), any(), any()) } returns Unit.right()
            coEvery { gaslessSender.deriveOwnerAndSetUpVault(any(), any(), any()) } returns Unit.right()
        }

        @Test
        fun `GIVEN a mobile wallet WHEN initOrUpdateVault THEN the gasless sender sets it up`() = runTest {
            // Act
            val actual = repository.initOrUpdateVault(userWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { gaslessSender.initOrUpdateVault(userWallet, SESSION_KEY, LIMITS) }
        }

        @Test
        fun `GIVEN a card WHEN initOrUpdateVault THEN the gasless sender sets it up`() = runTest {
            // Arrange
            val coldWallet: UserWallet.Cold = mockk { every { walletId } returns WALLET_ID }

            // Act
            val actual = repository.initOrUpdateVault(coldWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { gaslessSender.initOrUpdateVault(coldWallet, SESSION_KEY, LIMITS) }
        }

        @Test
        fun `GIVEN a card WHEN deriveOwnerAndSetUpVault THEN the gasless sender sets it up`() = runTest {
            // Arrange
            val coldWallet: UserWallet.Cold = mockk { every { walletId } returns WALLET_ID }

            // Act
            val actual = repository.deriveOwnerAndSetUpVault(coldWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) { gaslessSender.deriveOwnerAndSetUpVault(coldWallet, SESSION_KEY, LIMITS) }
        }

        @Test
        fun `GIVEN a mobile wallet WHEN deriveOwnerAndSetUpVault THEN the owner is derived before the set-up`() =
            runTest {
                // Arrange
                coEvery { ownerDeriver.deriveOwner(WALLET_ID) } returns OWNER.right()

                // Act
                val actual = repository.deriveOwnerAndSetUpVault(userWallet, SESSION_KEY, LIMITS)

                // Assert
                assertThat(actual).isEqualTo(Unit.right())
                coVerifyOrder {
                    ownerDeriver.deriveOwner(WALLET_ID)
                    gaslessSender.initOrUpdateVault(userWallet, SESSION_KEY, LIMITS)
                }
            }

        @Test
        fun `GIVEN the owner cannot be derived WHEN deriveOwnerAndSetUpVault THEN nothing is set up`() = runTest {
            // Arrange
            coEvery { ownerDeriver.deriveOwner(WALLET_ID) } returns CollectiblesAccountError.Cancelled.left()

            // Act
            val actual = repository.deriveOwnerAndSetUpVault(userWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Cancelled.left())
            coVerify(exactly = 0) { gaslessSender.initOrUpdateVault(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendPurchasePack {

        @BeforeEach
        fun resetMocks() {
            clearMocks(gaslessSender, sessionKey)
            coEvery { sessionKey.getPublicKeyOrNull(any()) } returns SESSION_KEY
        }

        @Test
        fun `GIVEN the sender pays WHEN sendPurchasePack THEN its hash is returned`() = runTest {
            // Arrange
            coEvery { gaslessSender.sendPurchasePack(userWallet, SESSION_KEY, AMOUNT, FEE_AMOUNT, MEMO, any()) } returns
                TX_HASH.right()

            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual).isEqualTo(TX_HASH.right())
        }

        @Test
        fun `GIVEN the session key fails to sign WHEN sendPurchasePack THEN its own error is returned`() = runTest {
            // Arrange
            val signingError = CollectiblesAccountError.SessionKeySigningFailed(IllegalStateException())
            coEvery { sessionKey.sign(any(), any()) } returns signingError.left()
            coEvery { gaslessSender.sendPurchasePack(any(), any(), any(), any(), any(), any()) } coAnswers {
                arg<suspend (ByteArray) -> Result<ByteArray>>(5).invoke(byteArrayOf(1))
                CollectiblesAccountError.Vault(GachaVaultError.Unexpected(IllegalStateException())).left()
            }

            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual).isEqualTo(signingError.left())
        }

        @Test
        fun `GIVEN the outcome is unknown WHEN sendPurchasePack THEN it is passed on`() = runTest {
            // Arrange
            val maybeSent = CollectiblesAccountError.MaybeSent(txHash = TX_HASH)
            coEvery { gaslessSender.sendPurchasePack(any(), any(), any(), any(), any(), any()) } returns
                maybeSent.left()

            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual).isEqualTo(maybeSent.left())
        }

        @Test
        fun `GIVEN the session key is not provisioned WHEN sendPurchasePack THEN nothing is sent`() = runTest {
            // Arrange
            coEvery { sessionKey.getPublicKeyOrNull(any()) } returns null

            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.SessionKeyNotProvisioned.left())
            coVerify(exactly = 0) { gaslessSender.sendPurchasePack(any(), any(), any(), any(), any(), any()) }
        }

        private suspend fun sendPurchasePack() = repository.sendPurchasePack(
            userWallet = userWallet,
            amount = AMOUNT,
            feeAmount = FEE_AMOUNT,
            memo = MEMO,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetVaultBalance {

        @BeforeEach
        fun resetMocks() {
            clearMocks(walletManagersFacade, networkProvider, walletManager)
            coEvery { networkProvider.create(userWallet) } returns network.right()
            coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) } returns walletManager
            coEvery { walletManager.getCollectiblesVaultAddress() } returns Result.Success(VAULT_ADDRESS)
            coEvery { walletManager.getCollectiblesVaultUsdc() } returns Result.Success(
                CollectiblesVaultUsdc(address = VAULT_USDC_ADDRESS, mint = MINT),
            )
        }

        @Test
        fun `GIVEN the vault holds the token WHEN getVaultBalance THEN its amount is returned`() = runTest {
            // Arrange
            stubVaultBalance(Result.Success(BigDecimal("12.5")))

            // Act
            val actual = repository.getVaultBalance(userWallet)

            // Assert
            assertThat(actual).isEqualTo(
                MockCollectibles.vaultBalance(
                    amount = BigDecimal("12.5"),
                    currency = expectedCurrency(),
                    vaultAddress = VAULT_ADDRESS,
                    ataAddress = VAULT_USDC_ADDRESS,
                ).right(),
            )
        }

        @Test
        fun `GIVEN a vault WHEN getVaultBalance THEN the balance is read for the vault's mint`() = runTest {
            // Arrange
            stubVaultBalance(Result.Success(BigDecimal.ZERO))

            // Act
            repository.getVaultBalance(userWallet)

            // Assert
            coVerify(exactly = 1) { walletManager.getCollectiblesVaultTokenBalance(mint = MINT) }
        }

        @Test
        fun `GIVEN the balance read fails WHEN getVaultBalance THEN an error is returned, not zero`() = runTest {
            // Arrange
            stubVaultBalance(Result.Failure(BlockchainSdkError.CustomError("rpc down")))

            // Act
            val actual = repository.getVaultBalance(userWallet)

            // Assert
            assertThat(actual.isLeft()).isTrue()
        }

        @Test
        fun `GIVEN the vault is already known WHEN getVaultBalance THEN the program is not asked for it again`() =
            runTest {
                // Arrange
                val known = MockCollectibles.vaultBalance(
                    amount = BigDecimal.ZERO,
                    currency = MockCollectibles.usdc(mint = MINT),
                )
                stubVaultBalance(Result.Success(BigDecimal.ONE))

                // Act
                val actual = repository.getVaultBalance(userWallet, cached = known)

                // Assert
                assertThat(actual.getOrNull()?.usdcToken).isEqualTo(known.usdcToken)
                assertThat(actual.getOrNull()?.ataAddress).isEqualTo(known.ataAddress)
                coVerify(exactly = 0) { walletManager.getCollectiblesVaultUsdc() }
            }

        @Test
        fun `GIVEN the config cannot be read WHEN getVaultBalance THEN no balance is read`() = runTest {
            // Arrange
            coEvery { walletManager.getCollectiblesVaultUsdc() } returns
                Result.Failure(BlockchainSdkError.CustomError("no config"))

            // Act
            val actual = repository.getVaultBalance(userWallet)

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) { walletManager.getCollectiblesVaultTokenBalance(any()) }
        }

        @Test
        fun `GIVEN the owner key is not derived WHEN getVaultBalance THEN no manager is built`() = runTest {
            // Arrange
            coEvery { networkProvider.create(userWallet) } returns GachaVaultError.WalletUnavailable.left()

            // Act
            val actual = repository.getVaultBalance(userWallet)

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) }
        }

        private fun expectedCurrency() = MockCollectibles.usdc(mint = MINT)

        private fun stubVaultBalance(result: Result<BigDecimal>) {
            coEvery { walletManager.getCollectiblesVaultTokenBalance(MINT) } returns result
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Withdraw {

        @BeforeEach
        fun resetMocks() {
            clearMocks(gaslessSender)
            coEvery { gaslessSender.withdraw(any(), any(), any(), any()) } returns TX_HASH.right()
        }

        @Test
        fun `GIVEN a mobile wallet WHEN withdraw THEN the gasless sender pays the wallet in base units`() = runTest {
            // Act
            val actual = repository.withdraw(userWallet, VAULT_TOKEN, RECIPIENT, BigDecimal("12.5"))

            // Assert
            assertThat(actual).isEqualTo(TX_HASH.right())
            coVerify(exactly = 1) {
                gaslessSender.withdraw(userWallet, MINT, RECIPIENT, 12_500_000L)
            }
        }

        @Test
        fun `GIVEN more decimals than USDC has WHEN withdraw THEN nothing is sent`() = runTest {
            // Act
            val actual = repository.withdraw(userWallet, VAULT_TOKEN, RECIPIENT, BigDecimal("0.0000001"))

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(CollectiblesAccountError.Vault::class.java)
            coVerify(exactly = 0) { gaslessSender.withdraw(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN the outcome is unknown WHEN withdraw THEN it is passed on`() = runTest {
            // Arrange
            val maybeSent = CollectiblesAccountError.MaybeSent(txHash = TX_HASH)
            coEvery { gaslessSender.withdraw(any(), any(), any(), any()) } returns maybeSent.left()

            // Act
            val actual = repository.withdraw(userWallet, VAULT_TOKEN, RECIPIENT, BigDecimal.ONE)

            // Assert
            assertThat(actual).isEqualTo(maybeSent.left())
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendWithOwnerSignature {

        private val coldWallet: UserWallet.Cold = mockk {
            every { walletId } returns WALLET_ID
            every { scanResponse } returns mockk {
                every { productType } returns ProductType.Wallet2
                every { card } returns mockk {
                    every { cardId } returns CARD_ID
                    every { backupStatus } returns CardDTO.BackupStatus.NoBackup
                }
            }
        }

        private val cardSigner: TransactionSigner = mockk()

        @BeforeEach
        fun resetMocks() {
            clearMocks(walletManagersFacade, networkProvider, walletManager, hotWalletSignerFactory)
            every { hotWalletSignerFactory.create(userWallet) } returns signer
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), WALLET_ID) } returns cardSigner
            coEvery { networkProvider.create(any()) } returns network.right()
            coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) } returns walletManager
            coEvery { walletManager.send(any<TransactionData>(), any(), any()) } returns
                Result.Success(TransactionSendResult(hash = TX_HASH))
        }

        @Test
        fun `GIVEN a mobile wallet WHEN sendWithOwnerSignature THEN the bytes are sent as is and the hash returned`() =
            runTest {
                // Arrange
                val bytes = solanaTransaction(instructionDataSize = LARGE_INSTRUCTION_DATA_SIZE)

                // Act
                val actual = repository.sendWithOwnerSignature(userWallet, bytes.toBase64())

                // Assert
                assertThat(actual).isEqualTo(TX_HASH.right())
                coVerify(exactly = 1) {
                    walletManager.send(
                        match<TransactionData.Compiled> { data ->
                            (data.value as TransactionData.Compiled.Data.Bytes).data.contentEquals(bytes)
                        },
                        signer,
                        any(),
                    )
                }
            }

        @Test
        fun `GIVEN a card wallet and a small transaction WHEN sent THEN the card signs it`() = runTest {
            // Arrange
            val transaction = solanaTransaction(instructionDataSize = 10).toBase64()

            // Act
            val actual = repository.sendWithOwnerSignature(coldWallet, transaction)

            // Assert
            assertThat(actual).isEqualTo(TX_HASH.right())
            coVerify(exactly = 1) { walletManager.send(any<TransactionData>(), cardSigner, any()) }
        }

        @Test
        fun `GIVEN a card wallet and a large transaction WHEN sent THEN it is refused before the tap`() = runTest {
            // Arrange
            val transaction = solanaTransaction(instructionDataSize = LARGE_INSTRUCTION_DATA_SIZE).toBase64()

            // Act
            val actual = repository.sendWithOwnerSignature(coldWallet, transaction)

            // Assert
            assertThat(actual).isEqualTo(
                CollectiblesAccountError.Vault(GachaVaultError.TransactionTooLarge).left(),
            )
            coVerify(exactly = 0) {
                networkProvider.create(any())
                walletManager.send(any<TransactionData>(), any(), any())
            }
        }

        @Test
        fun `GIVEN malformed base64 WHEN sendWithOwnerSignature THEN nothing is sent`() = runTest {
            // Act
            val actual = repository.sendWithOwnerSignature(userWallet, "not base64!")

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(CollectiblesAccountError.Vault::class.java)
            coVerify(exactly = 0) { walletManager.send(any<TransactionData>(), any(), any()) }
        }

        @Test
        fun `GIVEN the user cancels the card session WHEN sent THEN cancelled is returned`() = runTest {
            // Arrange
            coEvery { walletManager.send(any<TransactionData>(), any(), any()) } returns
                Result.Failure(BlockchainSdkError.WrappedTangemError(TangemSdkError.UserCancelled()))

            // Act
            val actual = repository.sendWithOwnerSignature(coldWallet, solanaTransaction(10).toBase64())

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Cancelled.left())
        }

        @Test
        fun `GIVEN no wallet manager WHEN sendWithOwnerSignature THEN nothing is sent`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) } returns null

            // Act
            val actual = repository.sendWithOwnerSignature(userWallet, solanaTransaction(10).toBase64())

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) { walletManager.send(any<TransactionData>(), any(), any()) }
        }

        /** A legacy transaction with one empty signature slot and one instruction carrying [instructionDataSize] bytes. */
        private fun solanaTransaction(instructionDataSize: Int): ByteArray {
            val message = byteArrayOf(1, 0, 1) +
                shortVec(2) + ByteArray(PUBLIC_KEY_SIZE) { 1 } + ByteArray(PUBLIC_KEY_SIZE) { 2 } +
                ByteArray(PUBLIC_KEY_SIZE) { 3 } +
                shortVec(1) + byteArrayOf(1) + shortVec(1) + byteArrayOf(0) +
                shortVec(instructionDataSize) + ByteArray(instructionDataSize)

            return shortVec(1) + ByteArray(SIGNATURE_SIZE) + message
        }

        private fun shortVec(value: Int): ByteArray {
            val bytes = mutableListOf<Byte>()
            var rest = value
            do {
                var byte = rest and 0x7F
                rest = rest shr 7
                if (rest != 0) byte = byte or 0x80
                bytes += byte.toByte()
            } while (rest != 0)
            return bytes.toByteArray()
        }

        private fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)
    }

    private companion object {

        val WALLET_ID = UserWalletId("011977")

        const val VAULT_ADDRESS = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
        const val VAULT_USDC_ADDRESS = "9vJc1bnfHRLbGeDdQCrmJFAsEtR9SEbNJVi8QeHfAo8f"
        const val MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
        val VAULT_TOKEN = MockCollectibles.usdc(mint = MINT)

        val SESSION_KEY = SessionKeyPublicKey("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        val LIMITS = GachaVaultLimits.Default

        const val AMOUNT = 50_000_000L
        const val FEE_AMOUNT = 1_000_000L
        const val MEMO = "cc-uuid"
        const val TX_HASH = "hash"
        const val OWNER = "owner"
        const val RECIPIENT = "Hx6LbkMHe69DYawhPyVNs8Apa6tyfogfzQV6a7XkwBUU"

        const val CARD_ID = "AC01000000041225"
        const val PUBLIC_KEY_SIZE = 32
        const val SIGNATURE_SIZE = 64

        /** Pushes the message past the 930 bytes a card can sign. */
        const val LARGE_INSTRUCTION_DATA_SIZE = 900

    }
}