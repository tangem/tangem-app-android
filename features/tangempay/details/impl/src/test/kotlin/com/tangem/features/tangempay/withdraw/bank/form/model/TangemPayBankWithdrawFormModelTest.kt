package com.tangem.features.tangempay.withdraw.bank.form.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.form.TangemPayBankWithdrawFormComponent
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawDraft
import com.tangem.features.tangempay.withdraw.bank.model.BankWithdrawHeaderUM
import com.tangem.features.tangempay.withdraw.bank.model.NewRecipientDraft
import com.tangem.features.tangempay.withdraw.bank.navigation.TangemPayBankWithdrawInnerRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TangemPayBankWithdrawFormModelTest {

    private val router: Router = mockk(relaxed = true)
    private val onBack: (Map<BankWithdrawFormField, String>) -> Unit = mockk(relaxed = true)
    private val onContinue: (Map<BankWithdrawFormField, String>) -> Unit = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val draft = MutableStateFlow(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))

    private val fields = persistentListOf(
        BankWithdrawFormField.AccountNumber,
        BankWithdrawFormField.RoutingNumber,
        BankWithdrawFormField.FirstName,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, onBack, onContinue)
        draft.value = BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal)
    }

    @Test
    fun `GIVEN empty draft WHEN model created THEN empty fields header 4 of 5 and continue disabled`() {
        // Act
        val model = createModel()

        // Assert
        val state = model.uiState.value
        assertThat(state.header).isEqualTo(
            BankWithdrawHeaderUM(
                title = resourceReference(R.string.tangempay_withdraw_hub_to_personal_account),
                subtitle = resourceReference(R.string.tangempay_withdraw_bank_step_counter, wrappedList(4, 5)),
            ),
        )
        assertThat(state.fields.map { it.field }).containsExactlyElementsIn(fields).inOrder()
        assertThat(state.fields.map { it.value }).containsExactly("", "", "")
        assertThat(state.fields.map { it.error }).containsExactly(null, null, null)
        assertThat(state.isContinueEnabled).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft with saved values WHEN model created THEN fields prefilled`() {
        // Arrange
        draft.value = BankWithdrawDraft(
            entry = BankWithdrawEntry.NewPersonal,
            newRecipient = NewRecipientDraft(fields = mapOf(BankWithdrawFormField.FirstName to "Basit")),
        )

        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.fields.map { it.value }).containsExactly("", "", "Basit").inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN all fields filled WHEN values change THEN continue enabled`() {
        // Arrange
        val model = createModel()

        // Act
        fill(model, BankWithdrawFormField.AccountNumber, "4830019271")
        fill(model, BankWithdrawFormField.RoutingNumber, "021000021")
        fill(model, BankWithdrawFormField.FirstName, "Basit")

        // Assert
        assertThat(model.uiState.value.isContinueEnabled).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN invalid value WHEN field loses focus THEN format error shown`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.RoutingNumber, "123")

        // Act
        field(model, BankWithdrawFormField.RoutingNumber).onFocusChange(false)

        // Assert
        assertThat(field(model, BankWithdrawFormField.RoutingNumber).error)
            .isEqualTo(resourceReference(R.string.tangempay_withdraw_bank_field_error_routing_number))
        model.onDestroy()
    }

    @Test
    fun `GIVEN empty value WHEN field loses focus THEN no error`() {
        // Arrange
        val model = createModel()

        // Act
        field(model, BankWithdrawFormField.AccountNumber).onFocusChange(false)

        // Assert
        assertThat(field(model, BankWithdrawFormField.AccountNumber).error).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN field with error WHEN value changes THEN error cleared`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.AccountNumber, "12")
        field(model, BankWithdrawFormField.AccountNumber).onFocusChange(false)

        // Act
        fill(model, BankWithdrawFormField.AccountNumber, "123")

        // Assert
        assertThat(field(model, BankWithdrawFormField.AccountNumber).error).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN blank and invalid fields WHEN continue clicked THEN errors shown and nothing reported`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.AccountNumber, "4830019271")
        fill(model, BankWithdrawFormField.RoutingNumber, "12")

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        assertThat(model.uiState.value.fields.map { it.error }).containsExactly(
            null,
            resourceReference(R.string.tangempay_withdraw_bank_field_error_routing_number),
            resourceReference(R.string.tangempay_order_data_field_required),
        ).inOrder()
        verify(exactly = 0) { onContinue(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN valid fields WHEN continue clicked THEN trimmed values reported`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.AccountNumber, "4830019271")
        fill(model, BankWithdrawFormField.RoutingNumber, "021000021")
        fill(model, BankWithdrawFormField.FirstName, " Basit ")

        // Act
        model.uiState.value.onContinueClick()

        // Assert
        verify(exactly = 1) {
            onContinue(
                mapOf(
                    BankWithdrawFormField.AccountNumber to "4830019271",
                    BankWithdrawFormField.RoutingNumber to "021000021",
                    BankWithdrawFormField.FirstName to "Basit",
                ),
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN fields WHEN model created THEN ime action next except last done`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.uiState.value.fields.map { it.imeAction.toString() })
            .containsExactly("Next", "Next", "Done").inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN typed values WHEN back clicked THEN values reported and router pops`() {
        // Arrange
        val model = createModel()
        fill(model, BankWithdrawFormField.FirstName, " Basit ")

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) {
            onBack(
                mapOf(
                    BankWithdrawFormField.AccountNumber to "",
                    BankWithdrawFormField.RoutingNumber to "",
                    BankWithdrawFormField.FirstName to "Basit",
                ),
            )
        }
        verify(exactly = 1) { router.pop() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN region error under US WHEN country changes THEN region error cleared`() {
        // Arrange
        val model = createModel(
            formFields = persistentListOf(BankWithdrawFormField.Country, BankWithdrawFormField.Region),
        )
        fill(model, BankWithdrawFormField.Country, "US")
        fill(model, BankWithdrawFormField.Region, "California")
        field(model, BankWithdrawFormField.Region).onFocusChange(false)
        assertThat(field(model, BankWithdrawFormField.Region).error).isNotNull()

        // Act
        fill(model, BankWithdrawFormField.Country, "GB")

        // Assert
        assertThat(field(model, BankWithdrawFormField.Region).error).isNull()
        model.onDestroy()
    }

    private fun fill(model: TangemPayBankWithdrawFormModel, field: BankWithdrawFormField, value: String) {
        field(model, field).onValueChange(value)
    }

    private fun field(model: TangemPayBankWithdrawFormModel, field: BankWithdrawFormField) =
        model.uiState.value.fields.first { it.field == field }

    private fun createModel(
        formFields: ImmutableList<BankWithdrawFormField> = fields,
        step: TangemPayBankWithdrawInnerRoute = TangemPayBankWithdrawInnerRoute.RecipientDetails,
    ): TangemPayBankWithdrawFormModel = TangemPayBankWithdrawFormModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawFormComponent.Params(
                draft = draft,
                step = step,
                fields = formFields,
                onBack = onBack,
                onContinue = onContinue,
            ),
        ),
        dispatchers = dispatchers,
        router = router,
    )
}