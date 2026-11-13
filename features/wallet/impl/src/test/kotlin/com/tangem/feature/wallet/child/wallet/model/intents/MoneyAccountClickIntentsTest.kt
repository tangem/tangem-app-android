package com.tangem.feature.wallet.child.wallet.model.intents

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.ModalMessage
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletTangemPayAnalyticsEventSender
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class MoneyAccountClickIntentsTest {

    private val uiMessageSender: UiMessageSender = mockk(relaxUnitFun = true)
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    private val tangemPayAnalyticsEventSender: WalletTangemPayAnalyticsEventSender = mockk(relaxUnitFun = true)
    private val router: InnerWalletRouter = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")

    @BeforeEach
    fun setUp() {
        clearMocks(
            uiMessageSender,
            sendFeedbackEmailUseCase,
            analyticsEventHandler,
            tangemPayAnalyticsEventSender,
            router,
        )
    }

    @Test
    fun `GIVEN failed money account modal WHEN contact support clicked THEN support analytics sent`() = runTest {
        // Arrange
        val message = slot<UiMessage>()
        createImplementor(scope = this).onMoneyAccountFailedClicked(userWalletId = userWalletId, customerId = "id")
        verify(exactly = 1) { uiMessageSender.send(capture(message)) }
        val contactSupportButton = (message.captured as ModalMessage).messageModalUM.buttons.single()
        val event = slot<AnalyticsEvent>()

        // Act
        contactSupportButton.onClick { }
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(capture(event)) }
        assertThat(event.captured.id).isEqualTo("[Basic] Request Support")
        assertThat(event.captured.params).isEqualTo(mapOf("Source" to "Tangem Pay"))
    }

    @Test
    fun `GIVEN failed money account modal WHEN contact support clicked THEN money account support email sent`() =
        runTest {
            // Arrange
            val message = slot<UiMessage>()
            createImplementor(scope = this).onMoneyAccountFailedClicked(userWalletId = userWalletId, customerId = "id")
            verify(exactly = 1) { uiMessageSender.send(capture(message)) }
            val contactSupportButton = (message.captured as ModalMessage).messageModalUM.buttons.single()

            // Act
            contactSupportButton.onClick { }
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) {
                sendFeedbackEmailUseCase(
                    type = FeedbackEmailType.TangemPay.MoneyAccountRequest(
                        walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
                        customerId = "id",
                    ),
                )
            }
        }

    @Test
    fun `GIVEN failed money account WHEN clicked THEN support analytics not sent until button clicked`() = runTest {
        // Act
        createImplementor(scope = this).onMoneyAccountFailedClicked(userWalletId = userWalletId, customerId = "id")
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsEventHandler.send(any()) }
    }

    @Test
    fun `GIVEN money account row WHEN clicked THEN account opening reported and details opened`() = runTest {
        // Act
        createImplementor(scope = this).onMoneyAccountClicked(userWalletId = userWalletId)

        // Assert
        verifyOrder {
            tangemPayAnalyticsEventSender.onAccountOpened(userWalletId = userWalletId, accountType = AccountType.MONEY)
            router.openMoneyAccountDetails(userWalletId = userWalletId)
        }
    }

    private fun createImplementor(scope: TestScope): MoneyAccountClickIntentsImplementor {
        val implementor = MoneyAccountClickIntentsImplementor(
            uiMessageSender = uiMessageSender,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            analyticsEventHandler = analyticsEventHandler,
            tangemPayAnalyticsEventSender = tangemPayAnalyticsEventSender,
        )
        implementor.initialize(router = router, coroutineScope = scope)

        return implementor
    }
}