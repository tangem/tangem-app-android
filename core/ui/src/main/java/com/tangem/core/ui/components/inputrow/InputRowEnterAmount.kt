package com.tangem.core.ui.components.inputrow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.rememberDecimalFormat

/**
 * Input row for entering amount. Manages correct amount format and validation
 *
 * @param title title reference
 * @param text primary text reference
 * @param onValueChange text change callback
 * @param modifier modifier
 * @param titleColor title color
 * @param textColor text color
 * @param keyboardOptions keyboard options for field
 * @param iconRes action icon
 * @param iconTint action icon tint
 * @param onIconClick click on action icon
 * @param showDivider show divider
 *
 * @see [InputRowDefault] for read only version
 * @see <a href=https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-799&mode=design&
 * t=IQ5lBJEkFGU4WSvi-4>InputRowEnter</a>
 */
@Composable
fun InputRowEnterAmount(
    title: TextReference,
    text: String,
    decimals: Int,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    symbol: String? = null,
    titleColor: Color = TangemTheme.colors.text.secondary,
    textColor: Color = TangemTheme.colors.text.primary1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    iconRes: Int? = null,
    iconTint: Color = TangemTheme.colors.icon.informative,
    onIconClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
) {
    DividerContainer(
        modifier = modifier,
        showDivider = showDivider,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = titleColor,
                )
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
                    textStyle = TangemTheme.typography.body2,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing8),
                )
            }
            iconRes?.let {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .padding(
                            top = TangemTheme.dimens.spacing10,
                            bottom = TangemTheme.dimens.spacing10,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                        ) { onIconClick?.invoke() },
                )
            }
        }
    }
}
