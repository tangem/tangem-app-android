package com.tangem.features.marketing.impl.concierge.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.marketing.SubmitDemandSurveyUseCase
import com.tangem.domain.marketing.models.SurveyAnswer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.marketing.api.concierge.ConciergeDemandSurveyComponent
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ConciergeDemandSurveyModelTest {

    private val submitDemandSurveyUseCase: SubmitDemandSurveyUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(submitDemandSurveyUseCase, uiMessageSender, router)
    }

    @Test
    fun `GIVEN new survey WHEN model created THEN nothing selected and submit disabled`() = runTest {
        // Act
        val model = createModel()

        // Assert
        val state = model.uiState.value
        assertThat(state.questions.map { it.key }).containsExactly(
            "goal",
            "transaction_size",
            "frequency",
            "needed_currency",
            "current_solution",
            "funds_owner",
            "business_interest",
        ).inOrder()
        assertThat(state.questions.flatMap { it.options }.none { it.isSelected }).isTrue()
        assertThat(state.isSubmitEnabled).isFalse()
    }

    @Test
    fun `GIVEN three required answers WHEN fourth required answered THEN submit enabled`() = runTest {
        // Arrange
        val model = createModel()
        model.select("goal", "buy")
        model.select("transaction_size", "500k_1m")
        model.select("frequency", "one_off")
        model.select("current_solution", "p2p")
        val isEnabledBefore = model.uiState.value.isSubmitEnabled

        // Act
        model.select("needed_currency", "usd")

        // Assert
        assertThat(isEnabledBefore).isFalse()
        assertThat(model.uiState.value.isSubmitEnabled).isTrue()
    }

    @Test
    fun `GIVEN answered question WHEN another option picked THEN only new option selected`() = runTest {
        // Arrange
        val model = createModel()
        model.select("goal", "buy")

        // Act
        model.select("goal", "sell")

        // Assert
        val selected = model.uiState.value.questions.first { it.key == "goal" }.options.filter { it.isSelected }
        assertThat(selected.map { it.key }).containsExactly("sell")
    }

    @Test
    fun `GIVEN answers WHEN submitted THEN only answered pairs sent in question order`() = runTest {
        // Arrange
        coEvery { submitDemandSurveyUseCase(any(), any()) } returns Unit.right()
        val model = createModel()
        model.select("business_interest", "yes")
        model.answerRequired()

        // Act
        model.uiState.value.onSubmitClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            submitDemandSurveyUseCase(
                userWalletId = USER_WALLET_ID,
                answers = listOf(
                    SurveyAnswer(questionKey = "goal", answerKey = "buy_sell"),
                    SurveyAnswer(questionKey = "transaction_size", answerKey = "1m_plus"),
                    SurveyAnswer(questionKey = "frequency", answerKey = "weekly_or_more"),
                    SurveyAnswer(questionKey = "needed_currency", answerKey = "eur"),
                    SurveyAnswer(questionKey = "business_interest", answerKey = "yes"),
                ),
            )
        }
    }

    @Test
    fun `GIVEN required answers missing WHEN submit clicked THEN nothing sent`() = runTest {
        // Arrange
        val model = createModel()
        model.select("goal", "buy")

        // Act
        model.uiState.value.onSubmitClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { submitDemandSurveyUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN submission succeeds WHEN submitted THEN success step replaces the survey`() = runTest {
        // Arrange
        coEvery { submitDemandSurveyUseCase(any(), any()) } returns Unit.right()
        val model = createModel()
        val stacks = trackStacks(model)
        model.answerRequired()

        // Act
        model.uiState.value.onSubmitClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isSubmitting).isFalse()
        assertThat(stacks).containsExactly(listOf(ConciergeDemandSurveyRoute.Success))
        verify(exactly = 0) { uiMessageSender.send(any()) }
    }

    @Test
    fun `GIVEN survey sent WHEN submit clicked again THEN answers not sent twice`() = runTest {
        // Arrange
        coEvery { submitDemandSurveyUseCase(any(), any()) } returns Unit.right()
        val model = createModel()
        model.answerRequired()
        model.uiState.value.onSubmitClick()
        advanceUntilIdle()

        // Act
        model.uiState.value.onSubmitClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { submitDemandSurveyUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN submission fails WHEN submitted THEN error shown and answers kept for retry`() = runTest {
        // Arrange
        coEvery { submitDemandSurveyUseCase(any(), any()) } returnsMany listOf(IllegalStateException().left(), Unit.right())
        val model = createModel()
        val stacks = trackStacks(model)
        model.answerRequired()
        val questionsBefore = model.uiState.value.questions

        // Act
        model.uiState.value.onSubmitClick()
        advanceUntilIdle()
        val stateAfterError = model.uiState.value
        val stacksAfterError = stacks.toList()
        stateAfterError.onSubmitClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any<DialogMessage>()) }
        assertThat(stateAfterError.isSubmitting).isFalse()
        assertThat(stateAfterError.isSubmitEnabled).isTrue()
        assertThat(stateAfterError.questions).isEqualTo(questionsBefore)
        coVerify(exactly = 2) { submitDemandSurveyUseCase(any(), any()) }
        assertThat(stacksAfterError).isEmpty()
        assertThat(stacks).containsExactly(listOf(ConciergeDemandSurveyRoute.Success))
    }

    @Test
    fun `GIVEN survey WHEN dismissed twice THEN router popped once`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onDismiss()
        model.uiState.value.onDismiss()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN submission in progress WHEN dismissed THEN router not popped`() = runTest {
        // Arrange
        coEvery { submitDemandSurveyUseCase(any(), any()) } coAnswers { awaitCancellation() }
        val model = createModel()
        model.answerRequired()
        model.uiState.value.onSubmitClick()

        // Act
        model.uiState.value.onDismiss()

        // Assert
        assertThat(model.uiState.value.isSubmitting).isTrue()
        verify(exactly = 0) { router.pop() }
        model.onDestroy()
    }

    private fun TestScope.createModel() = ConciergeDemandSurveyModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            ConciergeDemandSurveyComponent.Params(userWalletId = USER_WALLET_ID),
        ),
        router = router,
        submitDemandSurveyUseCase = submitDemandSurveyUseCase,
        uiMessageSender = uiMessageSender,
    )

    private fun trackStacks(model: ConciergeDemandSurveyModel): List<List<ConciergeDemandSurveyRoute>> {
        val stacks = mutableListOf<List<ConciergeDemandSurveyRoute>>()
        model.stackNavigation.subscribe { event -> stacks += event.transformer(listOf(ConciergeDemandSurveyRoute.Survey)) }
        return stacks
    }

    private fun ConciergeDemandSurveyModel.select(questionKey: String, optionKey: String) {
        uiState.value.questions
            .first { it.key == questionKey }
            .options
            .first { it.key == optionKey }
            .onClick()
    }

    private fun ConciergeDemandSurveyModel.answerRequired() {
        select("goal", "buy_sell")
        select("transaction_size", "1m_plus")
        select("frequency", "weekly_or_more")
        select("needed_currency", "eur")
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}