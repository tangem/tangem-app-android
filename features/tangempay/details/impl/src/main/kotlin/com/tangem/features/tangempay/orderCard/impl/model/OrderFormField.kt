package com.tangem.features.tangempay.orderCard.impl.model

import com.tangem.features.tangempay.common.TangemPayFormCharset
import com.tangem.features.tangempay.orderCard.impl.ui.state.OrderFieldError
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.FieldUM
import com.tangem.features.tangempay.orderCard.impl.ui.state.TangemPayOrderCardDataScreenUM.Form

private const val EMBOSS_NAME_MAX_LENGTH = 26
private const val RECIPIENT_NAME_MAX_LENGTH = 50
private const val REGION_MAX_LENGTH = 50
private const val CITY_MAX_LENGTH = 50
private const val ADDRESS_LINE_MAX_LENGTH = 100
private const val POSTAL_CODE_MAX_LENGTH = 9

internal enum class OrderFormField(
    val read: (Form) -> FieldUM,
    val write: (Form, FieldUM) -> Form,
    val isRequired: Boolean = true,
    val maxLength: Int = Int.MAX_VALUE,
    val isContentValid: (String) -> Boolean = TangemPayFormCharset::isShippingCharsetValid,
) {
    EmbossName(
        read = { it.embossName },
        write = { form, field -> form.copy(embossName = field) },
        maxLength = EMBOSS_NAME_MAX_LENGTH,
        isContentValid = TangemPayFormCharset::isEmbossCharsetValid,
    ),
    FirstName(
        read = { it.firstName },
        write = { form, field -> form.copy(firstName = field) },
        maxLength = RECIPIENT_NAME_MAX_LENGTH,
        isContentValid = TangemPayFormCharset::isRecipientNameCharsetValid,
    ),
    LastName(
        read = { it.lastName },
        write = { form, field -> form.copy(lastName = field) },
        maxLength = RECIPIENT_NAME_MAX_LENGTH,
        isContentValid = TangemPayFormCharset::isRecipientNameCharsetValid,
    ),
    Region(
        read = { it.region },
        write = { form, field -> form.copy(region = field) },
        maxLength = REGION_MAX_LENGTH,
    ),
    City(
        read = { it.city },
        write = { form, field -> form.copy(city = field) },
        maxLength = CITY_MAX_LENGTH,
    ),
    Line1(
        read = { it.addressLine1 },
        write = { form, field -> form.copy(addressLine1 = field) },
        maxLength = ADDRESS_LINE_MAX_LENGTH,
    ),
    Line2(
        read = { it.addressLine2 },
        write = { form, field -> form.copy(addressLine2 = field) },
        isRequired = false,
        maxLength = ADDRESS_LINE_MAX_LENGTH,
    ),
    PostalCode(
        read = { it.postalCode },
        write = { form, field -> form.copy(postalCode = field) },
        maxLength = POSTAL_CODE_MAX_LENGTH,
        isContentValid = TangemPayFormCharset::isPostalCodeCharsetValid,
    ),
}

internal fun Form.updateField(field: OrderFormField, transform: FieldUM.() -> FieldUM): Form =
    field.write(this, field.read(this).transform())

internal fun Form.fieldError(field: OrderFormField): OrderFieldError? {
    val fieldUM = field.read(this)
    val value = fieldUM.value.trim()
    return when {
        !fieldUM.isEditable -> null
        value.isEmpty() -> OrderFieldError.Required.takeIf { field.isRequired }
        else -> field.charsetError(value)
    }
}

internal fun OrderFormField.charsetError(value: String): OrderFieldError? = when {
    isContentValid(value.trim()) -> null
    TangemPayFormCharset.containsNonLatinLetter(value) -> OrderFieldError.NonLatin
    else -> OrderFieldError.Invalid
}

internal fun Form.phoneError(): OrderFieldError? = when {
    phone.value.isEmpty() -> OrderFieldError.Required
    !PhoneMaskFormatter.isComplete(phone.value, phoneMask) -> OrderFieldError.Invalid
    else -> null
}

internal fun Form.isFormValid(): Boolean = OrderFormField.entries.all { fieldError(it) == null } && phoneError() == null