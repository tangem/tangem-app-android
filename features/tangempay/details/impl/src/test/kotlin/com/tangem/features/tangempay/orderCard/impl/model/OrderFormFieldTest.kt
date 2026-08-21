package com.tangem.features.tangempay.orderCard.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError.NonLatin
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError.Required
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Form
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderFormFieldTest {

    @Test
    fun `GIVEN the wiring table WHEN compared with the enum THEN every field is covered`() {
        // Act
        val covered = fieldCases().map { it.field }

        // Assert
        assertThat(covered).containsExactlyElementsIn(OrderFormField.entries)
    }

    @ParameterizedTest
    @MethodSource("fieldCases")
    fun fieldWiring(expected: FieldModel) {
        // Arrange
        val form = emptyForm()

        // Act
        val actual = FieldModel(
            field = expected.field,
            isRequired = expected.field.isRequired,
            blankError = form.withValue(expected.field, "").fieldError(expected.field),
            accentedError = form.withValue(expected.field, "José").fieldError(expected.field),
        )

        // Assert
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a blank read-only field WHEN validated THEN the form stays valid`() {
        // Arrange
        val form = validForm().updateField(OrderFormField.EmbossName) { copy(value = "", isEditable = false) }

        // Act
        val embossError = form.fieldError(OrderFormField.EmbossName)

        // Assert
        assertThat(embossError).isNull()
        assertThat(form.isFormValid()).isTrue()
    }

    internal data class FieldModel(
        val field: OrderFormField,
        val isRequired: Boolean,
        val blankError: OrderFieldError?,
        val accentedError: OrderFieldError?,
    )

    private fun Form.withValue(field: OrderFormField, value: String) = updateField(field) { copy(value = value) }

    private fun validForm(): Form = OrderFormField.entries
        .fold(emptyForm()) { form, field -> form.withValue(field, "Night City") }
        .let { form -> form.copy(phone = form.phone.copy(value = "2345678901"), phoneMask = "##########") }

    private fun emptyForm() = Form(
        onBackClick = {},
        onCloseClick = {},
        country = "US",
        email = "j.silverhand@gmail.com",
        phoneMask = "",
        embossName = emptyField(),
        firstName = emptyField(),
        lastName = emptyField(),
        region = emptyField(),
        city = emptyField(),
        addressLine1 = emptyField(),
        addressLine2 = emptyField(),
        postalCode = emptyField(),
        phone = emptyField(),
        isOrderEnabled = false,
        isSubmitting = false,
        onOrderClick = {},
    )

    private fun emptyField() = TangemPayOrderCardDataScreenUM.FieldUM(
        value = "",
        error = null,
        isRequired = true,
        onValueChange = {},
        onFocusChange = {},
    )

    private fun case(
        field: OrderFormField,
        isRequired: Boolean = true,
        blankError: OrderFieldError? = Required,
        accentedError: OrderFieldError? = null,
    ) = FieldModel(field, isRequired, blankError, accentedError)

    private fun fieldCases() = listOf(
        case(OrderFormField.EmbossName, accentedError = NonLatin),
        case(OrderFormField.FirstName),
        case(OrderFormField.LastName),
        case(OrderFormField.Region),
        case(OrderFormField.City),
        case(OrderFormField.Line1),
        case(OrderFormField.Line2, isRequired = false, blankError = null),
        case(OrderFormField.PostalCode),
    )
}