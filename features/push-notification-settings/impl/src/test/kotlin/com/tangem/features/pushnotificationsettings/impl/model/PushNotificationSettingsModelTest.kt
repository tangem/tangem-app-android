package com.tangem.features.pushnotificationsettings.impl.model

import app.cash.turbine.test
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.pushnotificationpreferences.IsPushNotificationFirstActivationDoneUseCase
import com.tangem.domain.pushnotificationpreferences.MarkPushNotificationFirstActivationDoneUseCase
import com.tangem.domain.pushnotificationpreferences.ObserveWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.SetAllWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.UpdateWalletPushNotificationPreferenceUseCase
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.domain.wallets.usecase.ApplyPushNotificationFirstActivationUseCase
import com.tangem.domain.wallets.usecase.SetNotificationsEnabledUseCase
import com.tangem.features.pushnotificationsettings.component.PushNotificationSettingsComponent
import com.tangem.features.pushnotificationsettings.impl.entity.PushNotificationSettingsUM
import com.tangem.features.pushnotificationsettings.impl.entity.ToggleId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@Suppress("LongParameterList", "LargeClass")
class PushNotificationSettingsModelTest {

    private val userWalletId = UserWalletId("0011223344556677")

    private val observePreferences: ObserveWalletPushNotificationPreferencesUseCase = mockk()
    private val updatePreference: UpdateWalletPushNotificationPreferenceUseCase = mockk()
    private val setAllPreferences: SetAllWalletPushNotificationPreferencesUseCase = mockk()
    private val setNotificationsEnabled: SetNotificationsEnabledUseCase = mockk()
    private val isFirstActivationDone: IsPushNotificationFirstActivationDoneUseCase = mockk()
    private val markFirstActivationDone: MarkPushNotificationFirstActivationDoneUseCase = mockk(relaxed = true)
    private val applyFirstActivation: ApplyPushNotificationFirstActivationUseCase = mockk()
    private val notificationsRepository: NotificationsRepository = mockk()
    private val systemNotificationsStateProvider: SystemNotificationsStateProvider = mockk()
    private val settingsManager: SettingsManager = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private fun model(
        osEnabled: Boolean = true,
        firstActivationDone: Boolean = true,
        consentGiven: Boolean = false,
        activationResult: Either<Throwable, Unit> = Either.Right(Unit),
        preferencesFlow: MutableSharedFlow<WalletPushNotificationPreferences> = MutableSharedFlow(replay = 1),
    ): PushNotificationSettingsModel {
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns osEnabled
        every { observePreferences(userWalletId) } returns preferencesFlow
        coEvery { isFirstActivationDone(userWalletId) } returns firstActivationDone
        coEvery { setNotificationsEnabled(any(), any()) } returns Either.Right(Unit)
        coEvery { setAllPreferences(any(), any(), any(), any()) } returns Either.Right(Unit)
        coEvery { notificationsRepository.isUserAllowToSubscribeOnPushNotifications() } returns consentGiven
        coEvery { applyFirstActivation(any()) } returns activationResult
        return PushNotificationSettingsModel(
            paramsContainer = MutableParamsContainer(PushNotificationSettingsComponent.Params(userWalletId)),
            dispatchers = TestingCoroutineDispatcherProvider(),
            messageSender = messageSender,
            analyticsEventHandler = analyticsEventHandler,
            observePreferences = observePreferences,
            updatePreference = updatePreference,
            systemNotificationsStateProvider = systemNotificationsStateProvider,
            settingsManager = settingsManager,
            setAllPreferences = setAllPreferences,
            setNotificationsEnabled = setNotificationsEnabled,
            isFirstActivationDone = isFirstActivationDone,
            markFirstActivationDone = markFirstActivationDone,
            applyFirstActivation = applyFirstActivation,
            notificationsRepository = notificationsRepository,
        )
    }

    @Test
    fun `GIVEN cache populated WHEN model created THEN ui state becomes Content`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(preferencesFlow = flow)
        advanceUntilIdle()

