package com.tangem.features.hotwallet.forgetwallet

import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.assetsdiscovery.usecase.StartAssetsDiscoveryUseCase
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.cloudbackup.usecase.DeleteCloudBackupWithRetryUseCase
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.common.wallets.error.DeleteWalletError
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.features.hotwallet.ForgetWalletComponent
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ForgetWalletModelTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val deleteWalletUseCase: DeleteWalletUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val startAssetsDiscoveryUseCase: StartAssetsDiscoveryUseCase = mockk(relaxed = true)
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()
    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase = mockk(relaxed = true)
    private val deleteCloudBackupWithRetryUseCase: DeleteCloudBackupWithRetryUseCase = mockk()
    private val paramsContainer: ParamsContainer = mockk()

    private val walletId = UserWalletId("011")
    private val fileId = "file-1"

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            deleteWalletUseCase,
            uiMessageSender,
            cloudBackupRepository,
            setCloudBackupStateUseCase,
            deleteCloudBackupWithRetryUseCase,
            hotWalletFeatureToggles,
        )
        coEvery { deleteWalletUseCase(walletId) } returns true.right()
    }

    @Test
    fun `GIVEN deleteCloudBackup AND matching backup WHEN forget confirmed THEN backup deleted AND wallet forgotten`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups() } returns listOf(backup(walletId = "011")).right()
            coEvery { deleteCloudBackupWithRetryUseCase(fileId) } returns Unit.right()

            // Act
            confirmForget(createModel(this, deleteCloudBackup = true))

            // Assert
            coVerify(exactly = 1) { deleteWalletUseCase(walletId) }
            coVerify(exactly = 1) { cloudBackupRepository.findBackups() }
            coVerify(exactly = 1) { deleteCloudBackupWithRetryUseCase(fileId) }
            coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = false) }
            verify(exactly = 1) {
                analyticsEventHandler.send(ofType<WalletSettingsAnalyticEvents.WalletForgotten>())
            }
        }

    @Test
    fun `GIVEN deleteCloudBackup AND deletion fails WHEN forget confirmed THEN state kept AND wallet forgotten`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups() } returns listOf(backup(walletId = "011")).right()
            coEvery { deleteCloudBackupWithRetryUseCase(fileId) } returns CloudBackupError.NetworkError.left()

            // Act
            confirmForget(createModel(this, deleteCloudBackup = true))

            // Assert
            coVerify(exactly = 1) { deleteCloudBackupWithRetryUseCase(fileId) }
            // the file is still in the cloud, so the wallet must not be recorded as not backed up
            coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), any()) }
            coVerify(exactly = 1) { deleteWalletUseCase(walletId) }
        }

    @Test
    fun `GIVEN deleteCloudBackup AND lookup fails WHEN forget confirmed THEN state kept AND wallet forgotten`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups() } returns CloudBackupError.NetworkError.left()

            // Act
            confirmForget(createModel(this, deleteCloudBackup = true))

            // Assert
            coVerify(exactly = 0) { deleteCloudBackupWithRetryUseCase(any()) }
            coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), any()) }
            coVerify(exactly = 1) { deleteWalletUseCase(walletId) }
        }

    @Test
    fun `GIVEN deleteCloudBackup AND no backup in cloud WHEN forget confirmed THEN stale state cleared`() = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { cloudBackupRepository.findBackups() } returns listOf(backup(walletId = "022")).right()

        // Act
        confirmForget(createModel(this, deleteCloudBackup = true))

        // Assert
        coVerify(exactly = 0) { deleteCloudBackupWithRetryUseCase(any()) }
        coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = false) }
        coVerify(exactly = 1) { deleteWalletUseCase(walletId) }
    }

    @Test
    fun `GIVEN deleteCloudBackup AND wallet deletion fails WHEN forget confirmed THEN backup untouched`() = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { deleteWalletUseCase(walletId) } returns DeleteWalletError.UnableToDelete.left()

        // Act
        confirmForget(createModel(this, deleteCloudBackup = true))

        // Assert the wallet stays on the device, so its backup must stay in the cloud
        coVerify(exactly = 0) { cloudBackupRepository.findBackups() }
        coVerify(exactly = 0) { deleteCloudBackupWithRetryUseCase(any()) }
        coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), any()) }
        verify(exactly = 0) {
            analyticsEventHandler.send(ofType<WalletSettingsAnalyticEvents.WalletForgotten>())
        }
    }

    @Test
    fun `GIVEN no deleteCloudBackup WHEN forget confirmed THEN backup untouched AND wallet forgotten`() = runTest {
        // Act
        confirmForget(createModel(this, deleteCloudBackup = false))

        // Assert
        coVerify(exactly = 0) { cloudBackupRepository.findBackups() }
        coVerify(exactly = 0) { deleteCloudBackupWithRetryUseCase(any()) }
        coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), any()) }
        coVerify(exactly = 1) { deleteWalletUseCase(walletId) }
    }

    @Test
    fun `GIVEN deleteCloudBackup AND toggle off WHEN forget confirmed THEN backup untouched AND wallet forgotten`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns false

            // Act
            confirmForget(createModel(this, deleteCloudBackup = true))

            // Assert
            coVerify(exactly = 0) { cloudBackupRepository.findBackups() }
            coVerify(exactly = 0) { deleteCloudBackupWithRetryUseCase(any()) }
            coVerify(exactly = 1) { deleteWalletUseCase(walletId) }
        }

    private fun TestScope.confirmForget(model: ForgetWalletModel) {
        val sentMessages = mutableListOf<UiMessage>()
        every { uiMessageSender.send(capture(sentMessages)) } just Runs

        advanceUntilIdle()
        model.uiState.value.onForgetWalletClick()

        val dialog = sentMessages.filterIsInstance<DialogMessage>().last()
        dialog.firstAction.onClick()
        advanceUntilIdle()
    }

    private fun backup(fileId: String = this.fileId, walletId: String? = "011") = CloudBackupInfo(
        fileId = fileId,
        walletName = "wallet",
        createdAtMillis = 0L,
        walletId = walletId,
    )

    private fun createModel(testScope: TestScope, deleteCloudBackup: Boolean): ForgetWalletModel {
        every { paramsContainer.require<ForgetWalletComponent.Params>() } returns
            ForgetWalletComponent.Params(userWalletId = walletId, shouldDeleteCloudBackup = deleteCloudBackup)

        val testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        return ForgetWalletModel(
            paramsContainer = paramsContainer,
            dispatchers = TestingCoroutineDispatcherProvider(
                main = testDispatcher,
                mainImmediate = testDispatcher,
                io = testDispatcher,
                default = testDispatcher,
                single = testDispatcher,
            ),
            appScope = testScope.asAppScope(),
            router = router,
            analyticsEventHandler = analyticsEventHandler,
            deleteWalletUseCase = deleteWalletUseCase,
            uiMessageSender = uiMessageSender,
            startAssetsDiscoveryUseCase = startAssetsDiscoveryUseCase,
            hotWalletFeatureToggles = hotWalletFeatureToggles,
            cloudBackupRepository = cloudBackupRepository,
            setCloudBackupStateUseCase = setCloudBackupStateUseCase,
            deleteCloudBackupWithRetryUseCase = deleteCloudBackupWithRetryUseCase,
        )
    }

    // the test scope itself, not backgroundScope: the model launches the cloud backup deletion from inside
    // the forget coroutine, and background work queued mid-advance is not run by advanceUntilIdle()
    private fun TestScope.asAppScope() = object : AppCoroutineScope {
        override val coroutineContext = this@asAppScope.coroutineContext
    }
}