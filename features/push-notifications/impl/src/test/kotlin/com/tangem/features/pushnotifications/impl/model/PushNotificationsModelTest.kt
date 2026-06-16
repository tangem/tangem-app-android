package com.tangem.features.pushnotifications.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.pushnotificationpreferences.SetAllWalletPushNotificationPreferencesUseCase
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.impl.domain.GetPushNotificationsDoubleAskVariantUseCase
import com.tangem.features.pushnotifications.impl.domain.DoubleAskVariant
import com.tangem.features.pushnotificationsettings.PushNotificationSettingsFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PushNotificationsModelTest {

    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase = mockk(relaxed = true)
    private val neverToInitiallyAskPermissionUseCase: NeverToInitiallyAskPermissionUseCase = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val analyticHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val notificationsRepository: NotificationsRepository = mockk(relaxed = true)
    private val pushNotificationSettingsFeatureToggles: PushNotificationSettingsFeatureToggles = mockk(relaxed = true)
    private val setAllWalletPushNotificationPreferences: SetAllWalletPushNotificationPreferencesUseCase =
        mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val accountsCRUDRepository: AccountsCRUDRepository = mockk(relaxed = true)
    private val getDoubleAskVariantUseCase: GetPushNotificationsDoubleAskVariantUseCase = mockk()
    private val modelCallbacks: PushNotificationsModelCallbacks = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        coEvery { getDoubleAskVariantUseCase() } returns DoubleAskVariant.Off
    }

    @Test
    fun `GIVEN onboarding treatment WHEN onLaterClick THEN double ask shown and not proceeded`() = runTest {
        coEvery { getDoubleAskVariantUseCase() } returns DoubleAskVariant.On
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onLaterClick()
        advanceUntilIdle()

        assertThat(model.isDoubleAskSheetShown.value).isTrue()
        verify {
            analyticHandler.send(
                match<PushNotificationAnalyticEvents.WarningScreenShown> {
                    it.variant == DoubleAskVariant.On.key
                },
            )
        }
        coVerify(exactly = 0) { neverRequestPermissionUseCase(any()) }
        verify(exactly = 0) { modelCallbacks.onDenySystemPermission() }
    }

    @Test
    fun `GIVEN onboarding control WHEN onLaterClick THEN proceeds without double ask`() = runTest {
        coEvery { getDoubleAskVariantUseCase() } returns DoubleAskVariant.Off
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onLaterClick()
        advanceUntilIdle()

        assertThat(model.isDoubleAskSheetShown.value).isFalse()
        coVerify { neverRequestPermissionUseCase(any()) }
        coVerify { neverToInitiallyAskPermissionUseCase(any()) }
        verify { modelCallbacks.onDenySystemPermission() }
        verify(exactly = 0) {
            analyticHandler.send(match<PushNotificationAnalyticEvents.WarningScreenShown> { true })
        }
    }

    @Test
    fun `GIVEN main bottom sheet WHEN onLaterClick THEN double ask not shown and variant not resolved`() = runTest {
        val model = createModel(
            testScope = this,
            isBottomSheet = true,
            source = AppRoute.PushNotification.Source.Main,
        )
        advanceUntilIdle()

        model.onLaterClick()
        advanceUntilIdle()

        assertThat(model.isDoubleAskSheetShown.value).isFalse()
        coVerify(exactly = 0) { getDoubleAskVariantUseCase() }
        verify { modelCallbacks.onDenySystemPermission() }
    }

    @Test
    fun `GIVEN double ask shown WHEN onDoubleAskEnableClick THEN enable tapped sent and not proceeded`() = runTest {
        coEvery { getDoubleAskVariantUseCase() } returns DoubleAskVariant.On
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onLaterClick()
        advanceUntilIdle()

        model.onDoubleAskEnableClick()
        advanceUntilIdle()

        verify {
            analyticHandler.send(match<PushNotificationAnalyticEvents.WarningScreenEnableTapped> { true })
        }
        coVerify { notificationsRepository.setUserAllowToSubscribeOnPushNotifications(true) }
        verify(exactly = 0) { modelCallbacks.onDenySystemPermission() }
    }

    @Test
    fun `GIVEN double ask shown WHEN onDoubleAskSkipClick THEN event sent and proceeded`() = runTest {
        coEvery { getDoubleAskVariantUseCase() } returns DoubleAskVariant.On
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onLaterClick()
        advanceUntilIdle()

        model.onDoubleAskSkipClick()
        advanceUntilIdle()

        verify {
            analyticHandler.send(match<PushNotificationAnalyticEvents.WarningScreenSkipTapped> { true })
        }
        coVerify { neverRequestPermissionUseCase(any()) }
        verify { modelCallbacks.onDenySystemPermission() }
    }

    @Test
    fun `GIVEN double ask shown WHEN onDoubleAskDismiss THEN sheet hidden and not proceeded`() = runTest {
        coEvery { getDoubleAskVariantUseCase() } returns DoubleAskVariant.On
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.onLaterClick()
        advanceUntilIdle()

        model.onDoubleAskDismiss()
        advanceUntilIdle()

        assertThat(model.isDoubleAskSheetShown.value).isFalse()
        verify(exactly = 0) { modelCallbacks.onDenySystemPermission() }
        verify(exactly = 0) {
            analyticHandler.send(match<PushNotificationAnalyticEvents.WarningScreenSkipTapped> { true })
        }
    }

    private fun createModel(
        testScope: TestScope,
        isBottomSheet: Boolean = false,
        source: AppRoute.PushNotification.Source = AppRoute.PushNotification.Source.Onboarding,
        paramsContainer: ParamsContainer = MutableParamsContainer(
            value = PushNotificationsParams(
                isBottomSheet = isBottomSheet,
                nextRoute = null,
                modelCallbacks = modelCallbacks,
                source = source,
            ),
        ),
    ): PushNotificationsModel {
        return PushNotificationsModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            neverRequestPermissionUseCase = neverRequestPermissionUseCase,
            neverToInitiallyAskPermissionUseCase = neverToInitiallyAskPermissionUseCase,
            appRouter = appRouter,
            analyticHandler = analyticHandler,
            notificationsRepository = notificationsRepository,
            pushNotificationSettingsFeatureToggles = pushNotificationSettingsFeatureToggles,
            setAllWalletPushNotificationPreferences = setAllWalletPushNotificationPreferences,
            userWalletsListRepository = userWalletsListRepository,
            accountsCRUDRepository = accountsCRUDRepository,
            getPushNotificationsDoubleAskVariantUseCase = getDoubleAskVariantUseCase,
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