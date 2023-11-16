package com.tangem.features.send.impl.presentation.ui.fee

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.fields.AmountVisualTransformation
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.ui.recipient.TextFieldWithInfo

private const val ETHEREUM_UNIT = "GWEI"

@Composable
internal fun SendCustomFeeEthereum(
    customValues: State<List<SendTextField.CustomFee>>,
    selectedFee: FeeType,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    val fee = customValues.value[0]
    val gasPrice = customValues.value[1]
    val gasLimit = customValues.value[2]

    if (selectedFee == FeeType.CUSTOM && customValues.value.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            modifier = modifier,
        ) {
            TextFieldWithInfo(
                value = fee.value,
                label = stringResource(R.string.send_max_fee),
                footer = stringResource(R.string.send_max_fee_footer),
                info = fee.label,
                visualTransformation = AmountVisualTransformation(symbol),
                keyboardOptions = fee.keyboardOptions,
                onValueChange = fee.onValueChange,
                isSingleLine = true,
            )
            TextFieldWithInfo(
                value = gasPrice.value,
                label = stringResource(R.string.send_gas_price),
                footer = stringResource(R.string.send_gas_price_footer),
                onValueChange = gasPrice.onValueChange,
                visualTransformation = AmountVisualTransformation(ETHEREUM_UNIT),
                keyboardOptions = fee.keyboardOptions,
                isSingleLine = true,
            )
            TextFieldWithInfo(
                value = gasLimit.value,
                label = stringResource(R.string.send_gas_limit),
                footer = stringResource(R.string.send_gas_limit_footer),
                onValueChange = gasLimit.onValueChange,
                keyboardOptions = fee.keyboardOptions,
                isSingleLine = true,
            )
        }
    }
}
