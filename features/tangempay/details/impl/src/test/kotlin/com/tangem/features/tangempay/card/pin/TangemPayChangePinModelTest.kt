package com.tangem.features.tangempay.card.pin

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.SetPinResult
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.card.details.TangemPayCardDetailsInnerRoute
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayChangePinModelTest {

    private val userWalletId = UserWalletId("123")
    private val card = tangemPayCard(id = "card_1")

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk()
    private val fetcher: PaymentAccountStatusFetcher = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(router, uiMessageSender, analytics, cardDetailsRepository, fetcher)
        coEvery { fetcher.invoke(any<PaymentAccountStatusFetcher.Params>()) } returns Unit.right()
    }

    @Test
    fun `GIVEN valid pin WHEN set pin succeeds THEN pin is cleared from state`() = runTest {
        // Arrange
        coEvery { cardDetailsRepository.setPin(any(), any(), any()) } returns SetPinResult.SUCCESS.right()
        val model = createModel(testScope = this)

        // Act
        model.uiState.value.onPinCodeChange(VALID_PIN)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.pinCode).isEmpty()
        assertThat(model.uiState.value.isSubmitButtonEnabled).isFalse()
        coVerify(exactly = 1) { cardDetailsRepository.setPin(userWalletId, card.id, VALID_PIN) }
        verify(exactly = 1) { router.push(TangemPayCardDetailsInnerRoute.ChangePINSuccess) }
        model.onDestroy()
    }

    private fun createModel(testScope: TestScope) = TangemPayChangePinModel(
        paramsContainer = MutableParamsContainer(
            TangemPayChangePinComponent.Params(card = card, userWalletId = userWalletId),
        ),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        uiMessageSender = uiMessageSender,
        router = router,
        cardDetailsRepository = cardDetailsRepository,
        analytics = analytics,
        fetcher = fetcher,
    )

    private companion object {
        const val VALID_PIN = "1357"
    }
}