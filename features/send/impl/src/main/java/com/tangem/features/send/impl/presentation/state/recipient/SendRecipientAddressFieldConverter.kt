package com.tangem.features.send.impl.presentation.state.recipient

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.MutableStateFlow

internal class SendRecipientAddressFieldConverter(
    private val clickIntents: SendClickIntents,
) : Converter<Unit, MutableStateFlow<SendTextField.RecipientAddress>> {

    override fun convert(value: Unit): MutableStateFlow<SendTextField.RecipientAddress> {
        return MutableStateFlow(
            SendTextField.RecipientAddress(
                value = "",
                onValueChange = clickIntents::onRecipientAddressValueChange,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text,
                ),
                placeholder = TextReference.Res(R.string.send_enter_address_field),
                label = TextReference.Res(R.string.send_recipient),
            ),
        )
    }
}
