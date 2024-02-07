package com.tangem.features.send.impl.presentation.ui.fee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.inputrow.InputRowEnterAmount
import com.tangem.core.ui.components.inputrow.InputRowEnterInfoAmount
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.ui.common.FooterContainer
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun SendCustomFeeEthereum(
    customValues: ImmutableList<SendTextField.CustomFee>,
    selectedFee: FeeType,
    modifier: Modifier = Modifier,
) {
    if (selectedFee == FeeType.CUSTOM && customValues.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
            modifier = modifier,
        ) {
            repeat(customValues.size) { index ->
                val value = customValues[index]
                FooterContainer(
                    footer = value.footer.resolveReference(),
                ) {
                    if (value.label != null) {
                        InputRowEnterInfoAmount(
                            text = value.value,
                            decimals = value.decimals,
                            symbol = value.symbol,
                            title = value.title,
                            info = value.label,
                            keyboardOptions = value.keyboardOptions,
                            keyboardActions = value.keyboardActions,
                            onValueChange = value.onValueChange,
                            showDivider = false,
                            modifier = Modifier
                                .background(
                                    color = TangemTheme.colors.background.action,
                                    shape = TangemTheme.shapes.roundedCornersXMedium,
                                ),
                        )
                    } else {
                        InputRowEnterAmount(
                            text = value.value,
                            decimals = value.decimals,
                            title = value.title,
                            symbol = value.symbol,
                            onValueChange = value.onValueChange,
                            keyboardOptions = value.keyboardOptions,
                            keyboardActions = value.keyboardActions,
                            showDivider = false,
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
    }
}
