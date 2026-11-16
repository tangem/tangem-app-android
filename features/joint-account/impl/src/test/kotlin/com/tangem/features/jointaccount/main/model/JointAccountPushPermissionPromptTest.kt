package com.tangem.features.jointaccount.main.model

import app.cash.turbine.test
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.notifications.SystemNotificationsStateProvider
import com.tangem.core.navigation.settings.SettingsManager
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.notifications.GetIsHuaweiDeviceWithoutGoogleServicesUseCase
import com.tangem.domain.notifications.SetShouldShowNotificationUseCase
import com.tangem.domain.notifications.ShouldShowNotificationUseCase
import com.tangem.features.pushnotifications.api.utils.getPushPermissionOrNull
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class JointAccountPushPermissionPromptTest {

    private val systemNotificationsStateProvider: SystemNotificationsStateProvider = mockk()
    private val getIsHuaweiDeviceWithoutGoogleServicesUseCase: GetIsHuaweiDeviceWithoutGoogleServicesUseCase = mockk()
    private val shouldShowNotificationUseCase: ShouldShowNotificationUseCase = mockk()
    private val setShouldShowNotificationUseCase: SetShouldShowNotificationUseCase = mockk(relaxed = true)
    private val settingsManager: SettingsManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private lateinit var prompt: JointAccountPushPermissionPrompt

    @BeforeAll
    fun mockPermissionUtils() {
        mockkStatic(PERMISSION_UTILS)
    }

    @AfterAll
    fun unmockPermissionUtils() {
        unmockkStatic(PERMISSION_UTILS)
    }

    @BeforeEach
    fun setUp() {
        clearMocks(
            systemNotificationsStateProvider,
            getIsHuaweiDeviceWithoutGoogleServicesUseCase,
            shouldShowNotificationUseCase,
            setShouldShowNotificationUseCase,
            settingsManager,
            uiMessageSender,
            analyticsEventHandler,
        )
        every { getPushPermissionOrNull() } returns PERMISSION
        every { systemNotificationsStateProvider.areNotificationsEnabled() } returns false
        coEvery { getIsHuaweiDeviceWithoutGoogleServicesUseCase() } returns false
        coEvery { shouldShowNotificationUseCase(PROMPT_KEY) } returns true
        prompt = JointAccountPushPermissionPrompt(
            systemNotificationsStateProvider = systemNotificationsStateProvider,
            getIsHuaweiDeviceWithoutGoogleServicesUseCase = getIsHuaweiDeviceWithoutGoogleServicesUseCase,
            shouldShowNotificationUseCase = shouldShowNotificationUseCase,
            setShouldShowNotificationUseCase = setShouldShowNotificationUseCase,
            settingsManager = settingsManager,
            uiMessageSender = uiMessageSender,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PromptOnce {

        @Test
        fun `GIVEN notifications enabled WHEN promptOnce THEN nothing shown and prompt kept`() = runTest {
            // Arrange
            every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true

            // Act
            prompt.promptOnce(userWalletId = USER_WALLET_ID, cryptoAccountId = CRYPTO_ACCOUNT_ID)

            // Assert
            assertNothingShown()
            coVerify(exactly = 0) { setShouldShowNotificationUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN device without push services WHEN promptOnce THEN nothing shown and prompt kept`() = runTest {
            // Arrange
            coEvery { getIsHuaweiDeviceWithoutGoogleServicesUseCase() } returns true

            // Act
            prompt.promptOnce(userWalletId = USER_WALLET_ID, cryptoAccountId = CRYPTO_ACCOUNT_ID)

            // Assert
            assertNothingShown()
            coVerify(exactly = 0) { setShouldShowNotificationUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN prompt already shown for the account WHEN promptOnce THEN nothing shown`() = runTest {
            // Arrange
            coEvery { shouldShowNotificationUseCase(PROMPT_KEY) } returns false

            // Act
            prompt.promptOnce(userWalletId = USER_WALLET_ID, cryptoAccountId = CRYPTO_ACCOUNT_ID)

            // Assert
            assertNothingShown()
            coVerify(exactly = 0) { setShouldShowNotificationUseCase(any(), any()) }
        }

        @Test
        fun `GIVEN android with runtime permission WHEN promptOnce THEN system request sent and prompt spent`() =
            runTest {
                // Act
                prompt.promptOnce(userWalletId = USER_WALLET_ID, cryptoAccountId = CRYPTO_ACCOUNT_ID)

                // Assert
                coVerify(exactly = 1) { setShouldShowNotificationUseCase(key = PROMPT_KEY, value = false) }
                prompt.systemRequests.test { awaitItem() }
                verify(exactly = 0) { uiMessageSender.send(any()) }
            }

        @Test
        fun `GIVEN android without runtime permission WHEN promptOnce THEN settings dialog shown and prompt spent`() =
            runTest {
                // Arrange
                every { getPushPermissionOrNull() } returns null

                // Act
                prompt.promptOnce(userWalletId = USER_WALLET_ID, cryptoAccountId = CRYPTO_ACCOUNT_ID)

                // Assert
                coVerify(exactly = 1) { setShouldShowNotificationUseCase(key = PROMPT_KEY, value = false) }
                prompt.systemRequests.test { expectNoEvents() }
                verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnSystemRequestResult {

        @ParameterizedTest
        @EnumSource(PushPermissionResult::class)
        fun `GIVEN any result WHEN onSystemRequestResult THEN Permission Status sent with Joint Account source`(
            result: PushPermissionResult,
        ) = runTest {
            // Arrange
            val expectedState = if (result == PushPermissionResult.Granted) "Allow" else "Cancel"

            // Act
            prompt.onSystemRequestResult(result)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    match<AnalyticsEvent> {
                        it.id == "[Push] Permission Status" &&
                            it.params == mapOf("State" to expectedState, "Source" to "Joint Account")
                    },
                )
            }
        }

        @Test
        fun `GIVEN granted and notifications enabled WHEN onSystemRequestResult THEN no dialog shown`() {
            // Arrange
            every { systemNotificationsStateProvider.areNotificationsEnabled() } returns true

            // Act
            prompt.onSystemRequestResult(PushPermissionResult.Granted)

            // Assert
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN granted but notifications disabled WHEN onSystemRequestResult THEN settings dialog shown`() =
            runTest {
                // Act
                prompt.onSystemRequestResult(PushPermissionResult.Granted)

                // Assert
                verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
            }

        @Test
        fun `GIVEN denied WHEN onSystemRequestResult THEN nothing else happens`() = runTest {
            // Act
            prompt.onSystemRequestResult(PushPermissionResult.Denied)

            // Assert
            verify(exactly = 0) { uiMessageSender.send(any()) }
        }

        @Test
        fun `GIVEN blocked WHEN onSystemRequestResult THEN settings dialog shown`() = runTest {
            // Act
            prompt.onSystemRequestResult(PushPermissionResult.Blocked)

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SettingsDialog {

        @Test
        fun `GIVEN dialog WHEN settings tapped THEN app notification settings opened`() = runTest {
            // Arrange
            val dialog = captureDialog()

            // Act
            dialog.firstAction.onClick()

            // Assert
            verify(exactly = 1) { settingsManager.openAppNotificationSettings() }
        }

        @Test
        fun `GIVEN dialog WHEN enable later tapped THEN settings not opened`() = runTest {
            // Arrange
            val dialog = captureDialog()

            // Act
            requireNotNull(dialog.secondAction).onClick()

            // Assert
            verify(exactly = 0) { settingsManager.openAppNotificationSettings() }
        }

        private fun captureDialog(): DialogMessage {
            val message = slot<UiMessage>()
            every { uiMessageSender.send(capture(message)) } returns Unit
            prompt.onSystemRequestResult(PushPermissionResult.Blocked)
            return message.captured as DialogMessage
        }
    }

    private suspend fun assertNothingShown() {
        prompt.systemRequests.test { expectNoEvents() }
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    private companion object {
        const val PERMISSION_UTILS = "com.tangem.features.pushnotifications.api.utils.PermissionUtilsKt"
        const val PERMISSION = "android.permission.POST_NOTIFICATIONS"
        const val CRYPTO_ACCOUNT_ID = "crypto_account_id"
        const val PROMPT_KEY = "joint_account_push_prompt_011_crypto_account_id"
        val USER_WALLET_ID = UserWalletId("011")
    }
}