package com.tangem.features.send.impl.presentation.state.recipient

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.coroutines.flow.MutableStateFlow

internal class SendRecipientMemoFieldConverter(
    private val clickIntents: SendClickIntents,
    private val cryptoCurrencyStatus: Provider<CryptoCurrencyStatus>,
) : Converter<Int, MutableStateFlow<SendTextField.RecipientMemo>> {

    fun convertOrNull(): MutableStateFlow<SendTextField.RecipientMemo>? {
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

    override fun convert(value: Int): MutableStateFlow<SendTextField.RecipientMemo> {
        return MutableStateFlow(
            SendTextField.RecipientMemo(
                value = "",
                onValueChange = clickIntents::onRecipientMemoValueChange,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                ),
                placeholder = TextReference.Res(R.string.send_optional_field),
                label = TextReference.Res(value),
                error = TextReference.Res(R.string.send_memo_destination_tag_error),
            ),
        )
    }
}