        model.uiState.test {
            assertThat(awaitItem()).isInstanceOf(PushNotificationSettingsUM.Content::class.java)
        }
    }

    @Test
    fun `GIVEN observe throws WHEN model created THEN ui state becomes Error`() = runTest {
        every { observePreferences(userWalletId) } returns flow { throw IllegalStateException("boom") }
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        coEvery { isFirstActivationDone(userWalletId) } returns true
        coEvery { setNotificationsEnabled(any(), any()) } returns Either.Right(Unit)
        coEvery { setAllPreferences(any(), any(), any(), any()) } returns Either.Right(Unit)
        coEvery { notificationsRepository.isUserAllowToSubscribeOnPushNotifications() } returns false
        coEvery { applyFirstActivation(any()) } returns Either.Right(Unit)

        val model = PushNotificationSettingsModel(
            paramsContainer = MutableParamsContainer(PushNotificationSettingsComponent.Params(userWalletId)),
            dispatchers = TestingCoroutineDispatcherProvider(),
            messageSender = messageSender,
            analyticsEventHandler = analyticsEventHandler,
            observePreferences = observePreferences,
            updatePreference = updatePreference,
            systemNotificationsStateProvider = systemNotificationsStateProvider,
            settingsManager = settingsManager,
            setAllPreferences = setAllPreferences,
            setNotificationsEnabled = setNotificationsEnabled,
            isFirstActivationDone = isFirstActivationDone,
            markFirstActivationDone = markFirstActivationDone,
            applyFirstActivation = applyFirstActivation,
            notificationsRepository = notificationsRepository,
        )
        advanceUntilIdle()

        model.uiState.test {
            assertThat(awaitItem()).isInstanceOf(PushNotificationSettingsUM.Error::class.java)
        }
    }

    @Test
    fun `GIVEN OS enabled AND any toggle on WHEN built THEN banner is Hidden`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(anyOn())
        val model = model(osEnabled = true, preferencesFlow = flow)
        advanceUntilIdle()

        val content = model.uiState.value as PushNotificationSettingsUM.Content
        assertThat(content.banner).isNull()
    }

    @Test
    fun `GIVEN OS disabled AND any toggle on WHEN built THEN banner is Visible`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(anyOn())
        val model = model(osEnabled = false, preferencesFlow = flow)
        advanceUntilIdle()

        val content = model.uiState.value as PushNotificationSettingsUM.Content
        assertThat(content.banner).isNotNull()
    }

    @Test
    fun `GIVEN OS disabled AND no toggle on WHEN built THEN banner is Hidden`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, preferencesFlow = flow)
        advanceUntilIdle()

        val content = model.uiState.value as PushNotificationSettingsUM.Content
        assertThat(content.banner).isNull()
    }

    @Test
    fun `GIVEN OS enabled WHEN toggle flipped on THEN repository is updated`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        coEvery {
            updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
        } returns Either.Right(Unit)
        val model = model(osEnabled = true, preferencesFlow = flow)
        advanceUntilIdle()

        val content = model.uiState.value as PushNotificationSettingsUM.Content
        val offers = content.toggles.first { it.id == ToggleId.OffersUpdates }
        offers.onCheckedChange(true)
        advanceUntilIdle()

        coVerify {
            updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
        }
    }

    @Test
    fun `GIVEN repository write fails WHEN toggle flipped THEN message is sent and toggle is reverted`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        coEvery {
            updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
        } returns Either.Left(RuntimeException("network"))
        val model = model(osEnabled = true, preferencesFlow = flow)
        advanceUntilIdle()

        val offers = (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
        offers.onCheckedChange(true)
        advanceUntilIdle()

        coVerify(atLeast = 1) { messageSender.send(any()) }
        val current = (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
        assertThat(current.isOn).isFalse()
    }

    @Test
    fun `GIVEN two toggles flipped WHEN one write fails THEN only the failed toggle reverts`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        coEvery {
            updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, true)
        } returns Either.Left(RuntimeException("network"))
        coEvery {
            updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
        } returns Either.Right(Unit)
        val model = model(osEnabled = true, preferencesFlow = flow)
        advanceUntilIdle()

        // Flip both toggles optimistically before either write resolves.
        val initial = model.uiState.value as PushNotificationSettingsUM.Content
        initial.toggles.first { it.id == ToggleId.TransactionAlerts }.onCheckedChange(true)
        initial.toggles.first { it.id == ToggleId.OffersUpdates }.onCheckedChange(true)
        advanceUntilIdle()

        // The failed TransactionAlerts write reverts only itself; OffersUpdates keeps its value.
        val toggles = (model.uiState.value as PushNotificationSettingsUM.Content).toggles
        assertThat(toggles.first { it.id == ToggleId.TransactionAlerts }.isOn).isFalse()
        assertThat(toggles.first { it.id == ToggleId.OffersUpdates }.isOn).isTrue()
    }

    @Test
    fun `GIVEN transaction alerts on WHEN written THEN tokens synced before preference`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        coEvery {
            updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, true)
        } returns Either.Right(Unit)
        val model = model(osEnabled = true, firstActivationDone = true, preferencesFlow = flow)
        advanceUntilIdle()

        (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.TransactionAlerts }
            .onCheckedChange(true)
        advanceUntilIdle()

        // Order matters: token re-subscription (which carries addresses) precedes the preferences write (spec §13.2).
        coVerifyOrder {
            setNotificationsEnabled(userWalletId, isEnabled = true)
            updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, true)
        }
    }

    @Test
    fun `GIVEN OS disabled WHEN toggle ON THEN permission request is triggered`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, preferencesFlow = flow)
        advanceUntilIdle()

        model.requestPushPermission.test {
            val offers = (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
            offers.onCheckedChange(true)
            advanceUntilIdle()

            awaitItem()
            expectNoEvents()
        }
        coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
    }

    @Test
    fun `WHEN banner CTA tapped THEN OS notification settings are opened`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(anyOn())
        val model = model(osEnabled = false, preferencesFlow = flow)
        advanceUntilIdle()

        val banner = requireNotNull(
            (model.uiState.value as PushNotificationSettingsUM.Content).banner,
        )
        model.requestPushPermission.test {
            banner.onOpenSettingsClick()
            advanceUntilIdle()

            // The banner CTA opens system settings directly and never asks for the permission.
            expectNoEvents()
        }
        verify(exactly = 1) { settingsManager.openAppNotificationSettings() }
    }

    @Test
    fun `GIVEN first activation not done WHEN Allow THEN all three categories enabled`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, firstActivationDone = false, preferencesFlow = flow)
        advanceUntilIdle()

        // Tap a single toggle in not_determined state -> OS prompt fires.
        (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
            .onCheckedChange(true)
        advanceUntilIdle()
        // User taps Allow.
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        model.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        // First-activation rule: all three enabled at once (not just the tapped one), token re-subscription, flag fixed.
        coVerify(exactly = 1) { setAllPreferences(userWalletId, true, true, true) }
        coVerify(exactly = 1) { setNotificationsEnabled(userWalletId, isEnabled = true) }
        coVerify(exactly = 1) { markFirstActivationDone(userWalletId) }
        coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
    }

    @Test
    fun `GIVEN first activation done WHEN Allow THEN only tapped toggle enabled`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        coEvery {
            updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
        } returns Either.Right(Unit)
        val model = model(osEnabled = false, firstActivationDone = true, preferencesFlow = flow)
        advanceUntilIdle()

        (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
            .onCheckedChange(true)
        advanceUntilIdle()
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        model.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        coVerify(exactly = 1) { updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true) }
        // Selective: exactly one preference write (the tapped one), no bulk enable, and the flag is already set.
        coVerify(exactly = 1) { updatePreference(any(), any(), any()) }
        coVerify(exactly = 0) { setAllPreferences(any(), any(), any(), any()) }
        coVerify(exactly = 0) { markFirstActivationDone(userWalletId) }
    }

    @Test
    fun `GIVEN permission granted but notifications disabled WHEN result THEN dialog shown AND flag not marked`() =
        runTest {
            val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
            flow.tryEmit(allFalse())
            val model = model(osEnabled = false, firstActivationDone = false, preferencesFlow = flow)
            advanceUntilIdle()

            (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
                .onCheckedChange(true)
            advanceUntilIdle()
            // POST_NOTIFICATIONS already granted (launcher returns true) but notifications stay OFF at OS level.
            model.onPermissionResult(isGranted = true)
            advanceUntilIdle()

            coVerify(exactly = 0) { setAllPreferences(any(), any(), any(), any()) }
            coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
            coVerify(exactly = 1) { messageSender.send(any()) }
            coVerify(exactly = 0) { markFirstActivationDone(userWalletId) }
        }

    @Test
    fun `GIVEN transaction alerts on AND tokens write fails WHEN written THEN preference not written and reverted`() =
        runTest {
            val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
            flow.tryEmit(allFalse())
            val model = model(osEnabled = true, firstActivationDone = true, preferencesFlow = flow)
            // Registered AFTER model() so this specific stub wins over model()'s any()-> Right default.
            coEvery {
                setNotificationsEnabled(userWalletId, isEnabled = true)
            } returns Either.Left(RuntimeException("net"))
            advanceUntilIdle()

            (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.TransactionAlerts }
                .onCheckedChange(true)
            advanceUntilIdle()

            coVerify(exactly = 0) { updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, any()) }
            coVerify(atLeast = 1) { messageSender.send(any()) }
            val tx = (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.TransactionAlerts }
            assertThat(tx.isOn).isFalse()
        }

    @Test
    fun `GIVEN transaction alerts on AND preference write fails WHEN written THEN token subscription undone`() =
        runTest {
            val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
            flow.tryEmit(allFalse())
            coEvery {
                updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, true)
            } returns Either.Left(RuntimeException("net"))
            val model = model(osEnabled = true, firstActivationDone = true, preferencesFlow = flow)
            advanceUntilIdle()

            (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.TransactionAlerts }
                .onCheckedChange(true)
            advanceUntilIdle()

            // tokens on -> preferences fail -> compensating tokens off; toggle reverted.
            coVerifyOrder {
                setNotificationsEnabled(userWalletId, isEnabled = true)
                updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, true)
                setNotificationsEnabled(userWalletId, isEnabled = false)
            }
            val tx = (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.TransactionAlerts }
            assertThat(tx.isOn).isFalse()
        }

    @Test
    fun `GIVEN first activation AND tokens write fails WHEN Allow THEN reverted and flag not marked`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, firstActivationDone = false, preferencesFlow = flow)
        // Registered AFTER model() so this specific stub wins over model()'s any()-> Right default.
        coEvery { setNotificationsEnabled(userWalletId, isEnabled = true) } returns Either.Left(RuntimeException("net"))
        advanceUntilIdle()

        (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
            .onCheckedChange(true)
        advanceUntilIdle()
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        model.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { setAllPreferences(any(), any(), any(), any()) }
        coVerify(exactly = 0) { markFirstActivationDone(userWalletId) }
        coVerify(atLeast = 1) { messageSender.send(any()) }
    }

    @Test
    fun `GIVEN first activation AND preferences write fails WHEN Allow THEN tokens undone and flag not marked`() =
        runTest {
            val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
            flow.tryEmit(allFalse())
            val model = model(osEnabled = false, firstActivationDone = false, preferencesFlow = flow)
            // Registered AFTER model() so this specific stub wins over model()'s any()-> Right default.
            coEvery {
                setAllPreferences(userWalletId, true, true, true)
            } returns Either.Left(RuntimeException("net"))
            advanceUntilIdle()

            (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
                .onCheckedChange(true)
            advanceUntilIdle()
            every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
            model.onPermissionResult(isGranted = true)
            advanceUntilIdle()

            coVerifyOrder {
                setNotificationsEnabled(userWalletId, isEnabled = true)
                setAllPreferences(userWalletId, true, true, true)
                setNotificationsEnabled(userWalletId, isEnabled = false)
            }
            coVerify(exactly = 0) { markFirstActivationDone(userWalletId) }
        }

    @Test
    fun `WHEN Deny THEN dialog shown AND flag not marked AND no writes`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, firstActivationDone = false, preferencesFlow = flow)
        advanceUntilIdle()

        (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
            .onCheckedChange(true)
        advanceUntilIdle()
        model.onPermissionResult(isGranted = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { messageSender.send(any()) }
        coVerify(exactly = 0) { markFirstActivationDone(userWalletId) }
        coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
        coVerify(exactly = 0) { setAllPreferences(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN tapped toggle refused WHEN notifications enabled in settings THEN toggle applied on resume`() =
        runTest {
            // Arrange
            val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
            flow.tryEmit(allFalse())
            coEvery {
                updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
            } returns Either.Right(Unit)
            val model = model(osEnabled = false, firstActivationDone = true, preferencesFlow = flow)
            advanceUntilIdle()

            // Act: tap -> refused -> notifications enabled in the OS settings -> back to the screen.
            (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
                .onCheckedChange(true)
            advanceUntilIdle()
            model.onPermissionResult(isGranted = false)
            advanceUntilIdle()
            every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
            model.onResume()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true) }
            val offers = (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
            assertThat(offers.isOn).isTrue()
        }

    @Test
    fun `GIVEN first activation not done AND toggle refused WHEN notifications enabled THEN all three enabled`() =
        runTest {
            // Arrange
            val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
            flow.tryEmit(allFalse())
            val model = model(osEnabled = false, firstActivationDone = false, preferencesFlow = flow)
            advanceUntilIdle()

            // Act
            (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
                .onCheckedChange(true)
            advanceUntilIdle()
            model.onPermissionResult(isGranted = false)
            advanceUntilIdle()
            every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
            model.onResume()
            advanceUntilIdle()

            // Assert: the refusal did not consume the first-activation rule, so it runs on the late grant.
            coVerify(exactly = 1) { setNotificationsEnabled(userWalletId, isEnabled = true) }
            coVerify(exactly = 1) { setAllPreferences(userWalletId, true, true, true) }
            coVerify(exactly = 1) { markFirstActivationDone(userWalletId) }
            coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
        }

    @Test
    fun `GIVEN tapped toggle refused WHEN returning with notifications still disabled THEN nothing is written`() =
        runTest {
            // Arrange
            val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
            flow.tryEmit(allFalse())
            val model = model(osEnabled = false, firstActivationDone = true, preferencesFlow = flow)
            advanceUntilIdle()

            // Act
            (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
                .onCheckedChange(true)
            advanceUntilIdle()
            model.onPermissionResult(isGranted = false)
            advanceUntilIdle()
            model.onResume()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
            coVerify(exactly = 0) { setAllPreferences(any(), any(), any(), any()) }
            coVerify(exactly = 0) { markFirstActivationDone(any()) }
            val offers = (model.uiState.value as PushNotificationSettingsUM.Content)
                .toggles.first { it.id == ToggleId.OffersUpdates }
            assertThat(offers.isOn).isFalse()
        }

    @Test
    fun `GIVEN dialog declined WHEN notifications enabled afterwards THEN tapped toggle is not applied`() = runTest {
        // Arrange
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, firstActivationDone = true, preferencesFlow = flow)
        advanceUntilIdle()
        val dialog = slot<DialogMessage>()

        // Act
        (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
            .onCheckedChange(true)
        advanceUntilIdle()
        model.onPermissionResult(isGranted = false)
        advanceUntilIdle()
        verify { messageSender.send(capture(dialog)) }
        dialog.captured.secondAction?.onClick?.invoke()
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        model.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
        coVerify(exactly = 0) { setAllPreferences(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN consent and OS enabled WHEN preferences loaded THEN first activation reapplied`() = runTest {
        // Arrange
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())

        // Act
        model(osEnabled = true, consentGiven = true, preferencesFlow = flow)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { applyFirstActivation(userWalletId) }
    }

    @Test
    fun `GIVEN preferences emitted twice WHEN loaded THEN first activation attempted once`() = runTest {
        // Arrange
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())

        // Act
        model(osEnabled = true, consentGiven = true, preferencesFlow = flow)
        advanceUntilIdle()
        flow.tryEmit(anyOn())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { applyFirstActivation(userWalletId) }
    }

    @Test
    fun `GIVEN no consent WHEN preferences loaded THEN first activation not reapplied`() = runTest {
        // Arrange
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())

        // Act
        model(osEnabled = true, consentGiven = false, preferencesFlow = flow)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { applyFirstActivation(any()) }
    }

    @Test
    fun `GIVEN OS disabled WHEN preferences loaded THEN first activation not reapplied`() = runTest {
        // Arrange
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())

        // Act
        model(osEnabled = false, consentGiven = true, preferencesFlow = flow)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { applyFirstActivation(any()) }
    }

    @Test
    fun `GIVEN activation failed WHEN preferences emitted again THEN not retried`() = runTest {
        // Arrange
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())

        // Act
        model(
            osEnabled = true,
            consentGiven = true,
            activationResult = Either.Left(RuntimeException("net")),
            preferencesFlow = flow,
        )
        advanceUntilIdle()
        flow.tryEmit(anyOn())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { applyFirstActivation(userWalletId) }
    }

    @Test
    fun `GIVEN OS enabled after pause WHEN onResume THEN first activation reapplied`() = runTest {
        // Arrange
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, consentGiven = true, preferencesFlow = flow)
        advanceUntilIdle()

        // Act: the user enabled notifications in the OS settings and returned to the screen.
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        model.onResume()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { applyFirstActivation(userWalletId) }
    }

    private fun allFalse() = WalletPushNotificationPreferences(
        transactionAlerts = PushNotificationPreference(isEnabled = false),
        offersUpdates = PushNotificationPreference(isEnabled = false),
        priceAlerts = PushNotificationPreference(isEnabled = false),
    )

    private fun anyOn() = WalletPushNotificationPreferences(
        transactionAlerts = PushNotificationPreference(isEnabled = true),
        offersUpdates = PushNotificationPreference(isEnabled = false),
        priceAlerts = PushNotificationPreference(isEnabled = false),
    )
}