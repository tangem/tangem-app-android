package com.tangem.features.tangempay.orderCard.impl.model

import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.FieldUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Form

internal enum class OrderFormField(
    val read: (Form) -> FieldUM,
    val write: (Form, FieldUM) -> Form,
    val isRequired: Boolean = true,
    val isContentValid: (String) -> Boolean = OrderFormValidator::isAddressCharsetValid,
) {
    EmbossName(
        read = { it.embossName },
        write = { form, field -> form.copy(embossName = field) },
        isContentValid = OrderFormValidator::isEmbossCharsetValid,
    ),
    FirstName(read = { it.firstName }, write = { form, field -> form.copy(firstName = field) }),
    LastName(read = { it.lastName }, write = { form, field -> form.copy(lastName = field) }),
    Region(read = { it.region }, write = { form, field -> form.copy(region = field) }),
    City(read = { it.city }, write = { form, field -> form.copy(city = field) }),
    Line1(read = { it.addressLine1 }, write = { form, field -> form.copy(addressLine1 = field) }),
    Line2(
        read = { it.addressLine2 },
        write = { form, field -> form.copy(addressLine2 = field) },
        isRequired = false,
    ),
    PostalCode(read = { it.postalCode }, write = { form, field -> form.copy(postalCode = field) }),
}

internal fun Form.updateField(field: OrderFormField, transform: FieldUM.() -> FieldUM): Form =
    field.write(this, field.read(this).transform())

internal fun Form.fieldError(field: OrderFormField): OrderFieldError? {
    val value = field.read(this).value.trim()
    return when {
        value.isEmpty() -> OrderFieldError.Required.takeIf { field.isRequired }
        !field.isContentValid(value) -> OrderFieldError.Invalid
        else -> null
    }
}

internal fun Form.phoneError(): OrderFieldError? = when {
    phone.value.isEmpty() -> OrderFieldError.Required
    !PhoneMaskFormatter.isComplete(phone.value, phoneMask) -> OrderFieldError.Invalid
    else -> null
}

internal fun Form.isFormValid(): Boolean = OrderFormField.entries.all { fieldError(it) == null } && phoneError() == null