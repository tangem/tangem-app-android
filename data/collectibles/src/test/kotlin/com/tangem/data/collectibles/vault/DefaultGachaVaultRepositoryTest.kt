package com.tangem.data.collectibles.vault

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.collectibles.models.CollectiblesVault
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.Result
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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
            coEvery { walletManager.send(any<TransactionData>(), signer) } returns Result.Success(mockk())
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
                walletManager.send(transaction, signer)
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
                walletManager.send(transaction, signer)
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
                walletManager.send(any<TransactionData>(), any())
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

    private companion object {

        val WALLET_ID = UserWalletId("011977")

        val DELEGATE = HotDelegatePublicKey("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
        val OTHER_DELEGATE = HotDelegatePublicKey("A4ahkivAG4NoZAE8Sy4qv8nn2DU9yoXRQcttuCeGtTJv")
    }
}