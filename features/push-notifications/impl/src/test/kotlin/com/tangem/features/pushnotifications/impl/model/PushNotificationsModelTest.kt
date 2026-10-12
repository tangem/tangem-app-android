package com.tangem.features.pushnotifications.impl.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.pushnotificationpreferences.MarkPushNotificationFirstActivationDoneUseCase
import com.tangem.domain.settings.NeverRequestPermissionUseCase
import com.tangem.domain.settings.NeverToInitiallyAskPermissionUseCase
import com.tangem.domain.wallets.usecase.ApplyPushNotificationFirstActivationUseCase
import com.tangem.features.pushnotifications.api.PushNotificationsModelCallbacks
import com.tangem.features.pushnotifications.api.PushNotificationsParams
import com.tangem.features.pushnotifications.api.analytics.PushNotificationAnalyticEvents
import com.tangem.features.pushnotifications.impl.domain.GetPushNotificationsDoubleAskVariantUseCase
import com.tangem.features.pushnotifications.impl.domain.DoubleAskVariant
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class PushNotificationsModelTest {

    private val neverRequestPermissionUseCase: NeverRequestPermissionUseCase = mockk(relaxed = true)
    private val neverToInitiallyAskPermissionUseCase: NeverToInitiallyAskPermissionUseCase = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val analyticHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val notificationsRepository: NotificationsRepository = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val applyPushNotificationFirstActivation: ApplyPushNotificationFirstActivationUseCase = mockk()
    private val markPushNotificationFirstActivationDone: MarkPushNotificationFirstActivationDoneUseCase =
        mockk(relaxed = true)
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

    @Test
    fun `GIVEN wallets present WHEN onAllowPermission THEN first activation applied for every wallet`() = runTest {
        val w1 = UserWalletId("aa")
        val w2 = UserWalletId("bb")
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(wallet(w1), wallet(w2))
        coEvery { applyPushNotificationFirstActivation(any()) } returns Either.Right(Unit)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onAllowPermission()
        advanceUntilIdle()

        coVerifyOrder {
            applyPushNotificationFirstActivation(w1)
            applyPushNotificationFirstActivation(w2)
        }
    }

    @Test
    fun `GIVEN activation fails for first wallet WHEN onAllowPermission THEN second wallet still attempted`() =
        runTest {
            val w1 = UserWalletId("aa")
            val w2 = UserWalletId("bb")
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(wallet(w1), wallet(w2))
            coEvery { applyPushNotificationFirstActivation(w1) } returns Either.Left(RuntimeException("404"))
            coEvery { applyPushNotificationFirstActivation(w2) } returns Either.Right(Unit)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            model.onAllowPermission()
            advanceUntilIdle()

            coVerifyOrder {
                applyPushNotificationFirstActivation(w1)
                applyPushNotificationFirstActivation(w2)
            }
        }

    @Test
    fun `GIVEN wallets present WHEN onDenyPermission THEN first-activation flag marked for all wallets`() = runTest {
        val w1 = UserWalletId("aa")
        val w2 = UserWalletId("bb")
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(wallet(w1), wallet(w2))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        model.onDenyPermission()
        advanceUntilIdle()

        coVerify(exactly = 1) { markPushNotificationFirstActivationDone(w1) }
        coVerify(exactly = 1) { markPushNotificationFirstActivationDone(w2) }
        // Deny only fixes the flag — it must not enable any category.
        coVerify(exactly = 0) { applyPushNotificationFirstActivation(any()) }
    }

    @Test
    fun `GIVEN root screen with next route WHEN onAllowPermission THEN next route replaces stack once`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        val model = createModel(testScope = this, nextRoute = NEXT_ROUTE)
        advanceUntilIdle()

        // Act
        model.onAllowPermission()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { appRouter.replaceAll(routes = arrayOf(NEXT_ROUTE), onComplete = any()) }
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    @Test
    fun `GIVEN root screen with next route WHEN onDenyPermission THEN next route replaces stack once`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        val model = createModel(testScope = this, nextRoute = NEXT_ROUTE)
        advanceUntilIdle()

        // Act
        model.onDenyPermission()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { appRouter.replaceAll(routes = arrayOf(NEXT_ROUTE), onComplete = any()) }
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    @Test
    fun `GIVEN root screen with next route WHEN onLaterClick THEN next route replaces stack once`() = runTest {
        // Arrange
        coEvery { getDoubleAskVariantUseCase() } returns DoubleAskVariant.Off
        val model = createModel(testScope = this, nextRoute = NEXT_ROUTE)
        advanceUntilIdle()

        // Act
        model.onLaterClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { appRouter.replaceAll(routes = arrayOf(NEXT_ROUTE), onComplete = any()) }
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    @Test
    fun `GIVEN bottom sheet with next route WHEN onDenyPermission THEN stack is not changed`() = runTest {
        // Arrange
        coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
        val model = createModel(testScope = this, isBottomSheet = true, nextRoute = NEXT_ROUTE)
        advanceUntilIdle()

        // Act
        model.onDenyPermission()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { appRouter.replaceAll(routes = anyVararg(), onComplete = any()) }
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    @Test
    fun `GIVEN root screen WHEN onLaterClick THEN wallet reminder is retired`() = runTest {
        // Arrange
        val model = createModel(testScope = this, nextRoute = NEXT_ROUTE)
        advanceUntilIdle()

        // Act
        model.onLaterClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            notificationsRepository.setShouldShowNotifications(
                key = NotificationId.EnablePushesReminderNotification.key,
                value = false,
            )
        }
    }

    @Test
    fun `GIVEN root screen WHEN the ask is left unanswered THEN wallet reminder stays armed`() = runTest {
        // Arrange + Act
        createModel(testScope = this, nextRoute = NEXT_ROUTE)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { notificationsRepository.setShouldShowNotifications(key = any(), value = any()) }
    }

    private fun wallet(id: UserWalletId): UserWallet = mockk { every { walletId } returns id }

    @ParameterizedTest
    @MethodSource("providePermissionStatusSources")
    fun `GIVEN a source WHEN permission is answered THEN Permission Status carries the point it came from`(
        source: AppRoute.PushNotification.Source,
        expected: AnalyticsParam.ScreensSources,
        isAllowed: Boolean,
    ) = runTest {
        // Arrange
        val model = createModel(testScope = this, source = source)

        // Act
        if (isAllowed) model.onAllowPermission() else model.onDenyPermission()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticHandler.send(
                match<PushNotificationAnalyticEvents.PermissionStatus> {
                    it.params == mapOf(
                        AnalyticsParam.STATE to if (isAllowed) "Allow" else "Cancel",
                        AnalyticsParam.SOURCE to expected.value,
                    )
                },
            )
        }
    }

    private fun createModel(
        testScope: TestScope,
        isBottomSheet: Boolean = false,
        source: AppRoute.PushNotification.Source = AppRoute.PushNotification.Source.Onboarding,
        nextRoute: AppRoute? = null,
        paramsContainer: ParamsContainer = MutableParamsContainer(
            value = PushNotificationsParams(
                isBottomSheet = isBottomSheet,
                nextRoute = nextRoute,
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
            userWalletsListRepository = userWalletsListRepository,
            applyPushNotificationFirstActivation = applyPushNotificationFirstActivation,
            markPushNotificationFirstActivationDone = markPushNotificationFirstActivationDone,
            getPushNotificationsDoubleAskVariantUseCase = getDoubleAskVariantUseCase,
        )
    }

    companion object {
        val NEXT_ROUTE: AppRoute = AppRoute.Home()

        @JvmStatic
        fun providePermissionStatusSources(): List<Arguments> = listOf(
            AppRoute.PushNotification.Source.Stories to AnalyticsParam.ScreensSources.Stories,
            AppRoute.PushNotification.Source.Main to AnalyticsParam.ScreensSources.MainRequestSheet,
            AppRoute.PushNotification.Source.Onboarding to AnalyticsParam.ScreensSources.Onboarding,
            AppRoute.PushNotification.Source.ColdCreation to AnalyticsParam.ScreensSources.ColdCreation,
            AppRoute.PushNotification.Source.HotCreation to AnalyticsParam.ScreensSources.HotCreation,
            AppRoute.PushNotification.Source.FirstSignIn to AnalyticsParam.ScreensSources.FirstSignIn,
        ).flatMap { (source, expected) ->
            listOf(Arguments.of(source, expected, true), Arguments.of(source, expected, false))
        }
    }
}