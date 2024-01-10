package com.tangem.features.send.impl.presentation.state.fee

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class SendFeeCustomFieldConverter(
    private val clickIntents: SendClickIntents,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<Fee, ImmutableList<SendTextField.CustomFee>> {

    override fun convert(value: Fee): ImmutableList<SendTextField.CustomFee> {
        val ethereumFee = value as? Fee.Ethereum ?: return persistentListOf()
        val appCurrency = appCurrencyProvider()

        val maxFeeFiat = BigDecimalFormatter.formatFiatAmount(
            fiatAmount = ethereumFee.amount.value,
            fiatCurrencyCode = appCurrency.code,
            fiatCurrencySymbol = appCurrency.symbol,
        )

        return persistentListOf(
            SendTextField.CustomFee(
                value = ethereumFee.amount.value.toString(),
                onValueChange = { clickIntents.onCustomFeeValueChange(0, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
                label = TextReference.Str(maxFeeFiat),
            ),
            SendTextField.CustomFee(
                value = ethereumFee.gasPrice.toString(),
                onValueChange = { clickIntents.onCustomFeeValueChange(1, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Number,
                ),
            ),
            SendTextField.CustomFee(
                value = ethereumFee.gasLimit.toString(),
                onValueChange = { clickIntents.onCustomFeeValueChange(2, it) },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }
}