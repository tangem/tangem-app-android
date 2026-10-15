package com.tangem.tap.common.pushes

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.feature.usedesk.api.SupportChatVisibilityProvider
import com.tangem.feature.usedesk.api.UseDeskFeatureToggles
import com.tangem.test.core.ProvideTestModels
import io.customer.messagingpush.CustomerIOFirebaseMessagingService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PushMessageDispatcherTest {

    private val context: Context = mockk()
    private val pushMessageHandler: PushMessageHandler = mockk(relaxed = true)
    private val pushNotificationDelegate: PushNotificationDelegate = mockk(relaxed = true)
    private val supportChatVisibilityProvider: SupportChatVisibilityProvider = mockk()
    private val useDeskFeatureToggles: UseDeskFeatureToggles = mockk()
    private val message: RemoteMessage = mockk()
    private val notification: RemoteMessage.Notification = mockk()

    private val dispatcher = PushMessageDispatcher(
        context = context,
        pushMessageHandler = pushMessageHandler,
        pushNotificationDelegate = pushNotificationDelegate,
        supportChatVisibilityProvider = supportChatVisibilityProvider,
        useDeskFeatureToggles = useDeskFeatureToggles,
    )

    @BeforeAll
    fun mockCustomerIo() {
        mockkObject(CustomerIOFirebaseMessagingService.Companion)
        every {
            CustomerIOFirebaseMessagingService.onMessageReceived(
                context = any(),
                remoteMessage = any(),
                handleNotificationTrigger = any(),
            )
        } returns true
    }

    @AfterAll
    fun unmockCustomerIo() {
        unmockkObject(CustomerIOFirebaseMessagingService.Companion)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            context,
            pushMessageHandler,
            pushNotificationDelegate,
            supportChatVisibilityProvider,
            useDeskFeatureToggles,
            message,
            notification,
        )
        clearMocks(CustomerIOFirebaseMessagingService.Companion, answers = false)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN push type, toggle and chat visibility WHEN dispatch THEN only support push with visible chat is muted`(
        model: Model,
    ) {
        // Arrange
        val data = buildMap {
            model.type?.let { put(TYPE_KEY, it) }
            put("title", "Support")
            put("body", "New reply")
        }
        every { useDeskFeatureToggles.isSupportChatDeepLinkEnabled } returns model.isToggleEnabled
        every { supportChatVisibilityProvider.isChatVisible } returns model.isChatVisible
        every { message.data } returns data
        every { message.priority } returns RemoteMessage.PRIORITY_HIGH
        every { message.notification } returns notification.takeIf { model.hasNotificationBlock }
        every { notification.title } returns "Support"
        every { notification.body } returns "New reply"
        every { notification.channelId } returns null
        every { notification.imageUrl } returns null
        every { notification.vibrateTimings } returns null

        // Act
        dispatcher.dispatch(message)

        // Assert
        verify(exactly = 1) {
            CustomerIOFirebaseMessagingService.onMessageReceived(
                context = context,
                remoteMessage = message,
                handleNotificationTrigger = false,
            )
        }
        verify(exactly = 1) { pushMessageHandler.onMessageReceived(data) }
        verify(exactly = model.expectedShowCount) {
            pushNotificationDelegate.showNotification(
                dataMap = data,
                title = "Support",
                body = "New reply",
                channelId = "Tangem General",
                priority = RemoteMessage.PRIORITY_HIGH,
                imageUrl = null,
                vibratePattern = null,
            )
        }
        verify(exactly = model.expectedShowCount) {
            pushNotificationDelegate.showNotification(
                dataMap = any(),
                title = any(),
                body = any(),
                channelId = any(),
                priority = any(),
                imageUrl = any(),
                vibratePattern = any(),
            )
        }
    }

    data class Model(
        val type: String?,
        val isToggleEnabled: Boolean,
        val isChatVisible: Boolean,
        val hasNotificationBlock: Boolean,
        val expectedShowCount: Int,
    )

    private fun provideTestModels(): List<Model> = listOf(SUPPORT_MESSAGE_TYPE, OTHER_TYPE, null).flatMap { type ->
        listOf(true, false).flatMap { isToggleEnabled ->
            listOf(true, false).flatMap { isChatVisible ->
                listOf(false, true).map { hasNotificationBlock ->
                    val isMuted = isToggleEnabled && type == SUPPORT_MESSAGE_TYPE && isChatVisible
                    Model(
                        type = type,
                        isToggleEnabled = isToggleEnabled,
                        isChatVisible = isChatVisible,
                        hasNotificationBlock = hasNotificationBlock,
                        expectedShowCount = if (isMuted) 0 else 1,
                    )
                }
            }
        }
    }

    private companion object {
        const val SUPPORT_MESSAGE_TYPE = "support_message"
        const val OTHER_TYPE = "promo"
    }
}