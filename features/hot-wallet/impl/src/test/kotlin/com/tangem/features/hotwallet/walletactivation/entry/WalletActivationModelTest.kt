package com.tangem.features.hotwallet.walletactivation.entry

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.hotwallet.SetAccessCodeSkippedUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.settings.ShouldAskPermissionUseCase
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.WalletActivationComponent
import com.tangem.features.hotwallet.walletactivation.entry.routing.WalletActivationRoute
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class WalletActivationModelTest {

    private val router: Router = mockk(relaxUnitFun = true)
    private val shouldAskPermissionUseCase: ShouldAskPermissionUseCase = mockk()
    private val setAccessCodeSkippedUseCase: SetAccessCodeSkippedUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val trackingContextProxy: TrackingContextProxy = mockk(relaxUnitFun = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase = mockk()
    private val paramsContainer: ParamsContainer = mockk()

    private val walletId = UserWalletId("011")

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            shouldAskPermissionUseCase,
            setAccessCodeSkippedUseCase,
            getUserWalletUseCase,
            uiMessageSender,
            trackingContextProxy,
            analyticsEventHandler,
            clearHotWalletContextualUnlockUseCase,
            paramsContainer,
        )
        every { clearHotWalletContextualUnlockUseCase(walletId) } returns Unit.right()
        coEvery { shouldAskPermissionUseCase(any()) } returns false
    }

    @Test
    fun `GIVEN access code is set WHEN manual backup completed THEN access code screen is skipped`() = runTest {
        // Arrange
        val model = createModel(testScope = this, authType = HotWalletId.AuthType.Password)
        val stacks = trackStacks(model)

        // Act
        model.manualBackupCompletedModelCallbacks.onContinueClick(walletId)
        advanceUntilIdle()

        // Assert
        assertThat(model.isAccessCodeStepRequired).isFalse()
        assertThat(stacks.last()).containsExactly(WalletActivationRoute.SetupFinished)
    }

    @Test
    fun `GIVEN access code is not set WHEN manual backup completed THEN access code screen is shown`() = runTest {
        // Arrange
        val model = createModel(testScope = this, authType = HotWalletId.AuthType.NoPassword)
        val stacks = trackStacks(model)

        // Act
        model.manualBackupCompletedModelCallbacks.onContinueClick(walletId)
        advanceUntilIdle()

        // Assert
        assertThat(model.isAccessCodeStepRequired).isTrue()
        assertThat(stacks.last().last()).isEqualTo(WalletActivationRoute.SetAccessCode)
    }

    @Test
    fun `GIVEN access code is set AND push permission required WHEN backup completed THEN push screen is shown`() =
        runTest {
            // Arrange
            coEvery { shouldAskPermissionUseCase(any()) } returns true
            val model = createModel(testScope = this, authType = HotWalletId.AuthType.Biometry)
            val stacks = trackStacks(model)

            // Act
            model.manualBackupCompletedModelCallbacks.onContinueClick(walletId)
            advanceUntilIdle()

            // Assert
            assertThat(stacks.last()).containsExactly(WalletActivationRoute.PushNotifications)
        }

    @Test
    fun `GIVEN backup exists AND access code is set WHEN model created THEN flow starts on setup finished`() =
        runTest {
            // Act
            val model = createModel(
                testScope = this,
                authType = HotWalletId.AuthType.Password,
                isBackupExists = true,
            )

            // Assert
            assertThat(model.isAccessCodeStepRequired).isFalse()
            assertThat(model.startRoute).isEqualTo(WalletActivationRoute.SetupFinished)
        }

    @Test
    fun `GIVEN backup exists AND access code is not set WHEN model created THEN flow starts on access code`() =
        runTest {
            // Act
            val model = createModel(
                testScope = this,
                authType = HotWalletId.AuthType.NoPassword,
                isBackupExists = true,
            )

            // Assert
            assertThat(model.isAccessCodeStepRequired).isTrue()
            assertThat(model.startRoute).isEqualTo(WalletActivationRoute.SetAccessCode)
        }

    @Test
    fun `GIVEN wallets are not loaded WHEN manual backup completed THEN access code screen is shown`() = runTest {
        // Arrange
        every { getUserWalletUseCase(walletId) } throws IllegalStateException("User wallets list is not loaded")
        val model = createModel(testScope = this, authType = null)
        val stacks = trackStacks(model)

        // Act
        model.manualBackupCompletedModelCallbacks.onContinueClick(walletId)
        advanceUntilIdle()

        // Assert
        assertThat(model.isAccessCodeStepRequired).isTrue()
        assertThat(stacks.last().last()).isEqualTo(WalletActivationRoute.SetAccessCode)
    }

    private fun trackStacks(model: WalletActivationModel): List<List<WalletActivationRoute>> {
        val stacks = mutableListOf<List<WalletActivationRoute>>()
        model.stackNavigation.subscribe { event -> stacks += event.transformer(listOf(model.startRoute)) }
        return stacks
    }

    private fun createModel(
        testScope: TestScope,
        authType: HotWalletId.AuthType?,
        isBackupExists: Boolean = false,
    ): WalletActivationModel {
        every { paramsContainer.require<WalletActivationComponent.Params>() } returns
            WalletActivationComponent.Params(
                userWalletId = walletId,
                isBackupExists = isBackupExists,
            )
        if (authType != null) {
            val hotWalletId: HotWalletId = mockk { every { this@mockk.authType } returns authType }
            val hotWallet: UserWallet.Hot = mockk {
                every { walletId } returns this@WalletActivationModelTest.walletId
                every { this@mockk.hotWalletId } returns hotWalletId
            }
            every { getUserWalletUseCase(walletId) } returns hotWallet.right()
        }

        return WalletActivationModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            router = router,
            shouldAskPermissionUseCase = shouldAskPermissionUseCase,
            setAccessCodeSkippedUseCase = setAccessCodeSkippedUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            uiMessageSender = uiMessageSender,
            trackingContextProxy = trackingContextProxy,
            analyticsEventHandler = analyticsEventHandler,
            clearHotWalletContextualUnlockUseCase = clearHotWalletContextualUnlockUseCase,
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