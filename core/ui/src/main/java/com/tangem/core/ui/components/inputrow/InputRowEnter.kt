package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.tooltip.TangemTooltip
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [InputRowEnter](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-799&mode=design&t=IQ5lBJEkFGU4WSvi-4)
 *
 * @param title title reference
 * @param text primary text reference
 * @param onValueChange text change callback
 * @param modifier modifier
 * @param titleColor title color
 * @param textColor text color
 * @param isSingleLine text
 * @param visualTransformation applied transformation to text
 * @param keyboardOptions keyboard options for field
 * @param iconRes action icon
 * @param iconTint action icon tint
 * @param onIconClick click on action icon
 * @param showDivider show divider
 * @see [InputRowDefault] for read only version
 */
@Composable
fun InputRowEnter(
    title: TextReference,
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: TextReference? = null,
    description: TextReference? = null,
    titleColor: Color = TangemTheme.colors.text.secondary,
    textColor: Color = TangemTheme.colors.text.primary1,
    isSingleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title.resolveReference(),
                        style = TangemTheme.typography.subtitle2,
                        color = titleColor,
                    )
                    if (description != null) {
                        TangemTooltip(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape),
                            text = description.resolveReference(),
                            content = { contentModifier ->
                                Icon(
                                    modifier = contentModifier.size(16.dp),
                                    painter = painterResource(R.drawable.ic_token_info_24),
                                    tint = TangemTheme.colors.icon.informative,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
                SimpleTextField(
                    value = text,
                    onValueChange = onValueChange,
                    placeholder = placeholder,
                    color = textColor,
                    singleLine = isSingleLine,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
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
                            indication = ripple(bounded = false),
                        ) { onIconClick?.invoke() },
                )
            }
        }
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowEnterPreview(
    @PreviewParameter(InputRowEnterPreviewDataProvider::class) data: InputRowEnterPreviewData,
) {
    TangemThemePreview {
        InputRowEnter(
            title = TextReference.Str(data.title),
            text = data.text,
            iconRes = data.iconRes,
            showDivider = data.showDivider,
            description = stringReference(""),
            onValueChange = {},
            modifier = Modifier.background(TangemTheme.colors.background.action),
        )
    }
}

private data class InputRowEnterPreviewData(
    val title: String,
    val text: String,
    val iconRes: Int?,
    val showDivider: Boolean,
)

private class InputRowEnterPreviewDataProvider :
    PreviewParameterProvider<InputRowEnterPreviewData> {
    override val values: Sequence<InputRowEnterPreviewData>
        get() = sequenceOf(
            InputRowEnterPreviewData(
                title = "title",
                text = "text",
                iconRes = null,
                showDivider = true,
            ),
            InputRowEnterPreviewData(
                title = "title",
                text = "text",
                iconRes = R.drawable.ic_chevron_right_24,
                showDivider = false,
            ),
        )
}
//endregion