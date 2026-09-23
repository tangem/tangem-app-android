package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankWithdrawFlowTest {

    @ParameterizedTest
    @MethodSource("provideStepsModels")
    fun `GIVEN entry WHEN steps THEN ordered steps returned`(model: StepsModel) {
        // Act
        val actual = BankWithdrawFlow.steps(model.entry)

        // Assert
        assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
        assertThat(BankWithdrawFlow.initialStep(model.entry)).isEqualTo(model.expected.first())
    }

    @Test
    fun `GIVEN new personal entry WHEN nextStep from amount THEN transfer type`() {
        // Act
        val actual = BankWithdrawFlow.nextStep(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
            current = TangemPayBankWithdrawInnerRoute.Amount,
        )

        // Assert
        assertThat(actual).isEqualTo(TangemPayBankWithdrawInnerRoute.TransferType)
    }

    @Test
    fun `GIVEN saved recipient picked mid-flow WHEN nextStep from amount THEN preview`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            recipient = bankAccount(id = "acc-1"),
        )

        // Act
        val actual = BankWithdrawFlow.nextStep(draft = draft, current = TangemPayBankWithdrawInnerRoute.Amount)

        // Assert
        assertThat(actual).isEqualTo(TangemPayBankWithdrawInnerRoute.Preview)
    }

    @Test
    fun `GIVEN see all entry with a picked recipient WHEN nextStep from recipients THEN amount`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.SeeAll,
            recipient = bankAccount(id = "acc-1"),
        )

        // Act
        val actual = BankWithdrawFlow.nextStep(draft = draft, current = TangemPayBankWithdrawInnerRoute.Recipients)

        // Assert
        assertThat(actual).isEqualTo(TangemPayBankWithdrawInnerRoute.Amount)
    }

    @Test
    fun `GIVEN saved entry WHEN nextStep from preview THEN null`() {
        // Act
        val actual = BankWithdrawFlow.nextStep(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1")),
            current = TangemPayBankWithdrawInnerRoute.Preview,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN saved entry WHEN nextStep from step outside flow THEN null`() {
        // Act
        val actual = BankWithdrawFlow.nextStep(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-1")),
            current = TangemPayBankWithdrawInnerRoute.TransferType,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideCounterModels")
    fun `GIVEN entry and step WHEN stepCounter THEN counter matches header`(model: CounterModel) {
        // Arrange
        val draft = BankWithdrawDraft(entry = model.entry)

        // Act
        val actual = BankWithdrawFlow.stepCounter(draft = draft, step = model.step)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN edit route WHEN nextStep THEN null`() {
        // Act
        val actual = BankWithdrawFlow.nextStep(
            draft = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal),
            current = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN new personal draft with a picked recipient WHEN stepCounter for edit THEN null`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            recipient = bankAccount(id = "acc-1"),
        )

        // Act
        val actual = BankWithdrawFlow.stepCounter(
            draft = draft,
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.BankDetails),
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN saved recipient picked mid-flow WHEN stepCounter THEN null`() {
        // Arrange
        val draft = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            recipient = bankAccount(id = "acc-1"),
        )

        // Act
        val actual = BankWithdrawFlow.stepCounter(draft = draft, step = TangemPayBankWithdrawInnerRoute.Amount)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN forms above amount WHEN cut THEN amount kept and preview pushed`() {
        // Arrange
        val stack = listOf(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.TransferType,
            TangemPayBankWithdrawInnerRoute.AccountType,
            TangemPayBankWithdrawInnerRoute.RecipientDetails,
        )

        // Act
        val actual = stack.cutToAmountThenPreview()

        // Assert
        assertThat(actual).containsExactly(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.Preview,
        ).inOrder()
    }

    @Test
    fun `GIVEN edit above preview WHEN cut THEN single preview on top of amount`() {
        // Arrange
        val stack = listOf(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.RecipientDetails,
            TangemPayBankWithdrawInnerRoute.BankDetails,
            TangemPayBankWithdrawInnerRoute.Preview,
            TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.RecipientDetails),
        )

        // Act
        val actual = stack.cutToAmountThenPreview()

        // Assert
        assertThat(actual).containsExactly(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.Preview,
        ).inOrder()
    }

    @Test
    fun `GIVEN no amount in stack WHEN cut THEN amount and preview`() {
        // Arrange
        val stack = listOf(TangemPayBankWithdrawInnerRoute.Recipients)

        // Act
        val actual = stack.cutToAmountThenPreview()

        // Assert
        assertThat(actual).containsExactly(
            TangemPayBankWithdrawInnerRoute.Amount,
            TangemPayBankWithdrawInnerRoute.Preview,
        ).inOrder()
    }

    data class StepsModel(val entry: BankWithdrawEntry, val expected: List<TangemPayBankWithdrawInnerRoute>)

    data class CounterModel(
        val entry: BankWithdrawEntry,
        val step: TangemPayBankWithdrawInnerRoute,
        val expected: BankWithdrawFlow.StepCounter?,
    )

    private fun provideStepsModels() = listOf(
        StepsModel(
            entry = BankWithdrawEntry.NewPersonal,
            expected = listOf(
                TangemPayBankWithdrawInnerRoute.Amount,
                TangemPayBankWithdrawInnerRoute.TransferType,
                TangemPayBankWithdrawInnerRoute.AccountType,
                TangemPayBankWithdrawInnerRoute.RecipientDetails,
                TangemPayBankWithdrawInnerRoute.BankDetails,
                TangemPayBankWithdrawInnerRoute.Preview,
            ),
        ),
        StepsModel(
            entry = BankWithdrawEntry.NewBusiness,
            expected = listOf(
                TangemPayBankWithdrawInnerRoute.Amount,
                TangemPayBankWithdrawInnerRoute.AccountType,
                TangemPayBankWithdrawInnerRoute.RecipientDetails,
                TangemPayBankWithdrawInnerRoute.BankDetails,
                TangemPayBankWithdrawInnerRoute.Preview,
            ),
        ),
        StepsModel(
            entry = BankWithdrawEntry.Saved("acc-1"),
            expected = listOf(TangemPayBankWithdrawInnerRoute.Amount, TangemPayBankWithdrawInnerRoute.Preview),
        ),
        StepsModel(
            entry = BankWithdrawEntry.SeeAll,
            expected = listOf(
                TangemPayBankWithdrawInnerRoute.Recipients,
                TangemPayBankWithdrawInnerRoute.Amount,
                TangemPayBankWithdrawInnerRoute.Preview,
            ),
        ),
    )

    private fun provideCounterModels() = listOf(
        CounterModel(
            entry = BankWithdrawEntry.NewPersonal,
            step = TangemPayBankWithdrawInnerRoute.Amount,
            expected = BankWithdrawFlow.StepCounter(current = 1, total = 5),
        ),
        CounterModel(
            entry = BankWithdrawEntry.NewPersonal,
            step = TangemPayBankWithdrawInnerRoute.BankDetails,
            expected = BankWithdrawFlow.StepCounter(current = 5, total = 5),
        ),
        CounterModel(
            entry = BankWithdrawEntry.NewPersonal,
            step = TangemPayBankWithdrawInnerRoute.AccountType,
            expected = BankWithdrawFlow.StepCounter(current = 3, total = 5),
        ),
        CounterModel(
            entry = BankWithdrawEntry.NewPersonal,
            step = TangemPayBankWithdrawInnerRoute.Preview,
            expected = null,
        ),
        CounterModel(
            entry = BankWithdrawEntry.NewBusiness,
            step = TangemPayBankWithdrawInnerRoute.RecipientDetails,
            expected = BankWithdrawFlow.StepCounter(current = 3, total = 4),
        ),
        CounterModel(
            entry = BankWithdrawEntry.Saved("acc-1"),
            step = TangemPayBankWithdrawInnerRoute.Amount,
            expected = null,
        ),
        CounterModel(
            entry = BankWithdrawEntry.SeeAll,
            step = TangemPayBankWithdrawInnerRoute.Amount,
            expected = null,
        ),
        CounterModel(
            entry = BankWithdrawEntry.NewPersonal,
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.BankDetails),
            expected = BankWithdrawFlow.StepCounter(current = 5, total = 5),
        ),
        CounterModel(
            entry = BankWithdrawEntry.NewPersonal,
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
            expected = BankWithdrawFlow.StepCounter(current = 1, total = 5),
        ),
        CounterModel(
            entry = BankWithdrawEntry.NewBusiness,
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.RecipientDetails),
            expected = BankWithdrawFlow.StepCounter(current = 3, total = 4),
        ),
        CounterModel(
            entry = BankWithdrawEntry.Saved("acc-1"),
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
            expected = null,
        ),
    )
}