package com.tangem.features.send.impl.presentation.ui.fee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.components.inputrow.InputRowEnter
import com.tangem.core.ui.components.inputrow.InputRowEnterInfo
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.ui.common.FooterContainer
import kotlinx.collections.immutable.ImmutableList

private const val ETHEREUM_UNIT = "GWEI"

@Composable
internal fun SendCustomFeeEthereum(
    customValues: ImmutableList<SendTextField.CustomFee>,
    selectedFee: FeeType,
    symbol: String,
    modifier: Modifier = Modifier,
) {
    if (selectedFee == FeeType.CUSTOM && customValues.isNotEmpty()) {
        val fee = customValues[0]
        val gasPrice = customValues[1]
        val gasLimit = customValues[2]

        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            modifier = modifier,
        ) {
            FooterContainer(
                footer = stringResource(R.string.send_max_fee_footer),
            ) {
                InputRowEnterInfo(
                    text = fee.value,
                    title = TextReference.Res(R.string.send_max_fee),
                    info = fee.label,
                    visualTransformation = AmountVisualTransformation(symbol),
                    keyboardOptions = fee.keyboardOptions,
                    onValueChange = fee.onValueChange,
                    isSingleLine = true,
                    modifier = Modifier
                        .background(
                            color = TangemTheme.colors.background.action,
                            shape = TangemTheme.shapes.roundedCornersXMedium,
                        ),
                )
            }
            FooterContainer(
                footer = stringResource(R.string.send_gas_price_footer),
            ) {
                InputRowEnter(
                    text = gasPrice.value,
                    title = TextReference.Res(R.string.send_gas_price),
                    onValueChange = gasPrice.onValueChange,
                    visualTransformation = AmountVisualTransformation(ETHEREUM_UNIT),
                    keyboardOptions = fee.keyboardOptions,
                    isSingleLine = true,
                    modifier = Modifier
                        .background(
                            color = TangemTheme.colors.background.action,
                            shape = TangemTheme.shapes.roundedCornersXMedium,
                        ),
                )
            }
            FooterContainer(
                footer = stringResource(R.string.send_gas_limit_footer),
            ) {
                InputRowEnter(
                    text = gasLimit.value,
                    title = TextReference.Res(R.string.send_gas_limit),
                    onValueChange = gasLimit.onValueChange,
                    keyboardOptions = fee.keyboardOptions,
                    isSingleLine = true,
                    modifier = Modifier
                        .background(
                            color = TangemTheme.colors.background.action,
                            shape = TangemTheme.shapes.roundedCornersXMedium,
                        ),
                )
            }
        }
    }
}