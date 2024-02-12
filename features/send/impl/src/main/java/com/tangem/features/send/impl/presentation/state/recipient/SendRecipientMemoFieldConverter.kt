package com.tangem.features.send.impl.presentation.state.recipient

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendRecipientMemoFieldConverter(
    private val clickIntents: SendClickIntents,
    private val cryptoCurrencyStatus: Provider<CryptoCurrencyStatus>,
) : Converter<Int, SendTextField.RecipientMemo> {

    fun convertOrNull(): SendTextField.RecipientMemo? {
        val cryptoCurrency = cryptoCurrencyStatus().currency

        return when (cryptoCurrency.network.id.value) {
            Blockchain.XRP.id -> convert(R.string.send_destination_tag_field)
            Blockchain.Binance.id,
            Blockchain.TON.id,
            Blockchain.Cosmos.id,
            Blockchain.TerraV1.id,
            Blockchain.TerraV2.id,
            Blockchain.Stellar.id,
            -> convert(R.string.send_extras_hint_memo)
            else -> null
        }
    }

    override fun convert(value: Int): SendTextField.RecipientMemo {
        return SendTextField.RecipientMemo(
            value = "",
            onValueChange = clickIntents::onRecipientMemoValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
            placeholder = resourceReference(R.string.send_optional_field),
            label = resourceReference(value),
            error = resourceReference(R.string.send_memo_destination_tag_error),
            disabledText = resourceReference(R.string.send_additional_field_already_included),
            isEnabled = true,
        )
    }
}