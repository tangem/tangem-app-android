package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.inputrow.inner.CrossIcon
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.inputrow.inner.PasteButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.coroutines.delay

/**
 * [Input Row Recipient](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-826&mode=design&t=IQ5lBJEkFGU4WSvi-4)
 *
 * @param title title reference
 * @param value recipient address
 * @param placeholder placeholder
 * @param onValueChange callback for value change
 * @param onPasteClick callback for paste
 * @param modifier composable modifier
 * @param singleLine is single line text
 * @param error error text
 * @param isError is error flag
 * @param showDivider show divider
 *
 * @see InputRowRecipientDefault for readonly version
 */
@Composable
fun InputRowRecipient(
    title: TextReference,
    value: String,
    placeholder: TextReference,
    onValueChange: (String) -> Unit,
    onPasteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    error: TextReference? = null,
    isError: Boolean = false,
    showDivider: Boolean = false,
    isLoading: Boolean = false,
    isValuePasted: Boolean = false,
) {
    val (titleText, color) = if (isError && error != null) {
        error to TangemTheme.colors.text.warning
    } else {
        title to TangemTheme.colors.text.secondary
    }
    DividerContainer(
        modifier = modifier,
        showDivider = showDivider,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
        ) {
            AnimatedContent(targetState = titleText, label = "Title Change") {
                Text(
                    text = it.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = color,
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing8),
            ) {
                Row(
                    verticalAlignment = CenterVertically,
                    modifier = Modifier.heightIn(TangemTheme.dimens.size40),
                ) {
                    InputIcon(
                        isLoading = isLoading,
                        value = value,
                    )
                    SimpleTextField(
                        value = value,
                        placeholder = placeholder,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        isValuePasted = isValuePasted,
                        modifier = Modifier
                            .padding(start = TangemTheme.dimens.spacing12)
                            .weight(1f)
                            .align(CenterVertically),
                    )
                    CrossIcon(
                        onClick = onPasteClick,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(start = TangemTheme.dimens.spacing8),
                    )
                }
                PasteButton(
                    isPasteButtonVisible = value.isBlank(),
                    onClick = onPasteClick,
                    modifier = Modifier
                        .align(CenterEnd)
                        .padding(start = TangemTheme.dimens.spacing8),
                )
            }
        }
    }
}

@Composable
private fun RowScope.InputIcon(isLoading: Boolean, value: String) {
    var isLoadingProxy by remember { mutableStateOf(isLoading) }

    // Do not show the progress indicator, which will disappear quickly
    LaunchedEffect(key1 = isLoading) {
        if (isLoading) {
            delay(timeMillis = 500)
        }
        isLoadingProxy = isLoading
    }

    AnimatedContent(
        targetState = isLoadingProxy,
        label = "Indicator Show Change",
        modifier = Modifier
            .align(CenterVertically)
            .clip(RoundedCornerShape(TangemTheme.dimens.radius18))
            .size(TangemTheme.dimens.size36)
            .background(TangemTheme.colors.background.tertiary),
    ) { showIndicator ->
        if (showIndicator) {
            CircularProgressIndicator(
                color = TangemTheme.colors.icon.informative,
                modifier = Modifier
                    .padding(TangemTheme.dimens.spacing8),
            )
        } else {
            IdentIcon(address = value)
        }
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowRecipientPreview(
    @PreviewParameter(InputRowRecipientPreviewDataProvider::class) value: InputRowRecipientPreviewData,
) {
    TangemThemePreview {
        InputRowRecipient(
            value = value.value,
            title = TextReference.Res(R.string.send_recipient),
            placeholder = TextReference.Res(R.string.send_optional_field),
            error = TextReference.Str("Error"),
            isError = value.isError,
            isLoading = value.isLoading,
            showDivider = true,
            onValueChange = {},
            onPasteClick = {},
            modifier = Modifier.background(TangemTheme.colors.background.primary),
        )
    }
}

private data class InputRowRecipientPreviewData(
    val value: String,
    val isError: Boolean,
    val isLoading: Boolean = false,
)

private class InputRowRecipientPreviewDataProvider : PreviewParameterProvider<InputRowRecipientPreviewData> {
    override val values: Sequence<InputRowRecipientPreviewData>
        get() = sequenceOf(
            InputRowRecipientPreviewData(
                value = "",
                isError = false,
            ),
            InputRowRecipientPreviewData(
                value = "0x391316d97a07027a0702c8A002c8A0C25d8470",
                isError = false,
            ),
            InputRowRecipientPreviewData(
                value = "0x391316d97a07027a0702c8A002c8A0C25d8470",
                isLoading = true,
                isError = true,
            ),
        )
}
//endregion
