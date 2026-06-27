package com.tangem.features.hotwallet.walletbackup.model

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.analytics.WalletSettingsAnalyticEvents
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.WalletBackupComponent
import com.tangem.features.hotwallet.walletbackup.entity.BackupStatus
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class WalletBackupModelTest {

    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase = mockk()
    private val router: Router = mockk(relaxUnitFun = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
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
            Assertions.assertEquals(BackupStatus.NoBackup, state.googleDriveStatus)
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

    private fun createModel(testScope: TestScope): WalletBackupModel {
        return WalletBackupModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getUserWalletUseCase = getUserWalletUseCase,
            unlockHotWalletContextualUseCase = unlockHotWalletContextualUseCase,
            router = router,
            trackingContextProxy = trackingContextProxy,
            analyticsEventHandler = analyticsEventHandler,
            uiMessageSender = uiMessageSender,
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
}