package com.tangem.features.send.impl.presentation.state.recipient

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import com.tangem.utils.converter.Converter

internal class SendRecipientAddressFieldConverter(
    private val clickIntents: SendClickIntents,
) : Converter<String, SendTextField.RecipientAddress> {

    override fun convert(value: String): SendTextField.RecipientAddress {
        return SendTextField.RecipientAddress(
            value = value,
            onValueChange = clickIntents::onRecipientAddressValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text,
            ),
            error = resourceReference(R.string.send_recipient_address_error),
            placeholder = resourceReference(R.string.send_enter_address_field),
            label = resourceReference(R.string.send_recipient),
            isValuePasted = false,
        )
    }
}