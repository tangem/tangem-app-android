package com.tangem.features.pushnotificationsettings.impl.model

import app.cash.turbine.test
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pushnotificationpreferences.ObserveWalletPushNotificationPreferencesUseCase
import com.tangem.domain.pushnotificationpreferences.UpdateWalletPushNotificationPreferenceUseCase
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationCategory
import com.tangem.domain.pushnotificationpreferences.models.PushNotificationPreference
import com.tangem.domain.pushnotificationpreferences.models.WalletPushNotificationPreferences
import com.tangem.features.pushnotificationsettings.component.PushNotificationSettingsComponent
import com.tangem.features.pushnotificationsettings.impl.entity.AllowPushNotificationsBannerUM
import com.tangem.features.pushnotificationsettings.impl.entity.PushNotificationSettingsUM
import com.tangem.features.pushnotificationsettings.impl.entity.ToggleId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@Suppress("LongParameterList")
class PushNotificationSettingsModelTest {

    private val userWalletId = UserWalletId("0011223344556677")

    private val observePreferences: ObserveWalletPushNotificationPreferencesUseCase = mockk()
    private val updatePreference: UpdateWalletPushNotificationPreferenceUseCase = mockk()
    private val systemNotificationsStateProvider: SystemNotificationsStateProvider = mockk()
    private val settingsManager: SettingsManager = mockk(relaxed = true)
    private val accountsCRUDRepository: AccountsCRUDRepository = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private fun model(
        osEnabled: Boolean = true,
        preferencesFlow: MutableSharedFlow<WalletPushNotificationPreferences> = MutableSharedFlow(replay = 1),
    ): PushNotificationSettingsModel {
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns osEnabled
        every { observePreferences(userWalletId) } returns preferencesFlow
        return PushNotificationSettingsModel(
            paramsContainer = MutableParamsContainer(PushNotificationSettingsComponent.Params(userWalletId)),
            dispatchers = TestingCoroutineDispatcherProvider(),
            messageSender = messageSender,
            analyticsEventHandler = analyticsEventHandler,
            observePreferences = observePreferences,
            updatePreference = updatePreference,
            systemNotificationsStateProvider = systemNotificationsStateProvider,
            settingsManager = settingsManager,
            accountsCRUDRepository = accountsCRUDRepository,
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

        val model = PushNotificationSettingsModel(
            paramsContainer = MutableParamsContainer(PushNotificationSettingsComponent.Params(userWalletId)),
            dispatchers = TestingCoroutineDispatcherProvider(),
            messageSender = messageSender,
            analyticsEventHandler = analyticsEventHandler,
            observePreferences = observePreferences,
            updatePreference = updatePreference,
            systemNotificationsStateProvider = systemNotificationsStateProvider,
            settingsManager = settingsManager,
            accountsCRUDRepository = accountsCRUDRepository,
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
    fun `GIVEN OS disabled WHEN toggle ON THEN permission request is triggered`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, preferencesFlow = flow)
        advanceUntilIdle()

        val offers = (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
        offers.onCheckedChange(true)
        advanceUntilIdle()

        val content = model.uiState.value as PushNotificationSettingsUM.Content
        assertThat(content.requestPermissionEvent.javaClass.simpleName).isEqualTo("Triggered")
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
        banner.onOpenSettingsClick()
        advanceUntilIdle()

        verify(exactly = 1) { settingsManager.openAppNotificationSettings() }
        val refreshed = model.uiState.value as PushNotificationSettingsUM.Content
        assertThat(refreshed.requestPermissionEvent.javaClass.simpleName).isEqualTo("Consumed")
    }

    @Test
    fun `WHEN Allow on a single tapped toggle THEN only that toggle is enabled`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        coEvery {
            updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
        } returns Either.Right(Unit)

        val model = model(osEnabled = false, preferencesFlow = flow)
        advanceUntilIdle()

        val offers = (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
        offers.onCheckedChange(true)
        advanceUntilIdle()
        // OS prompt fires; user taps Allow.
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true
        model.onPermissionResult(isGranted = true)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            updatePreference(userWalletId, PushNotificationCategory.OffersUpdates, true)
        }
        coVerify(exactly = 0) {
            updatePreference(userWalletId, PushNotificationCategory.TransactionAlerts, any())
        }
        coVerify(exactly = 0) {
            updatePreference(userWalletId, PushNotificationCategory.PriceAlerts, any())
        }
    }

    @Test
    fun `WHEN Deny THEN Enable Notifications dialog is shown and no PUT`() = runTest {
        val flow = MutableSharedFlow<WalletPushNotificationPreferences>(replay = 1)
        flow.tryEmit(allFalse())
        val model = model(osEnabled = false, preferencesFlow = flow)
        advanceUntilIdle()

        val offers = (model.uiState.value as PushNotificationSettingsUM.Content)
            .toggles.first { it.id == ToggleId.OffersUpdates }
        offers.onCheckedChange(true)
        advanceUntilIdle()
        model.onPermissionResult(isGranted = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { messageSender.send(any()) }
        coVerify(exactly = 0) { updatePreference(any(), any(), any()) }
    }

    private fun allFalse() = WalletPushNotificationPreferences(
        transactionAlerts = PushNotificationPreference(isEnabled = false, isVisible = true),
        offersUpdates = PushNotificationPreference(isEnabled = false, isVisible = true),
        priceAlerts = PushNotificationPreference(isEnabled = false, isVisible = true),
    )

    private fun anyOn() = WalletPushNotificationPreferences(
        transactionAlerts = PushNotificationPreference(isEnabled = true, isVisible = true),
        offersUpdates = PushNotificationPreference(isEnabled = false, isVisible = true),
        priceAlerts = PushNotificationPreference(isEnabled = false, isVisible = true),
    )
}