package com.tangem.tap.domain.userWalletList.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.common.wallets.UserWalletSelectedHandler
import com.tangem.domain.hotwallet.repository.HotWalletRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.hot.HotWalletAccessCodeAttemptsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.feature.referral.domain.MobileWalletPromoRepository
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
    private val mobileWalletPromoRepository: MobileWalletPromoRepository = mockk(relaxed = true)
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
            mobileWalletPromoRepository,
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
            mobileWalletPromoRepository = mobileWalletPromoRepository,
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
}