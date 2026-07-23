package com.tangem.tap.domain.userWalletList.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.appsflyer.usecase.ClearAppsFlyerDeeplinkUseCase
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.common.wallets.UserWalletSelectedHandler
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.Provider
import com.tangem.utils.ProviderSuspend
import dagger.Lazy
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultUserWalletsListRepositoryTest {

    private val publicInformationRepository: UserWalletsPublicInformationRepository = mockk()
    private val sensitiveInformationRepository: UserWalletsSensitiveInformationRepository = mockk()
    private val selectedUserWalletRepository: SelectedUserWalletRepository = mockk(relaxed = true)
    private val passwordRequester: HotWalletPasswordRequester = mockk(relaxed = true)
    private val userWalletEncryptionKeysRepository: UserWalletEncryptionKeysRepository = mockk(relaxed = true)
    private val tangemSdkManager: TangemSdkManager = mockk(relaxed = true)
    private val appPreferencesStore: AppPreferencesStore = mockk(relaxed = true)
    private val hotWalletAccessCodeAttemptsRepository: HotWalletAccessCodeAttemptsRepository = mockk(relaxed = true)
    private val tangemHotSdk: TangemHotSdk = mockk(relaxed = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val hotWalletRepository: HotWalletRepository = mockk(relaxed = true)
    private val clearAppsFlyerDeeplinkUseCase: ClearAppsFlyerDeeplinkUseCase = mockk(relaxed = true)
    private val userWalletSelectedHandler: UserWalletSelectedHandler = mockk(relaxed = true)

    private val walletA = MockUserWalletFactory.create().copy(walletId = UserWalletId("0011"), name = "Wallet A")
    private val walletB = MockUserWalletFactory.create().copy(walletId = UserWalletId("0022"), name = "Wallet B")

    private lateinit var repository: DefaultUserWalletsListRepository

    @BeforeEach
    fun setup() {
        clearMocks(
            publicInformationRepository,
            sensitiveInformationRepository,
            selectedUserWalletRepository,
            userWalletEncryptionKeysRepository,
            trackingContextProxy,
            clearAppsFlyerDeeplinkUseCase,
            userWalletSelectedHandler,
        )

        coEvery { publicInformationRepository.delete(any()) } returns CompletionResult.Success(Unit)
        coEvery { sensitiveInformationRepository.delete(any()) } returns CompletionResult.Success(Unit)

        repository = DefaultUserWalletsListRepository(
            publicInformationRepository = publicInformationRepository,
            sensitiveInformationRepository = sensitiveInformationRepository,
            selectedUserWalletRepository = selectedUserWalletRepository,
            passwordRequester = passwordRequester,
            userWalletEncryptionKeysRepository = userWalletEncryptionKeysRepository,
            tangemSdkManagerProvider = Provider { tangemSdkManager },
            savePersistentInformation = ProviderSuspend { true },
            appPreferencesStore = appPreferencesStore,
            hotWalletAccessCodeAttemptsRepository = hotWalletAccessCodeAttemptsRepository,
            tangemHotSdk = tangemHotSdk,
            trackingContextProxy = trackingContextProxy,
            analyticsEventHandler = analyticsEventHandler,
            hotWalletRepository = hotWalletRepository,
            clearAppsFlyerDeeplinkUseCase = clearAppsFlyerDeeplinkUseCase,
            userWalletSelectedHandler = Lazy { userWalletSelectedHandler },
        )
    }

    @Test
    fun `GIVEN two wallets WHEN delete non-last wallet THEN remaining wallet identified and context not erased`() =
        runTest {
            // Arrange
            repository.userWallets.value = listOf(walletA, walletB)
            repository.selectedUserWallet.value = walletA

            // Act
            val result = repository.delete(listOf(walletA.walletId))

            // Assert
            assertThat(result.isRight()).isTrue()
            assertThat(repository.userWallets.value).containsExactly(walletB)
            assertThat(repository.selectedUserWallet.value).isEqualTo(walletB)
            coVerify(exactly = 1) { userWalletSelectedHandler.invoke(walletB) }
            verify(exactly = 0) { trackingContextProxy.eraseContext() }
        }

    @Test
    fun `GIVEN single wallet WHEN delete it THEN context erased after local state teardown`() = runTest {
        // Arrange
        repository.userWallets.value = listOf(walletA)
        repository.selectedUserWallet.value = walletA

        var walletsOnErase: List<UserWallet>? = listOf(walletA)
        var selectedOnErase: UserWallet? = walletA
        every { trackingContextProxy.eraseContext() } answers {
            walletsOnErase = repository.userWallets.value
            selectedOnErase = repository.selectedUserWallet.value
        }

        // Act
        val result = repository.delete(listOf(walletA.walletId))

        // Assert
        assertThat(result.isRight()).isTrue()
        verify(exactly = 1) { trackingContextProxy.eraseContext() }
        assertThat(walletsOnErase).isEmpty()
        assertThat(selectedOnErase).isNull()
        coVerify(exactly = 0) { userWalletSelectedHandler.invoke(any()) }
    }

    @Test
    fun `GIVEN single wallet WHEN delete fails THEN context not erased`() = runTest {
        // Arrange
        repository.userWallets.value = listOf(walletA)
        repository.selectedUserWallet.value = walletA
        coEvery { publicInformationRepository.delete(any()) } returns
            CompletionResult.Failure(mockk<TangemError>(relaxed = true))

        // Act
        val result = repository.delete(listOf(walletA.walletId))

        // Assert
        assertThat(result.isLeft()).isTrue()
        verify(exactly = 0) { trackingContextProxy.eraseContext() }
    }

    @Test
    fun `GIVEN stale backup status WHEN duplicate save rejected THEN stored card state refreshed`() = runTest {
        // Arrange
        val storedWallet = MockUserWalletFactory.create(staleScanResponse)
        val freshWallet = MockUserWalletFactory.create(freshScanResponse)
        repository.userWallets.value = listOf(storedWallet)
        coEvery { publicInformationRepository.save(any(), any()) } returns CompletionResult.Success(Unit)

        // Act
        val result = repository.saveWithoutLock(freshWallet, canOverride = false)

        // Assert
        assertThat(result.isLeft()).isTrue()
        val updatedWallet = repository.userWallets.value?.single() as UserWallet.Cold
        assertThat(updatedWallet.scanResponse.card.backupStatus)
            .isEqualTo(CardDTO.BackupStatus.Active(cardCount = 1))
        assertThat(updatedWallet.scanResponse.card.isAccessCodeSet).isTrue()
        coVerify(exactly = 1) { publicInformationRepository.save(any(), true) }
    }

    @Test
    fun `GIVEN locked wallet with stale backup status WHEN unlock with scanned card THEN stored card state refreshed`() =
        runTest {
            // Arrange
            val storedWallet = MockUserWalletFactory.create(staleScanResponse).let { wallet ->
                wallet.copy(
                    scanResponse = wallet.scanResponse.copy(
                        card = wallet.scanResponse.card.copy(wallets = emptyList()),
                    ),
                )
            }
            repository.userWallets.value = listOf(storedWallet)
            coEvery { publicInformationRepository.save(any(), any()) } returns CompletionResult.Success(Unit)
            coEvery { sensitiveInformationRepository.getAll(any()) } returns CompletionResult.Success(emptyMap())

            // Act
            val result = repository.unlock(
                userWalletId = storedWallet.walletId,
                unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(
                    scanResponse = freshScanResponse,
                    source = AnalyticsParam.ScreensSources.SignIn,
                ),
            )

            // Assert
            assertThat(result.isRight()).isTrue()
            val updatedWallet = repository.userWallets.value?.single() as UserWallet.Cold
            assertThat(updatedWallet.scanResponse.card.backupStatus)
                .isEqualTo(CardDTO.BackupStatus.Active(cardCount = 1))
            assertThat(updatedWallet.scanResponse.card.isAccessCodeSet).isTrue()
            coVerify(exactly = 1) { publicInformationRepository.save(any(), true) }
        }

    @Test
    fun `GIVEN active card of backup set scanned WHEN duplicate save rejected THEN stored card state refreshed`() =
        runTest {
            // Arrange
            val storedWallet = MockUserWalletFactory.create(staleScanResponse)
            val otherCardScanResponse = freshScanResponse.copy(
                card = freshScanResponse.card.copy(cardId = "OTHER-CARD"),
            )
            val freshWallet = MockUserWalletFactory.create(otherCardScanResponse)
            repository.userWallets.value = listOf(storedWallet)
            coEvery { publicInformationRepository.save(any(), any()) } returns CompletionResult.Success(Unit)

            // Act
            val result = repository.saveWithoutLock(freshWallet, canOverride = false)

            // Assert
            assertThat(result.isLeft()).isTrue()
            val updatedWallet = repository.userWallets.value?.single() as UserWallet.Cold
            assertThat(updatedWallet.scanResponse.card.cardId).isEqualTo(storedWallet.scanResponse.card.cardId)
            assertThat(updatedWallet.scanResponse.card.backupStatus)
                .isEqualTo(CardDTO.BackupStatus.Active(cardCount = 1))
            assertThat(updatedWallet.scanResponse.card.isAccessCodeSet).isTrue()
            coVerify(exactly = 1) { publicInformationRepository.save(any(), true) }
        }

    @Test
    fun `GIVEN no backup card of same wallet scanned WHEN duplicate save rejected THEN stored status downgraded`() =
        runTest {
            // Arrange
            val storedWallet = MockUserWalletFactory.create(freshScanResponse)
            val newCardScanResponse = staleScanResponse.copy(
                card = staleScanResponse.card.copy(cardId = "SAME-SEED-NEW-CARD"),
            )
            val freshWallet = MockUserWalletFactory.create(newCardScanResponse)
            repository.userWallets.value = listOf(storedWallet)
            coEvery { publicInformationRepository.save(any(), any()) } returns CompletionResult.Success(Unit)

            // Act
            val result = repository.saveWithoutLock(freshWallet, canOverride = false)

            // Assert
            assertThat(result.isLeft()).isTrue()
            val updatedWallet = repository.userWallets.value?.single() as UserWallet.Cold
            assertThat(updatedWallet.scanResponse.card.cardId).isEqualTo(storedWallet.scanResponse.card.cardId)
            assertThat(updatedWallet.scanResponse.card.backupStatus).isEqualTo(CardDTO.BackupStatus.NoBackup)
            assertThat(updatedWallet.scanResponse.card.isAccessCodeSet).isFalse()
            coVerify(exactly = 1) { publicInformationRepository.save(any(), true) }
        }

    @Test
    fun `GIVEN stored card linked status WHEN duplicate save with another card rejected THEN status preserved`() =
        runTest {
            // Arrange
            val cardLinkedScanResponse = staleScanResponse.copy(
                card = staleScanResponse.card.copy(backupStatus = CardDTO.BackupStatus.CardLinked(cardCount = 1)),
            )
            val storedWallet = MockUserWalletFactory.create(cardLinkedScanResponse)
            val otherCardScanResponse = freshScanResponse.copy(
                card = freshScanResponse.card.copy(cardId = "OTHER-CARD"),
            )
            val freshWallet = MockUserWalletFactory.create(otherCardScanResponse)
            repository.userWallets.value = listOf(storedWallet)

            // Act
            val result = repository.saveWithoutLock(freshWallet, canOverride = false)

            // Assert
            assertThat(result.isLeft()).isTrue()
            assertThat(repository.userWallets.value).containsExactly(storedWallet)
            coVerify(exactly = 0) { publicInformationRepository.save(any(), any()) }
        }

    @Test
    fun `GIVEN stored card state is actual WHEN duplicate save rejected THEN nothing persisted`() = runTest {
        // Arrange
        val storedWallet = MockUserWalletFactory.create(freshScanResponse)
        val freshWallet = MockUserWalletFactory.create(freshScanResponse)
        repository.userWallets.value = listOf(storedWallet)

        // Act
        val result = repository.saveWithoutLock(freshWallet, canOverride = false)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(repository.userWallets.value).containsExactly(storedWallet)
        coVerify(exactly = 0) { publicInformationRepository.save(any(), any()) }
    }

    private companion object {

        val staleScanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(maxWalletCount = 2),
            derivedKeys = emptyMap(),
        ).let { scanResponse ->
            scanResponse.copy(card = scanResponse.card.copy(backupStatus = CardDTO.BackupStatus.NoBackup))
        }

        val freshScanResponse = staleScanResponse.copy(
            card = staleScanResponse.card.copy(
                backupStatus = CardDTO.BackupStatus.Active(cardCount = 1),
                isAccessCodeSet = true,
            ),
        )
    }
}