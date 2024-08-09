package com.tangem.core.ui.components.inputrow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.rememberDecimalFormat

/**
 * `Input Row Enter Info` for entering amount. Manages correct amount format and validation

 * @param title title reference
 * @param text primary text reference
 * @param decimals amount text decimal count
 * @param onValueChange text change callback
 * @param modifier modifier
 * @param symbol amount symbol
 * @param info info text
 * @param titleColor title color
 * @param textColor text color
 * @param infoColor info text color
 * @param keyboardOptions keyboard options for field
 * @param keyboardActions keyboard actions for field
 * @param showDivider show divider
 *
 * @see [InputRowEnterInfo]
 * @see <a href=https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-799&mode
 * =design&t=IQ5lBJEkFGU4WSvi-4>Input Row Enter</a>
 * @see <a href=https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node
 *  * -id=7854-33577&mode=design&t=6o23sqF8fDQdn4C5-4>Input Row Enter Info</a>
 */
@Composable
fun InputRowEnterInfoAmount(
    title: TextReference,
    text: String,
    decimals: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    symbol: String? = null,
    info: TextReference? = null,
    titleColor: Color = TangemTheme.colors.text.secondary,
    textColor: Color = TangemTheme.colors.text.primary1,
    infoColor: Color = TangemTheme.colors.text.tertiary,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    showDivider: Boolean = false,
    isReadOnly: Boolean = false,
) {
    DividerContainer(
        modifier = modifier,
        showDivider = showDivider,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = titleColor,
            )
            Row {
                AmountTextField(
                    value = text,
                    decimals = decimals,
                    visualTransformation = AmountVisualTransformation(
                        decimals = decimals,
                        symbol = symbol,
                        decimalFormat = rememberDecimalFormat(),
                    ),
                    onValueChange = onValueChange,
                    color = textColor,
                    isEnabled = !isReadOnly,
                    textStyle = TangemTheme.typography.body2,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing8)
                        .weight(1f),
                )
                info?.let {
                    Text(
                        text = it.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = infoColor,
                        modifier = Modifier
                            .padding(start = TangemTheme.dimens.spacing8)
                            .align(Alignment.Bottom),
                    )
                }
            }
        }
    }
}