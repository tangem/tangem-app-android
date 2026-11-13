package com.tangem.feature.wallet.child.wallet.model.intents

import com.tangem.domain.tangempay.TangemPayKycAdditionalInfoAnalyticsEvents.AccountType
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletTangemPayAnalyticsEventSender
import com.tangem.test.mock.MockAccounts
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class TangemPayClickIntentsTest {

    private val tangemPayAnalyticsEventSender: WalletTangemPayAnalyticsEventSender = mockk(relaxUnitFun = true)
    private val router: InnerWalletRouter = mockk(relaxed = true)

    @Test
    fun `GIVEN payment account row WHEN clicked THEN account opening reported and details opened`() = runTest {
        // Arrange
        val status = MockAccounts.createPaymentAccountStatus()

        // Act
        createImplementor(scope = this).openDetails(status)

        // Assert
        verifyOrder {
            tangemPayAnalyticsEventSender.onAccountOpened(
                userWalletId = MockAccounts.userWalletId,
                accountType = AccountType.PAYMENT,
            )
            router.openTangemPayDetails(status = status)
        }
    }

    private fun createImplementor(scope: TestScope): TangemPayClickIntentsImplementor {
        val implementor = TangemPayClickIntentsImplementor(
            stateHolder = mockk(),
            onboardingRepository = mockk(),
            renewTangemPaySessionUseCase = mockk(),
            sendFeedbackEmailUseCase = mockk(),
            tangemPayOnboardingRepository = mockk(),
            uiMessageSender = mockk(),
            analyticsEventHandler = mockk(),
            paymentAccountStatusFetcher = mockk(),
            tangemPayAnalyticsEventSender = tangemPayAnalyticsEventSender,
        )
        implementor.initialize(router = router, coroutineScope = scope)

        return implementor
    }
}