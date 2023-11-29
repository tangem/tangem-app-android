package com.tangem.features.send.impl.presentation.state.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.converter.Converter
import java.text.NumberFormat

internal class SendAmountFieldConverter(
    private val clickIntents: SendClickIntents,
) : Converter<Unit, SendTextField.Amount> {

    override fun convert(value: Unit): SendTextField.Amount {
        return SendTextField.Amount(
            value = "",
            fiatValue = DEFAULT_VALUE,
            onValueChange = clickIntents::onAmountValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number,
            ),
            placeholder = TextReference.Str(DEFAULT_VALUE),
            isError = false,
            error = TextReference.Res(R.string.swapping_insufficient_funds),
        )
    }

    companion object {
        private val DEFAULT_VALUE = NumberFormat.getInstance().format(0.00)
    }
}
