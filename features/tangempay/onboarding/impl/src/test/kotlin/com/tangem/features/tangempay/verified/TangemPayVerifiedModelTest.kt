package com.tangem.features.tangempay.verified

import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.usecase.GetTangemPayProductsAvailabilityUseCase
import com.tangem.domain.moneyaccount.usecase.StartTangemPayOnboardingUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.components.TangemPayVerifiedComponent
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayVerifiedModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val getProductsAvailabilityUseCase: GetTangemPayProductsAvailabilityUseCase = mockk()
    private val startTangemPayOnboardingUseCase: StartTangemPayOnboardingUseCase = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, uiMessageSender, getProductsAvailabilityUseCase, startTangemPayOnboardingUseCase, analytics)
    }

    @Test
    fun `GIVEN products loaded WHEN model created THEN verification result event sent with them`() = runTest {
        // Arrange
        val products = setOf(TangemPayProduct.PAYMENT_ACCOUNT, TangemPayProduct.BANKING_ACCOUNT)
        coEvery { getProductsAvailabilityUseCase(USER_WALLET_ID) } returns products.right()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.VerificationResultReceived(availableProducts = products))
        }
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.EligibilityRetryScreenOpened>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN availability fails WHEN model created THEN after verification retry event sent`() = runTest {
        // Arrange
        coEvery { getProductsAvailabilityUseCase(USER_WALLET_ID) } returns TangemPayApiError.ServerUnavailable.left()

        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analytics.send(
                TangemPayAnalyticsEvents.EligibilityRetryScreenOpened(
                    stage = TangemPayAnalyticsEvents.EligibilityStage.AFTER_VERIFICATION,
                ),
            )
        }
        verify(exactly = 0) { analytics.send(ofType<TangemPayAnalyticsEvents.VerificationResultReceived>()) }
        model.onDestroy()
    }

    private fun TestScope.createModel() = TangemPayVerifiedModel(
        paramsContainer = MutableParamsContainer(TangemPayVerifiedComponent.Params(userWalletId = USER_WALLET_ID)),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        router = router,
        uiMessageSender = uiMessageSender,
        getTangemPayProductsAvailabilityUseCase = getProductsAvailabilityUseCase,
        startTangemPayOnboardingUseCase = startTangemPayOnboardingUseCase,
        analytics = analytics,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}