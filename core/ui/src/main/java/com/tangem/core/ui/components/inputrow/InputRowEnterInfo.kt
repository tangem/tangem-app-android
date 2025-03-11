package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme

/**
 * Input Row Enter with Info variation.
 * [Input Row Enter](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-799&mode=design&t=IQ5lBJEkFGU4WSvi-4)
 * [Input Row Enter Info](https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?type=design&node-id=7854-33577&mode=design&t=6o23sqF8fDQdn4C5-4)
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
 * @param showDivider show divider
 */
@Composable
fun InputRowEnterInfo(
    title: TextReference,
    text: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    info: TextReference? = null,
    titleColor: Color = TangemTheme.colors.text.secondary,
    textColor: Color = TangemTheme.colors.text.primary1,
    infoColor: Color = TangemTheme.colors.text.tertiary,
    isSingleLine: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    showDivider: Boolean = false,
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
                SimpleTextField(
                    value = text,
                    onValueChange = onValueChange,
                    singleLine = isSingleLine,
                    color = textColor,
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
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

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowEnterInfoPreview(
    @PreviewParameter(InputRowEnterInfoPreviewDataProvider::class) data: InputRowEnterInfoPreviewData,
) {
    TangemThemePreview {
        InputRowEnterInfo(
            title = data.title,
            text = data.text,
            info = data.info,
            showDivider = data.showDivider,
            onValueChange = {},
            modifier = Modifier.background(TangemTheme.colors.background.action),
        )
    }
}

private data class InputRowEnterInfoPreviewData(
    val title: TextReference,
    val text: String,
    val showDivider: Boolean,
    val info: TextReference?,
)

private class InputRowEnterInfoPreviewDataProvider :
    PreviewParameterProvider<InputRowEnterInfoPreviewData> {
    override val values: Sequence<InputRowEnterInfoPreviewData>
        get() = sequenceOf(
            InputRowEnterInfoPreviewData(
                title = TextReference.Str("title"),
                text = "text",
                showDivider = true,
                info = TextReference.Str("info"),
            ),
            InputRowEnterInfoPreviewData(
                title = TextReference.Str("title"),
                text = "text",
                showDivider = false,
                info = null,
            ),
        )
}
//endregion