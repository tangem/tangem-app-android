package com.tangem.features.send.impl.presentation.state.recipient

import androidx.annotation.StringRes
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendRecipientMemoFieldConverter(
    private val clickIntents: SendClickIntents,
    private val cryptoCurrencyStatus: Provider<CryptoCurrencyStatus>,
) : Converter<SendRecipientMemoFieldConverter.Data, SendTextField.RecipientMemo> {

    fun convertOrNull(memoValue: String?): SendTextField.RecipientMemo? {
        val cryptoCurrency = cryptoCurrencyStatus().currency
        val memo = memoValue ?: ""

        return when (cryptoCurrency.network.transactionExtrasType) {
            Network.TransactionExtrasType.NONE -> null
            Network.TransactionExtrasType.MEMO -> convert(
                value = Data(
                    memo = memo,
                    label = R.string.send_extras_hint_memo,
                ),
            )
            Network.TransactionExtrasType.DESTINATION_TAG -> convert(
                value = Data(
                    memo = memo,
                    label = R.string.send_destination_tag_field,
                ),
            )
        }
    }

    override fun convert(value: Data): SendTextField.RecipientMemo {
        return SendTextField.RecipientMemo(
            value = value.memo,
            onValueChange = clickIntents::onRecipientMemoValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
            placeholder = resourceReference(R.string.send_optional_field),
            label = resourceReference(value.label),
            error = resourceReference(R.string.send_memo_destination_tag_error),
            disabledText = resourceReference(R.string.send_additional_field_already_included),
            isEnabled = true,
            isValuePasted = false,
        )
    }

    data class Data(val memo: String, @StringRes val label: Int)
}