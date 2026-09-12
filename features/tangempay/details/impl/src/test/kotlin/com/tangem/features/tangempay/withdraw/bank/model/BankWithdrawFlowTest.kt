package com.tangem.features.tangempay.withdraw.bank.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.navigation.BankWithdrawEditTarget
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
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
        val actual = BankWithdrawFlow.nextStep(BankWithdrawEntry.NewPersonal, TangemPayBankWithdrawInnerRoute.Amount)

        // Assert
        assertThat(actual).isEqualTo(TangemPayBankWithdrawInnerRoute.TransferType)
    }

    @Test
    fun `GIVEN saved entry WHEN nextStep from preview THEN null`() {
        // Act
        val actual = BankWithdrawFlow.nextStep(BankWithdrawEntry.Saved("acc-1"), TangemPayBankWithdrawInnerRoute.Preview)

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN saved entry WHEN nextStep from step outside flow THEN null`() {
        // Act
        val actual = BankWithdrawFlow.nextStep(
            BankWithdrawEntry.Saved("acc-1"),
            TangemPayBankWithdrawInnerRoute.TransferType,
        )

        // Assert
        assertThat(actual).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideCounterModels")
    fun `GIVEN entry and step WHEN stepCounter THEN counter matches header`(model: CounterModel) {
        // Act
        val actual = BankWithdrawFlow.stepCounter(model.entry, model.step)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN edit route WHEN nextStep THEN null`() {
        // Act
        val actual = BankWithdrawFlow.nextStep(
            entry = BankWithdrawEntry.NewPersonal,
            current = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.Amount),
        )

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN edit route WHEN stepCounter THEN null so the header shows no counter`() {
        // Act
        val actual = BankWithdrawFlow.stepCounter(
            entry = BankWithdrawEntry.NewPersonal,
            step = TangemPayBankWithdrawInnerRoute.Edit(BankWithdrawEditTarget.RecipientDetails),
        )

        // Assert
        assertThat(actual).isNull()
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
    )
}