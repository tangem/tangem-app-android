package com.tangem.data.collectibles.vault.transaction

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessInstructionRule
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSignedTransaction
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessTransactionSigner
import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.run
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.collectibles.vault.CollectiblesOwnerNetworkProvider
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.delegate.SessionKeyPublicKey
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.sdk.api.TangemSdkManager
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class GaslessGachaVaultTransactionSenderTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val networkProvider: CollectiblesOwnerNetworkProvider = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val tangemSdkManager: TangemSdkManager = mockk()
    private val hotWalletSignerFactory: TangemHotWalletSigner.Factory = mockk()
    private val gaslessRepository: SolanaGaslessTransactionRepository = mockk()

    private val walletManager: WalletManager = mockk(moreInterfaces = arrayOf(SolanaGaslessTransactionSigner::class))
    private val gaslessSigner = walletManager as SolanaGaslessTransactionSigner
    private val signer: TangemHotWalletSigner = mockk()
    private val network: Network = mockk(relaxed = true)
    private val transaction: TransactionData.Compiled = mockk()
    private val signed = SolanaGaslessSignedTransaction(transaction = SIGNED_BYTES, lastValidBlockHeight = 42)

    private val userWallet: UserWallet.Hot = mockk { every { walletId } returns WALLET_ID }

    private val sender = GaslessGachaVaultTransactionSender(
        walletManagersFacade = walletManagersFacade,
        networkProvider = networkProvider,
        derivationsRepository = derivationsRepository,
        tangemSdkManager = tangemSdkManager,
        signers = GachaVaultSigners(cardSdkConfigRepository, hotWalletSignerFactory),
        gaslessRepository = gaslessRepository,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(walletManagersFacade, networkProvider, walletManager, gaslessRepository, tangemSdkManager)
        every { hotWalletSignerFactory.create(userWallet) } returns signer
        coEvery { gaslessRepository.getFeePayer() } returns FEE_PAYER.right()
        coEvery { gaslessRepository.submit(WALLET_ID, SIGNED_BYTES, 42) } returns TX_HASH.right()
        coEvery { networkProvider.create(any()) } returns network.right()
        coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) } returns walletManager
        coEvery { walletManager.getCollectiblesVault() } returns Result.Success(null)
        coEvery { walletManager.buildInitCollectiblesVault(any(), any(), any()) } returns Result.Success(transaction)
        every { walletManager.getGaslessInstructionRules() } returns RULES
        coEvery { gaslessSigner.signGaslessTransaction(transaction, FEE_PAYER, any(), RULES) } returns
            Result.Success(signed)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitOrUpdateVault {

        @Test
        fun `GIVEN no vault WHEN initOrUpdate THEN it is signed with the vault rules and submitted`() = runTest {
            // Act
            val actual = sender.initOrUpdateVault(userWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerifyOrder {
                gaslessSigner.signGaslessTransaction(transaction, FEE_PAYER, signer, RULES)
                gaslessRepository.submit(WALLET_ID, SIGNED_BYTES, 42)
            }
            coVerify(exactly = 0) { walletManager.send(any<TransactionData>(), any(), any()) }
        }

        @Test
        fun `GIVEN the vault already points here WHEN initOrUpdate THEN nothing is signed`() = runTest {
            // Arrange
            coEvery { walletManager.getCollectiblesVault() } returns Result.Success(vault(SESSION_KEY.base58))

            // Act
            val actual = sender.initOrUpdateVault(userWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 0) {
                gaslessSigner.signGaslessTransaction(any(), any(), any(), any())
                gaslessRepository.submit(any(), any(), any())
            }
        }

        @Test
        fun `GIVEN the sponsor is unavailable WHEN initOrUpdate THEN the vault is never read`() = runTest {
            // Arrange
            coEvery { gaslessRepository.getFeePayer() } returns SolanaGaslessError.Disabled.left()

            // Act
            val actual = sender.initOrUpdateVault(userWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.SponsorUnavailable).left())
            coVerify(exactly = 0) { walletManager.getCollectiblesVault() }
        }

        @Test
        fun `GIVEN the submit may have landed WHEN initOrUpdate THEN the outcome is unknown`() = runTest {
            // Arrange
            coEvery { gaslessRepository.submit(any(), any(), any()) } returns
                SolanaGaslessError.MaybeSent(signature = TX_HASH).left()

            // Act
            val actual = sender.initOrUpdateVault(userWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.MaybeSent(txHash = TX_HASH).left())
        }
    }

    /** Nothing is broadcast from the card session, so it ends on the signature and the submit follows it. */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class DeriveOwnerAndSetUpVault {

        private val coldWallet: UserWallet.Cold = mockk {
            every { walletId } returns WALLET_ID
            every { scanResponse } returns mockk {
                every { productType } returns ProductType.Wallet2
                every { card } returns mockk {
                    every { cardId } returns CARD_ID
                    every { backupStatus } returns CardDTO.BackupStatus.NoBackup
                    every { wallets } returns listOf(
                        mockk {
                            every { curve } returns EllipticCurve.Ed25519Slip0010
                            every { publicKey } returns SEED_KEY
                        },
                    )
                }
            }
        }

        private val cardSigner: TransactionSigner = mockk()
        private val session: CardSession = mockk()

        /** Cancelled the way the SDK cancels it when the session stops. */
        private lateinit var sessionScope: CoroutineScope
        private var sessionResult: CompletionResult<*>? = null

        @BeforeEach
        fun resetSessionMocks() {
            mockkStatic(CURVES_CONFIG_FILE)
            mockkStatic(CARD_SESSION_RUNNABLE_FILE)

            sessionResult = null
            every { coldWallet.curvesConfig } returns mockk {
                every { primaryCurve(any()) } returns EllipticCurve.Ed25519Slip0010
            }
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), WALLET_ID, session) } returns cardSigner
            coEvery { derivationsRepository.storeDerivedKeys(any(), any()) } returns Unit
            coEvery { any<DeriveWalletPublicKeyTask>().run(session) } returns
                CompletionResult.Success(mockk<ExtendedPublicKey>())
        }

        @AfterEach
        fun unmockStatics() {
            unmockkStatic(CURVES_CONFIG_FILE)
            unmockkStatic(CARD_SESSION_RUNNABLE_FILE)
        }

        @Test
        fun `GIVEN the card signs WHEN deriveOwnerAndSetUpVault THEN it is submitted after the session ends`() =
            runTest {
                // Arrange
                stubCardSession()
                var isSessionActiveOnSubmit: Boolean? = null
                coEvery { gaslessRepository.submit(WALLET_ID, SIGNED_BYTES, 42) } coAnswers {
                    isSessionActiveOnSubmit = sessionScope.isActive
                    TX_HASH.right()
                }

                // Act
                val actual = sender.deriveOwnerAndSetUpVault(coldWallet, SESSION_KEY, LIMITS)

                // Assert
                assertThat(actual).isEqualTo(Unit.right())
                assertThat((sessionResult as? CompletionResult.Success)?.data).isSameInstanceAs(signed)
                assertThat(isSessionActiveOnSubmit).isFalse()
                coVerify(
                    exactly = 1,
                ) { gaslessSigner.signGaslessTransaction(transaction, FEE_PAYER, cardSigner, RULES) }
            }

        @Test
        fun `GIVEN the sponsor is unavailable WHEN deriveOwnerAndSetUpVault THEN no card session starts`() = runTest {
            // Arrange
            stubCardSession()
            coEvery { gaslessRepository.getFeePayer() } returns SolanaGaslessError.Unauthorized.left()

            // Act
            val actual = sender.deriveOwnerAndSetUpVault(coldWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.SponsorUnavailable).left())
            coVerify(exactly = 0) {
                tangemSdkManager.runTaskAsync(any<CardSessionRunnable<*>>(), any(), any(), any(), any(), any(), any())
            }
        }

        @Test
        fun `GIVEN the user cancels the signing WHEN deriveOwnerAndSetUpVault THEN nothing is submitted`() = runTest {
            // Arrange
            stubCardSession()
            coEvery { gaslessSigner.signGaslessTransaction(any(), any(), any(), any()) } returns
                Result.Failure(BlockchainSdkError.WrappedTangemError(TangemSdkError.UserCancelled()))

            // Act
            val actual = sender.deriveOwnerAndSetUpVault(coldWallet, SESSION_KEY, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Cancelled.left())
            assertThat(sessionResult).isInstanceOf(CompletionResult.Failure::class.java)
            coVerify(exactly = 0) { gaslessRepository.submit(any(), any(), any()) }
        }

        /** Emulates the SDK: cancels the session's scope when the runnable reports, as stopping a session does. */
        private fun TestScope.stubCardSession() {
            sessionScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            every { session.scope } returns sessionScope

            coEvery {
                tangemSdkManager.runTaskAsync(
                    runnable = any<CardSessionRunnable<SolanaGaslessSignedTransaction?>>(),
                    preflightReadFilter = any(),
                    cardId = any(),
                    initialMessage = any(),
                    accessCode = any(),
                    iconScanRes = any(),
                    registerColdWallet = any(),
                )
            } coAnswers {
                val runnable = firstArg<CardSessionRunnable<SolanaGaslessSignedTransaction?>>()

                suspendCancellableCoroutine { continuation ->
                    runnable.run(session) { result ->
                        sessionResult = result
                        sessionScope.cancel()
                        continuation.resume(result)
                    }
                }
            }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SendPurchasePack {

        private val sessionKeySigner: suspend (ByteArray) -> Result<ByteArray> = { Result.Success(it) }

        @BeforeEach
        fun stubPurchase() {
            coEvery {
                walletManager.signGaslessPurchasePack(
                    SESSION_KEY.base58, AMOUNT, FEE_AMOUNT, MEMO, FEE_PAYER, sessionKeySigner,
                )
            } returns Result.Success(signed)
        }

        @Test
        fun `GIVEN the sponsor takes it WHEN sendPurchasePack THEN its signature is returned`() = runTest {
            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual).isEqualTo(TX_HASH.right())
            coVerify(exactly = 0) { walletManager.sendPurchasePack(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN the submit may have landed WHEN sendPurchasePack THEN the outcome is unknown`() = runTest {
            // Arrange
            coEvery { gaslessRepository.submit(any(), any(), any()) } returns
                SolanaGaslessError.MaybeSent(signature = null).left()

            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.MaybeSent(txHash = null).left())
        }

        @Test
        fun `GIVEN the sponsor is rate limited WHEN sendPurchasePack THEN nothing is signed`() = runTest {
            // Arrange
            coEvery { gaslessRepository.getFeePayer() } returns
                SolanaGaslessError.RateLimited(isPerWallet = true).left()

            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.SponsorRateLimited).left())
            coVerify(exactly = 0) { walletManager.signGaslessPurchasePack(any(), any(), any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN the signing fails WHEN sendPurchasePack THEN nothing is submitted`() = runTest {
            // Arrange
            coEvery { walletManager.signGaslessPurchasePack(any(), any(), any(), any(), any(), any()) } returns
                Result.Failure(BlockchainSdkError.CustomError("session key failed"))

            // Act
            val actual = sendPurchasePack()

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) { gaslessRepository.submit(any(), any(), any()) }
        }

        private suspend fun sendPurchasePack() = sender.sendPurchasePack(
            userWallet = userWallet,
            sessionKey = SESSION_KEY,
            amount = AMOUNT,
            feeAmount = FEE_AMOUNT,
            memo = MEMO,
            signer = sessionKeySigner,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Withdraw {

        @BeforeEach
        fun stubWithdraw() {
            coEvery { walletManager.buildWithdrawCollectiblesVault(MINT, RECIPIENT, AMOUNT) } returns
                Result.Success(transaction)
        }

        @Test
        fun `GIVEN the sponsor takes it WHEN withdraw THEN it is signed with the vault rules and submitted`() =
            runTest {
                // Act
                val actual = sender.withdraw(userWallet, MINT, RECIPIENT, AMOUNT)

                // Assert
                assertThat(actual).isEqualTo(TX_HASH.right())
                coVerifyOrder {
                    gaslessSigner.signGaslessTransaction(transaction, FEE_PAYER, signer, RULES)
                    gaslessRepository.submit(WALLET_ID, SIGNED_BYTES, 42)
                }
                coVerify(exactly = 0) { walletManager.send(any<TransactionData>(), any(), any()) }
            }

        @Test
        fun `GIVEN the sponsor is unavailable WHEN withdraw THEN nothing is built`() = runTest {
            // Arrange
            coEvery { gaslessRepository.getFeePayer() } returns SolanaGaslessError.Disabled.left()

            // Act
            val actual = sender.withdraw(userWallet, MINT, RECIPIENT, AMOUNT)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Vault(GachaVaultError.SponsorUnavailable).left())
            coVerify(exactly = 0) { walletManager.buildWithdrawCollectiblesVault(any(), any(), any()) }
        }

        @Test
        fun `GIVEN the build fails WHEN withdraw THEN nothing is signed`() = runTest {
            // Arrange
            coEvery { walletManager.buildWithdrawCollectiblesVault(any(), any(), any()) } returns
                Result.Failure(BlockchainSdkError.CustomError("bad recipient"))

            // Act
            val actual = sender.withdraw(userWallet, MINT, RECIPIENT, AMOUNT)

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) {
                gaslessSigner.signGaslessTransaction(any(), any(), any(), any())
                gaslessRepository.submit(any(), any(), any())
            }
        }

        @Test
        fun `GIVEN the submit may have landed WHEN withdraw THEN the outcome is unknown`() = runTest {
            // Arrange
            coEvery { gaslessRepository.submit(any(), any(), any()) } returns
                SolanaGaslessError.MaybeSent(signature = TX_HASH).left()

            // Act
            val actual = sender.withdraw(userWallet, MINT, RECIPIENT, AMOUNT)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.MaybeSent(txHash = TX_HASH).left())
        }
    }

    private fun vault(sessionKey: String): CollectiblesVault = mockk {
        every { this@mockk.sessionKey } returns sessionKey
    }

    private companion object {

        val WALLET_ID = UserWalletId("011977")

        val SESSION_KEY = SessionKeyPublicKey("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        val LIMITS = GachaVaultLimits.Default
        val RULES = SolanaGaslessInstructionRule.ASSOCIATED_TOKEN_ACCOUNT

        const val FEE_PAYER = "6KYqD3JnqeqGVQfF2f5FrxKKNuKvsb1LS2TFrd25o6C7"
        val SIGNED_BYTES = byteArrayOf(1, 2, 3)

        const val AMOUNT = 50_000_000L
        const val FEE_AMOUNT = 1_000_000L
        const val MEMO = "cc-uuid"
        const val TX_HASH = "hash"
        const val MINT = "Gh9ZwEmdLJ8DscKNTkTqPbNwLNNBjuSzaG9Vp2KGtKJr"
        const val RECIPIENT = "Hx6LbkMHe69DYawhPyVNs8Apa6tyfogfzQV6a7XkwBUU"

        const val CARD_ID = "AC01000000041225"
        val SEED_KEY = byteArrayOf(2, 0xA.toByte(), 0xB.toByte())

        const val CURVES_CONFIG_FILE = "com.tangem.domain.wallets.config.CurvesConfigKt"
        const val CARD_SESSION_RUNNABLE_FILE = "com.tangem.common.core.CardSessionRunnableKt"
    }
}