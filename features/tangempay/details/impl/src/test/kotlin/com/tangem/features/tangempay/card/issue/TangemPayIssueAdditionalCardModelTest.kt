package com.tangem.features.tangempay.card.issue

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.IssueAdditionalCardError
import com.tangem.domain.pay.model.IssueAdditionalCardResult
import com.tangem.domain.pay.usecase.IssueAdditionalCardUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.OUTCOME_UNKNOWN_ERRORS
import com.tangem.features.tangempay.payOrder
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Currency

internal class TangemPayIssueAdditionalCardModelTest {

    private val issueAdditionalCard: IssueAdditionalCardUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val listener: TangemPayIssueAdditionalCardComponent.Listener = mockk(relaxed = true)

    private var model: TangemPayIssueAdditionalCardModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(issueAdditionalCard, uiMessageSender, analytics, listener)
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN balance below fee WHEN model created THEN issuing is blocked`() = runTest {
        // Act
        val model = createModel(fiatBalance = BigDecimal.ONE, feeAmount = BigDecimal.TEN)

        // Assert
        assertThat(model.uiState.value.isBalanceInsufficient).isTrue()
        assertThat(model.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `GIVEN issue succeeds WHEN issue clicked THEN listener notified AND loading reset`() = runTest {
        // Arrange
        coEvery { issueAdditionalCard(USER_WALLET_ID, any()) } returns
            IssueAdditionalCardResult.Created(payOrder()).right()
        val model = createModel()

        // Act
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isLoading).isFalse()
        verify(exactly = 1) { listener.onIssueAdditionalCardSucceeded() }
    }

    @Test
    fun `GIVEN generic error WHEN issue clicked THEN error shown AND sheet dismissed`() = runTest {
        // Arrange
        coEvery { issueAdditionalCard(USER_WALLET_ID, any()) } returns
            IssueAdditionalCardError.Other(VisaApiError.Unspecified).left()
        val model = createModel()

        // Act
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        verify(exactly = 1) { listener.onIssueAdditionalCardDismissed() }
    }

    @ParameterizedTest
    @MethodSource(OUTCOME_UNKNOWN_ERRORS)
    fun `GIVEN outcome unknown WHEN issue clicked THEN sheet stays open AND loading reset`(
        error: VisaApiError,
    ) = runTest {
        // Arrange
        coEvery { issueAdditionalCard(USER_WALLET_ID, any()) } returns IssueAdditionalCardError.Other(error).left()
        val model = createModel()

        // Act
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isLoading).isFalse()
        verify(exactly = 1) { uiMessageSender.send(any<ToastMessage>()) }
        verify(exactly = 0) { listener.onIssueAdditionalCardDismissed() }
    }

    @Test
    fun `GIVEN insufficient balance error WHEN issue clicked THEN add funds offered AND loading reset`() = runTest {
        // Arrange
        coEvery { issueAdditionalCard(USER_WALLET_ID, any()) } returns
            IssueAdditionalCardError.Other(VisaApiError.CardIssueInsufficientBalance).left()
        val model = createModel()

        // Act
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isBalanceInsufficient).isTrue()
        assertThat(model.uiState.value.isLoading).isFalse()
        verify(exactly = 0) { listener.onIssueAdditionalCardDismissed() }
    }

    @Test
    fun `GIVEN insufficient balance error WHEN issue clicked again THEN a new idempotency key is used`() = runTest {
        // Arrange
        val idempotencyKeys = mutableListOf<String>()
        coEvery { issueAdditionalCard(USER_WALLET_ID, capture(idempotencyKeys)) } returns
            IssueAdditionalCardError.Other(VisaApiError.CardIssueInsufficientBalance).left()
        val model = createModel()
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Assert
        assertThat(idempotencyKeys).hasSize(2)
        assertThat(idempotencyKeys.toSet()).hasSize(2)
    }

    @ParameterizedTest
    @MethodSource(OUTCOME_UNKNOWN_ERRORS)
    fun `GIVEN outcome unknown WHEN issue clicked again THEN the same idempotency key is reused`(
        error: VisaApiError,
    ) = runTest {
        // Arrange
        val idempotencyKeys = mutableListOf<String>()
        coEvery { issueAdditionalCard(USER_WALLET_ID, capture(idempotencyKeys)) } returns
            IssueAdditionalCardError.Other(error).left()
        val model = createModel()
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Assert
        assertThat(idempotencyKeys).hasSize(2)
        assertThat(idempotencyKeys.toSet()).hasSize(1)
    }

    @Test
    fun `GIVEN issue in flight WHEN issue clicked again THEN the second tap is ignored`() = runTest {
        // Arrange
        coEvery { issueAdditionalCard(USER_WALLET_ID, any()) } coAnswers { awaitCancellation() }
        val model = createModel()

        // Act
        model.uiState.value.onIssueClick()
        model.uiState.value.onIssueClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isLoading).isTrue()
        coVerify(exactly = 1) { issueAdditionalCard(USER_WALLET_ID, any()) }
    }

    private fun TestScope.createModel(
        fiatBalance: BigDecimal = BigDecimal.TEN,
        feeAmount: BigDecimal = BigDecimal.ONE,
    ) = TangemPayIssueAdditionalCardModel(
        paramsContainer = MutableParamsContainer(
            TangemPayIssueAdditionalCardComponent.Params(
                userWalletId = USER_WALLET_ID,
                feeAmount = feeAmount,
                feeCurrency = Currency.getInstance("USD"),
                fiatBalance = fiatBalance,
                listener = listener,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        issueAdditionalCard = issueAdditionalCard,
        uiMessageSender = uiMessageSender,
        analytics = analytics,
    ).also { model = it }

    private companion object {
        val USER_WALLET_ID = UserWalletId("1234567890ABCDEF")
    }
}