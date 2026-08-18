package com.tangem.features.hotwallet.walletbackup.model

import android.text.format.DateFormat
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.BottomSheetMessage
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.domain.cloudbackup.models.CloudBackupInfo
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.cloudbackup.usecase.SetCloudBackupStateUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import dagger.Lazy
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class WalletBackupModelTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase = mockk()
    private val router: Router = mockk(relaxUnitFun = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val setCloudBackupStateUseCase: SetCloudBackupStateUseCase = mockk(relaxed = true)
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase = mockk(relaxed = true)
    private val hotWalletFeatureToggles: HotWalletFeatureToggles = mockk()
    private val paramsContainer: ParamsContainer = mockk()

    private val walletId = UserWalletId("011")
    private val hotWalletId: HotWalletId = mockk()
    private val params = WalletBackupComponent.Params(
        userWalletId = walletId,
        isColdWalletOptionShown = true,
    )
    private val hotWalletNotBackedUp: UserWallet.Hot = mockk {
        every { walletId } returns this@WalletBackupModelTest.walletId
        every { hotWalletId } returns this@WalletBackupModelTest.hotWalletId
        every { backedUp } returns false
    }
    private val hotWalletBackedUp: UserWallet.Hot = mockk {
        every { walletId } returns this@WalletBackupModelTest.walletId
        every { hotWalletId } returns this@WalletBackupModelTest.hotWalletId
        every { backedUp } returns true
    }
    private val coldWallet: UserWallet.Cold = mockk {
        every { walletId } returns this@WalletBackupModelTest.walletId
    }

    @BeforeEach
    fun setUp() {
        every { paramsContainer.require<WalletBackupComponent.Params>() } returns params
        every { getUserWalletUseCase.invokeFlow(walletId) } returns flowOf(hotWalletNotBackedUp.right())
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns false
        coEvery { cloudBackupRepository.isBackedUp(any()) } returns false

        // DateTimeFormatters delegates to android.text.format.DateFormat, an Android stub unavailable on the JVM
        mockkStatic(DateFormat::class)
        every { DateFormat.getBestDateTimePattern(any(), any()) } answers { secondArg() }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(DateFormat::class)
    }

    @Test
    fun `GIVEN hot wallet WHEN model is created THEN context added AND BackupScreenOpened sent AND state updated`() =
        runTest {
            every { getUserWalletUseCase.invokeFlow(walletId) } returns flowOf(hotWalletNotBackedUp.right())

            val model = createModel(this)
            advanceUntilIdle()

            verify { trackingContextProxy.addHotWalletContext() }
            verify {
                analyticsEventHandler.send(WalletSettingsAnalyticEvents.BackupScreenOpened(isBackedUp = false))
            }
            val state = model.uiState.value
            Assertions.assertEquals(false, state.isBackedUp)
            Assertions.assertEquals(BackupStatus.ComingSoon, state.googleDriveStatus)
        }

    @Test
    fun `GIVEN toggle off WHEN model created THEN google drive is ComingSoon AND clickable AND not refreshed`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns false

            // Act
            val model = createModel(this)
            advanceUntilIdle()

            // Assert
            val state = model.uiState.value
            assertThat(state.googleDriveStatus).isEqualTo(BackupStatus.ComingSoon)
            assertThat(state.googleDriveOption?.text)
                .isEqualTo(resourceReference(R.string.common_coming_soon))
            assertThat(state.isGoogleDriveEnabled).isTrue()
            coVerify(exactly = 0) { cloudBackupRepository.findBackups(any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN toggle off WHEN the screen is used AND closed THEN the cloud backup graph is never touched`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns false
            val lazyRepository: Lazy<CloudBackupRepository> = mockk()
            val lazyUseCase: Lazy<SetCloudBackupStateUseCase> = mockk()

            // Act
            val model = createModel(this, lazyRepository, lazyUseCase)
            advanceUntilIdle()
            model.uiState.value.onGoogleDriveClick()
            model.onScreenResumed()
            advanceUntilIdle()
            model.onDestroy()

            // Assert
            verify(exactly = 0) { lazyRepository.get() }
            verify(exactly = 0) { lazyUseCase.get() }
        }

    @Test
    fun `WHEN the screen is closed THEN the contextual unlock is cleared`() = runTest {
        // Arrange
        val model = createModel(this)
        advanceUntilIdle()

        // Act
        model.onDestroy()

        // Assert
        verify(exactly = 1) { clearHotWalletContextualUnlockUseCase.invoke(walletId) }
    }

    @Test
    fun `GIVEN toggle on WHEN the status is still loading THEN the google drive row is disabled`() = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { cloudBackupRepository.findBackups() } coAnswers {
            awaitCancellation()
        }

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.googleDriveStatus).isEqualTo(BackupStatus.Loading)
        assertThat(state.isGoogleDriveEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN cold wallet WHEN model is created THEN context added AND BackupScreenOpened not sent AND state untouched`() =
        runTest {
            every { getUserWalletUseCase.invokeFlow(walletId) } returns flowOf(coldWallet.right())

            val model = createModel(this)
            advanceUntilIdle()

            verify { trackingContextProxy.addHotWalletContext() }
            verify(exactly = 0) {
                analyticsEventHandler.send(match<WalletSettingsAnalyticEvents.BackupScreenOpened> { true })
            }
            val state = model.uiState.value
            Assertions.assertEquals(false, state.isBackedUp)
            Assertions.assertEquals(BackupStatus.ComingSoon, state.googleDriveStatus)
        }

    @Test
    fun `GIVEN error WHEN model is created THEN context added AND BackupScreenOpened not sent`() = runTest {
        every { getUserWalletUseCase.invokeFlow(walletId) } returns flowOf(GetUserWalletError.UserWalletNotFound.left())

        createModel(this)
        advanceUntilIdle()

        verify { trackingContextProxy.addHotWalletContext() }
        verify(exactly = 0) {
            analyticsEventHandler.send(match<WalletSettingsAnalyticEvents.BackupScreenOpened> { true })
        }
    }

    @Test
    fun `WHEN onDestroy THEN trackingContextProxy removeContext is called`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        model.onDestroy()

        verify { trackingContextProxy.removeContext() }
    }

    @Test
    fun `GIVEN backed up hot wallet AND unlock success WHEN onRecoveryPhraseClick THEN ViewPhrase pushed`() = runTest {
        every { getUserWalletUseCase.invokeFlow(walletId) } returns flowOf(hotWalletBackedUp.right())
        every { getUserWalletUseCase.invoke(walletId) } returns hotWalletBackedUp.right()
        coEvery { unlockHotWalletContextualUseCase.invoke(hotWalletId) } returns mockk<UnlockHotWallet>().right()

        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onRecoveryPhraseClick()
        advanceUntilIdle()

        verify { analyticsEventHandler.send(match<WalletSettingsAnalyticEvents.ButtonRecoveryPhrase> { true }) }
        coVerify { unlockHotWalletContextualUseCase.invoke(hotWalletId) }
        verify { router.push(route = AppRoute.ViewPhrase(userWalletId = walletId), onComplete = any()) }
    }

    @Test
    fun `GIVEN backed up hot wallet AND unlock failure WHEN onRecoveryPhraseClick THEN ViewPhrase not pushed`() =
        runTest {
            every { getUserWalletUseCase.invokeFlow(walletId) } returns flowOf(hotWalletBackedUp.right())
            every { getUserWalletUseCase.invoke(walletId) } returns hotWalletBackedUp.right()
            coEvery { unlockHotWalletContextualUseCase.invoke(hotWalletId) } returns Throwable("error").left()

            val model = createModel(this)
            advanceUntilIdle()

            model.uiState.value.onRecoveryPhraseClick()
            advanceUntilIdle()

            verify { analyticsEventHandler.send(match<WalletSettingsAnalyticEvents.ButtonRecoveryPhrase> { true }) }
            coVerify { unlockHotWalletContextualUseCase.invoke(hotWalletId) }
            verify(exactly = 0) {
                router.push(route = AppRoute.ViewPhrase(userWalletId = walletId), onComplete = any())
            }
        }

    @Test
    fun `GIVEN backed up cold wallet WHEN onRecoveryPhraseClick THEN no navigation AND no unlock`() = runTest {
        every { getUserWalletUseCase.invokeFlow(walletId) } returns flowOf(hotWalletBackedUp.right())
        every { getUserWalletUseCase.invoke(walletId) } returns coldWallet.right()

        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onRecoveryPhraseClick()
        advanceUntilIdle()

        verify { analyticsEventHandler.send(match<WalletSettingsAnalyticEvents.ButtonRecoveryPhrase> { true }) }
        coVerify(exactly = 0) { unlockHotWalletContextualUseCase.invoke(any()) }
        verify(exactly = 0) { router.push(route = any(), onComplete = any()) }
    }

    @Test
    fun `GIVEN not backed up wallet WHEN onRecoveryPhraseClick THEN WalletActivation pushed`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onRecoveryPhraseClick()
        advanceUntilIdle()

        verify { analyticsEventHandler.send(match<WalletSettingsAnalyticEvents.ButtonRecoveryPhrase> { true }) }
        verify(exactly = 0) { getUserWalletUseCase.invoke(walletId) }
        coVerify(exactly = 0) { unlockHotWalletContextualUseCase.invoke(any()) }
        verify {
            router.push(
                route = AppRoute.WalletActivation(userWalletId = walletId, isBackupExists = false),
                onComplete = any(),
            )
        }
    }

    @Test
    fun `WHEN onHardwareWalletClick THEN ButtonHardwareUpdate sent AND WalletHardwareBackup pushed`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onHardwareWalletClick()

        verify { analyticsEventHandler.send(match<WalletSettingsAnalyticEvents.ButtonHardwareUpdate> { true }) }
        verify {
            router.push(
                route = AppRoute.WalletHardwareBackup(userWalletId = walletId),
                onComplete = any(),
            )
        }
    }

    @Test
    fun `WHEN onGoogleDriveClick THEN DialogMessage sent AND analytics sent`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onGoogleDriveClick()

        verify {
            analyticsEventHandler.send(
                event = match<WalletSettingsAnalyticEvents.ButtonGoogleDriveBackup> { true }
            )
        }
        verify {
            uiMessageSender.send(
                match<DialogMessage> {
                    val isTitleCorrect = it.title == resourceReference(
                        id = R.string.hw_backup_google_drive_dialog_title
                    )
                    val isMessageCorrect = it.message == resourceReference(
                        id = R.string.hw_backup_google_drive_dialog_message
                    )
                    isTitleCorrect && isMessageCorrect
                }
            )
        }
    }

    @Test
    fun `WHEN onBackClick THEN router pop is called`() = runTest {
        val model = createModel(this)
        advanceUntilIdle()

        model.uiState.value.onBackClick()

        verify { router.pop(onComplete = any()) }
    }

    @Test
    fun `GIVEN toggle on AND findBackups returns backup matching this wallet WHEN model created THEN googleDriveStatus is Done`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups() } returns listOf(backup(walletId = "011")).right()

            // Act
            val model = createModel(this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.Done)
            model.onDestroy()
        }

    @Test
    fun `GIVEN toggle on AND findBackups returns only other-wallet backups WHEN model created THEN googleDriveStatus is NoBackup`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups() } returns listOf(backup(walletId = "999")).right()

            // Act
            val model = createModel(this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.NoBackup)
            model.onDestroy()
        }

    @Test
    fun `GIVEN toggle on AND findBackups returns CloudUnavailable WHEN model created THEN googleDriveStatus is NoBackup`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups() } returns CloudBackupError.CloudUnavailable.left()

            // Act
            val model = createModel(this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.NoBackup)
            model.onDestroy()
        }

    @Test
    fun `GIVEN wallet was backed up AND findBackups returns NetworkError WHEN model created THEN status is NetworkError`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.isBackedUp("011") } returns true
            coEvery { cloudBackupRepository.findBackups() } returns CloudBackupError.NetworkError.left()

            // Act
            val model = createModel(this)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.NetworkError)
            model.onDestroy()
        }

    @ParameterizedTest
    @MethodSource("provideAccessErrors")
    fun `GIVEN wallet was backed up AND findBackups fails on access WHEN model created THEN NoAccess required`(
        error: CloudBackupError,
    ) = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { cloudBackupRepository.isBackedUp("011") } returns true
        coEvery { cloudBackupRepository.findBackups() } returns error.left()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.googleDriveStatus)
            .isEqualTo(BackupStatus.ActionRequired(BackupStatus.ActionRequired.Reason.NoAccess))
        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet was backed up AND no backup file found WHEN model created THEN FileNotFound required`() = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { cloudBackupRepository.isBackedUp("011") } returns true
        coEvery { cloudBackupRepository.findBackups() } returns emptyList<CloudBackupInfo>().right()

        // Act
        val model = createModel(this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.googleDriveStatus)
            .isEqualTo(BackupStatus.ActionRequired(BackupStatus.ActionRequired.Reason.FileNotFound))
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle on WHEN screen resumed THEN first resume is skipped AND the next one refreshes`() = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { cloudBackupRepository.findBackups() } returns emptyList<CloudBackupInfo>().right()
        val model = createModel(this)
        advanceUntilIdle()

        // Act & Assert
        model.onScreenResumed()
        advanceUntilIdle()
        coVerify(exactly = 1) { cloudBackupRepository.findBackups() }

        model.onScreenResumed()
        advanceUntilIdle()
        coVerify(exactly = 2) { cloudBackupRepository.findBackups() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN toggle on AND backup present WHEN delete confirmed THEN deleteBackup invoked AND status returns to NoBackup`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups() } returnsMany listOf(
                listOf(backup(fileId = "file-1", walletId = "011")).right(),
                emptyList<CloudBackupInfo>().right(),
            )
            coEvery { cloudBackupRepository.deleteBackup("file-1") } returns Unit.right()
            val sentMessages = mutableListOf<UiMessage>()
            every { uiMessageSender.send(capture(sentMessages)) } just Runs

            val model = createModel(this)
            advanceUntilIdle()
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.Done)

            // Act
            model.uiState.value.onGoogleDriveClick()
            val sheet = sentMessages.filterIsInstance<BottomSheetMessage>().last()
            val removeButton = sheet.messageBottomSheetUM.elements
                .filterIsInstance<MessageBottomSheetUM.Button>()
                .first()
            removeButton.onClick?.invoke(sheet.messageBottomSheetUM.closeScope)
            val confirmDialog = sentMessages.filterIsInstance<DialogMessage>().last()
            confirmDialog.firstAction.onClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { cloudBackupRepository.deleteBackup("file-1") }
            coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = false) }
            coVerify(exactly = 1) { cloudBackupRepository.findBackups() }
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.NoBackup)
            assertThat(sentMessages.any { it is SnackbarMessage }).isTrue()
            model.onDestroy()
        }

    @Test
    fun `GIVEN cloud unread AND backup exists WHEN google drive tapped THEN backup recognized AND flag restored`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups(interactive = false) } returns
                CloudBackupError.AuthRequired.left()
            coEvery { cloudBackupRepository.findBackups(interactive = true) } returns
                listOf(backup(walletId = "011")).right()

            val model = createModel(this)
            advanceUntilIdle()
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.NoBackup)

            // Act
            model.uiState.value.onGoogleDriveClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.Done)
            coVerify(exactly = 1) { setCloudBackupStateUseCase("011", isBackedUp = true) }
            verify(exactly = 0) { router.push(route = AppRoute.CreateCloudBackup(walletId), onComplete = any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN cloud unread AND no backup WHEN google drive tapped THEN create backup flow opened`() = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { cloudBackupRepository.findBackups(interactive = false) } returns CloudBackupError.AuthRequired.left()
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns emptyList<CloudBackupInfo>().right()

        val model = createModel(this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onGoogleDriveClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(route = AppRoute.CreateCloudBackup(walletId), onComplete = any()) }
        coVerify(exactly = 0) { setCloudBackupStateUseCase(any(), isBackedUp = true) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN cloud already read AND no backup WHEN google drive tapped THEN create flow opened without re-reading`() =
        runTest {
            // Arrange
            every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
            coEvery { cloudBackupRepository.findBackups(interactive = false) } returns
                emptyList<CloudBackupInfo>().right()

            val model = createModel(this)
            advanceUntilIdle()

            // Act
            model.uiState.value.onGoogleDriveClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.push(route = AppRoute.CreateCloudBackup(walletId), onComplete = any()) }
            coVerify(exactly = 0) { cloudBackupRepository.findBackups(interactive = true) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN cloud unread WHEN sign in cancelled THEN status stays NoBackup AND create flow not opened`() = runTest {
        // Arrange
        every { hotWalletFeatureToggles.isGoogleDriveBackupEnabled } returns true
        coEvery { cloudBackupRepository.findBackups(interactive = false) } returns CloudBackupError.AuthRequired.left()
        coEvery { cloudBackupRepository.findBackups(interactive = true) } returns CloudBackupError.AuthCanceled.left()

        val model = createModel(this)
        advanceUntilIdle()

        // Act
        model.uiState.value.onGoogleDriveClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.googleDriveStatus).isEqualTo(BackupStatus.NoBackup)
        verify(exactly = 0) { router.push(route = AppRoute.CreateCloudBackup(walletId), onComplete = any()) }
        model.onDestroy()
    }

    private fun backup(fileId: String = "file-1", walletId: String? = "011") = CloudBackupInfo(
        fileId = fileId,
        walletName = "wallet",
        createdAtMillis = 0L,
        walletId = walletId,
    )

    private fun createModel(
        testScope: TestScope,
        lazyRepository: Lazy<CloudBackupRepository> = dagger.Lazy { cloudBackupRepository },
        lazySetStateUseCase: Lazy<SetCloudBackupStateUseCase> = dagger.Lazy { setCloudBackupStateUseCase },
    ): WalletBackupModel {
        return WalletBackupModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getUserWalletUseCase = getUserWalletUseCase,
            unlockHotWalletContextualUseCase = unlockHotWalletContextualUseCase,
            router = router,
            trackingContextProxy = trackingContextProxy,
            analyticsEventHandler = analyticsEventHandler,
            uiMessageSender = uiMessageSender,
            cloudBackupRepository = lazyRepository,
            setCloudBackupStateUseCase = lazySetStateUseCase,
            clearHotWalletContextualUnlockUseCase = clearHotWalletContextualUnlockUseCase,
            hotWalletFeatureToggles = hotWalletFeatureToggles,
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {

        @JvmStatic
        fun provideAccessErrors() = listOf(
            CloudBackupError.AuthRequired,
            CloudBackupError.AuthPermissionsMissing,
        )
    }
}