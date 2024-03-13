package com.tangem.features.send.impl.presentation.state.recipient

import androidx.annotation.StringRes
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
) : Converter<SendRecipientMemoFieldConverter.Data, SendTextField.RecipientMemo> {

    fun convertOrNull(memoValue: String?): SendTextField.RecipientMemo? {
        val cryptoCurrency = cryptoCurrencyStatus().currency
        val memo = memoValue ?: ""

        return when (cryptoCurrency.network.id.value) {
            Blockchain.XRP.id -> {
                convert(
                    value = Data(memo = memo, label = R.string.send_destination_tag_field),
                )
            }
            Blockchain.Binance.id,
            Blockchain.TON.id,
            Blockchain.Cosmos.id,
            Blockchain.TerraV1.id,
            Blockchain.TerraV2.id,
            Blockchain.Stellar.id,
            Blockchain.Hedera.id,
            Blockchain.Algorand.id,
            -> {
                convert(
                    value = Data(memo = memo, label = R.string.send_extras_hint_memo),
                )
            }
            else -> null
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
        )
    }

    data class Data(val memo: String, @StringRes val label: Int)
}