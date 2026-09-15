package com.tangem.data.collectibles.vault

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.run
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class DefaultGachaVaultRepositoryTest {

    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val networkProvider: CollectiblesOwnerNetworkProvider = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val tangemSdkManager: TangemSdkManager = mockk()
    private val hotWalletSignerFactory: TangemHotWalletSigner.Factory = mockk()

    private val walletManager: WalletManager = mockk()
    private val signer: TangemHotWalletSigner = mockk()
    private val network: Network = mockk(relaxed = true)
    private val transaction: TransactionData.Compiled = mockk()

    private val userWallet: UserWallet.Hot = mockk { every { walletId } returns WALLET_ID }

    private val repository = DefaultGachaVaultRepository(
        walletManagersFacade = walletManagersFacade,
        networkProvider = networkProvider,
        ownerDeriver = ownerDeriver,
        derivationsRepository = derivationsRepository,
        cardSdkConfigRepository = cardSdkConfigRepository,
        tangemSdkManager = tangemSdkManager,
        hotWalletSignerFactory = hotWalletSignerFactory,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    /**
     * The one place the init-or-update rule lives: the one-tap session, the mobile wallet and a card that already
     * holds the owner key all come through here.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class InitOrUpdateVault {

        @BeforeEach
        fun resetMocks() {
            clearMocks(walletManagersFacade, networkProvider, hotWalletSignerFactory, walletManager)
            every { hotWalletSignerFactory.create(userWallet) } returns signer
            coEvery { networkProvider.create(userWallet) } returns network.right()
            coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) } returns walletManager
            coEvery { walletManager.buildInitCollectiblesVault(any(), any(), any()) } returns
                Result.Success(transaction)
            coEvery { walletManager.buildUpdateCollectiblesVault(any(), any(), any()) } returns
                Result.Success(transaction)
            coEvery { walletManager.send(any<TransactionData>(), signer, any()) } returns Result.Success(mockk())
        }

        @Test
        fun `GIVEN no vault WHEN initOrUpdate THEN it is created with the limits`() = runTest {
            // Arrange
            stubVault(vault = null)

            // Act
            val actual = repository.initOrUpdateVault(userWallet, DELEGATE, GachaVaultLimits.Default)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) {
                walletManager.buildInitCollectiblesVault(
                    hotDelegate = DELEGATE.base58,
                    perSpinCap = GachaVaultLimits.Default.perSpinCap,
                    dailyCap = GachaVaultLimits.Default.dailyCap,
                )
                walletManager.send(transaction, signer, any())
            }
            coVerify(exactly = 0) { walletManager.buildUpdateCollectiblesVault(any(), any(), any()) }
        }

        @Test
        fun `GIVEN a vault of another device WHEN initOrUpdate THEN the delegate is re-pointed`() = runTest {
            // Arrange
            stubVault(vault = vault(hotDelegate = OTHER_DELEGATE.base58))

            // Act
            val actual = repository.initOrUpdateVault(userWallet, DELEGATE, GachaVaultLimits.Default)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) {
                walletManager.buildUpdateCollectiblesVault(
                    hotDelegate = DELEGATE.base58,
                    perSpinCap = null,
                    dailyCap = null,
                )
                walletManager.send(transaction, signer, any())
            }
            coVerify(exactly = 0) { walletManager.buildInitCollectiblesVault(any(), any(), any()) }
        }

        @Test
        fun `GIVEN the vault already points here WHEN initOrUpdate THEN nothing is signed`() = runTest {
            // Arrange
            stubVault(vault = vault(hotDelegate = DELEGATE.base58))

            // Act
            val actual = repository.initOrUpdateVault(userWallet, DELEGATE, GachaVaultLimits.Default)

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 0) {
                walletManager.buildInitCollectiblesVault(any(), any(), any())
                walletManager.buildUpdateCollectiblesVault(any(), any(), any())
                walletManager.send(any<TransactionData>(), any(), any())
            }
        }

        @Test
        fun `GIVEN the read fails WHEN initOrUpdate THEN nothing is signed`() = runTest {
            // Arrange
            coEvery { walletManager.getCollectiblesVault() } returns
                Result.Failure(BlockchainSdkError.CustomError("rpc is down"))

            // Act
            val actual = repository.initOrUpdateVault(userWallet, DELEGATE, GachaVaultLimits.Default)

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) {
                walletManager.buildInitCollectiblesVault(any(), any(), any())
                walletManager.buildUpdateCollectiblesVault(any(), any(), any())
            }
        }

        @Test
        fun `GIVEN no wallet manager WHEN initOrUpdate THEN the vault is never read`() = runTest {
            // Arrange
            coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) } returns null

            // Act
            val actual = repository.initOrUpdateVault(userWallet, DELEGATE, GachaVaultLimits.Default)

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) { walletManager.getCollectiblesVault() }
        }

        @Test
        fun `GIVEN the owner key is not derived WHEN initOrUpdate THEN no manager is built`() = runTest {
            // Arrange
            coEvery { networkProvider.create(userWallet) } returns GachaVaultError.WalletUnavailable.left()

            // Act
            val actual = repository.initOrUpdateVault(userWallet, DELEGATE, GachaVaultLimits.Default)

            // Assert
            assertThat(actual.isLeft()).isTrue()
            coVerify(exactly = 0) { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) }
        }

        private fun stubVault(vault: CollectiblesVault?) {
            coEvery { walletManager.getCollectiblesVault() } returns Result.Success(vault)
        }

        private fun vault(hotDelegate: String): CollectiblesVault = mockk {
            every { this@mockk.hotDelegate } returns hotDelegate
        }
    }

    /**
     * The one-tap session signs on the card and broadcasts afterwards, and the NFC sheet lives exactly as long as
     * the session's runnable — so the runnable has to end on the signature, not on the broadcast.
     */
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
        private lateinit var broadcast: CompletableDeferred<Unit>
        private var sessionResult: CompletionResult<Unit>? = null

        @BeforeEach
        fun resetMocks() {
            clearMocks(walletManagersFacade, networkProvider, walletManager, tangemSdkManager, cardSigner)
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

            coEvery { networkProvider.create(coldWallet) } returns network.right()
            coEvery { walletManagersFacade.getOrCreateWalletManager(WALLET_ID, network) } returns walletManager
            coEvery { walletManager.getCollectiblesVault() } returns Result.Success(null)
            coEvery { walletManager.buildInitCollectiblesVault(any(), any(), any()) } returns
                Result.Success(transaction)
        }

        @AfterEach
        fun unmockStatics() {
            unmockkStatic(CURVES_CONFIG_FILE)
            unmockkStatic(CARD_SESSION_RUNNABLE_FILE)
        }

        @Test
        fun `GIVEN the broadcast is still running WHEN deriveOwnerAndSetUpVault THEN the session is already over`() =
            runTest {
                // Arrange
                stubCardSession()
                stubSend()

                // Act
                val setUp = async { repository.deriveOwnerAndSetUpVault(coldWallet, DELEGATE, LIMITS) }
                advanceUntilIdle()

                // Assert
                assertThat(sessionScope.isActive).isFalse()
                assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
                assertThat(setUp.isCompleted).isFalse()

                broadcast.complete(Unit)
                advanceUntilIdle()
                assertThat(setUp.await()).isEqualTo(Unit.right())
            }

        @Test
        fun `GIVEN the session is over WHEN the broadcast finishes THEN its result is returned`() = runTest {
            // Arrange
            stubCardSession()
            stubSend()

            // Act
            val setUp = async { repository.deriveOwnerAndSetUpVault(coldWallet, DELEGATE, LIMITS) }
            advanceUntilIdle()
            broadcast.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(setUp.await()).isEqualTo(Unit.right())
            coVerify(exactly = 1) { walletManager.send(transaction, cardSigner, any()) }
        }

        /** The broadcast is past the card, so its failure is ours to show rather than the session's. */
        @Test
        fun `GIVEN the broadcast fails WHEN deriveOwnerAndSetUpVault THEN the failure is returned`() = runTest {
            // Arrange
            stubCardSession()
            stubSend(result = Result.Failure(BlockchainSdkError.FailedToSendException))

            // Act
            val setUp = async { repository.deriveOwnerAndSetUpVault(coldWallet, DELEGATE, LIMITS) }
            advanceUntilIdle()
            broadcast.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(setUp.await().isLeft()).isTrue()
            assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
        }

        /** Nothing signs here, so the sheet is still up — the failure belongs to the session. */
        @Test
        fun `GIVEN the signing fails WHEN deriveOwnerAndSetUpVault THEN the session reports the failure`() = runTest {
            // Arrange
            stubCardSession()
            coEvery { walletManager.send(any<TransactionData>(), any(), any()) } returns
                Result.Failure(BlockchainSdkError.WrappedTangemError(TangemSdkError.UserCancelled()))

            // Act
            val actual = repository.deriveOwnerAndSetUpVault(coldWallet, DELEGATE, LIMITS)

            // Assert
            assertThat(actual).isEqualTo(CollectiblesAccountError.Cancelled.left())
            assertThat(sessionResult).isInstanceOf(CompletionResult.Failure::class.java)
        }

        @Test
        fun `GIVEN the vault cannot be read WHEN deriveOwnerAndSetUpVault THEN the session still ends`() = runTest {
            // Arrange
            stubCardSession()
            coEvery { walletManager.getCollectiblesVault() } returns
                Result.Failure(BlockchainSdkError.CustomError("rpc is down"))

            // Act
            val actual = repository.deriveOwnerAndSetUpVault(coldWallet, DELEGATE, LIMITS)

            // Assert
            assertThat(actual.isLeft()).isTrue()
            assertThat(sessionScope.isActive).isFalse()
            coVerify(exactly = 0) { walletManager.send(any<TransactionData>(), any(), any()) }
        }

        /** Emulates the SDK: cancels the session's scope when the runnable reports, as stopping a session does. */
        private fun TestScope.stubCardSession() {
            sessionScope = CoroutineScope(StandardTestDispatcher(testScheduler))
            broadcast = CompletableDeferred()
            every { session.scope } returns sessionScope

            coEvery {
                tangemSdkManager.runTaskAsync(
                    runnable = any<CardSessionRunnable<Unit>>(),
                    preflightReadFilter = any(),
                    cardId = any(),
                    initialMessage = any(),
                    accessCode = any(),
                    iconScanRes = any(),
                    registerColdWallet = any(),
                )
            } coAnswers {
                val runnable = firstArg<CardSessionRunnable<Unit>>()

                suspendCancellableCoroutine { continuation ->
                    runnable.run(session) { result ->
                        sessionResult = result
                        sessionScope.cancel()
                        continuation.resume(result)
                    }
                }
            }
        }

        /** The manager reports the signature, then broadcasts — which is where [broadcast] holds it. */
        private fun stubSend(result: Result<TransactionSendResult> = Result.Success(mockk())) {
            coEvery { walletManager.send(any<TransactionData>(), any(), any()) } coAnswers {
                thirdArg<() -> Unit>().invoke()
                broadcast.await()
                result
            }
        }
    }

    private companion object {

        val WALLET_ID = UserWalletId("011977")

        val DELEGATE = HotDelegatePublicKey("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        val OTHER_DELEGATE = HotDelegatePublicKey("A4ahkivAG4NoZAE8Sy4qv8nn2DU9yoXRQcttuCeGtTJv")
        val LIMITS = GachaVaultLimits.Default

        const val CARD_ID = "AC01000000041225"
        val SEED_KEY = byteArrayOf(2, 0xA.toByte(), 0xB.toByte())

        const val CURVES_CONFIG_FILE = "com.tangem.domain.wallets.config.CurvesConfigKt"
        const val CARD_SESSION_RUNNABLE_FILE = "com.tangem.common.core.CardSessionRunnableKt"
    }
}