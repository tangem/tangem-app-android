package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.inputrow.inner.CrossIcon
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.components.inputrow.inner.PasteButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendAddressScreenTestTags
import com.tangem.core.ui.utils.DEFAULT_ANIMATION_DURATION
import kotlinx.coroutines.delay

/**
 * * [Design Reference](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-826&mode=design&t=IQ5lBJEkFGU4WSvi-4)
 *
 * @param title The title text reference to display above the input field
 * @param value The current recipient address value to display in the input field
 * @param placeholder The placeholder text reference to show when the input is empty
 * @param onValueChange Callback invoked when the input value changes, receives the new string value
 * @param onPasteClick Callback invoked when the paste button is clicked, receives the pasted string
 * @param onQrCodeClick Callback invoked when the QR code scan button is clicked
 * @param modifier Optional modifier to apply to the composable
 * @param singleLine Whether the text field should be constrained to a single line (default: false)
 * @param error Optional error text reference to display when validation fails
 * @param isError Whether the input is in an error state, affects styling and title color
 * @param showDivider Whether to show a divider below the component (default: false)
 * @param isLoading Whether to show a loading indicator instead of the identicon (default: false)
 * @param isValuePasted Whether the current value was pasted, affects visual feedback (default: false)
 *
 * @see InputRowRecipientDefault for a read-only version of this component
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun InputRowRecipient(
    title: TextReference,
    value: String,
    placeholder: TextReference,
    onValueChange: (String) -> Unit,
    onPasteClick: (String) -> Unit,
    onQrCodeClick: () -> Unit,
    isRedesignEnabled: Boolean,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    error: TextReference? = null,
    isError: Boolean = false,
    showDivider: Boolean = false,
    isLoading: Boolean = false,
    isValuePasted: Boolean = false,
    resolvedAddress: String? = null,
) {
    val (titleText, color) = if (isError && error != null) {
        error to TangemTheme.colors.text.warning
    } else {
        title to TangemTheme.colors.text.tertiary
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
                    modifier = Modifier.testTag(SendAddressScreenTestTags.ADDRESS_TEXT_FIELD_TITLE),
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
                            .align(CenterVertically)
                            .testTag(SendAddressScreenTestTags.ADDRESS_TEXT_FIELD),
                    )
                    CrossIcon(
                        onClick = onPasteClick,
                        modifier = Modifier
                            .align(CenterVertically)
                            .padding(start = TangemTheme.dimens.spacing8),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(CenterEnd),
                ) {
                    if (isRedesignEnabled) {
                        QrButton(
                            visible = value.isBlank(),
                            onQrCodeClick = onQrCodeClick,
                        )
                    }
                    PasteButton(
                        isPasteButtonVisible = value.isBlank(),
                        onClick = onPasteClick,
                        backgroundColorEnabled = if (isRedesignEnabled) {
                            TangemTheme.colors.button.secondary
                        } else {
                            TangemTheme.colors.button.primary
                        },
                        textColor = if (isRedesignEnabled) {
                            TangemTheme.colors.text.primary1
                        } else {
                            TangemTheme.colors.text.primary2
                        },
                        modifier = Modifier.padding(start = TangemTheme.dimens.spacing8),
                    )
                }
            }

            ResolvedAddressRow(
                isLoading = isLoading,
                resolvedAddress = resolvedAddress,
            )
        }
    }
}

@Composable
private fun QrButton(visible: Boolean, onQrCodeClick: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(DEFAULT_ANIMATION_DURATION)),
        modifier = modifier,
    ) {
        TangemIconButton(
            iconRes = R.drawable.ic_scan_16,
            onClick = onQrCodeClick,
            background = TangemTheme.colors.button.secondary,
            iconTint = TangemTheme.colors.text.primary1,
        )
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

@Composable
private fun ResolvedAddressRow(isLoading: Boolean, resolvedAddress: String?) {
    AnimatedContent(
        targetState = if (resolvedAddress.isNullOrBlank() || isLoading) {
            ResolvedState.Hide
        } else {
            ResolvedState.Show(resolvedAddress)
        },
        label = "Resolved Address",
    ) { state ->
        if (state is ResolvedState.Show) {
            Column {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                    color = TangemTheme.colors.stroke.primary,
                )
                Text(
                    text = state.address,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

private sealed interface ResolvedState {
    data object Hide : ResolvedState
    data class Show(val address: String) : ResolvedState
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
            onQrCodeClick = {},
            modifier = Modifier.background(TangemTheme.colors.background.primary),
            isRedesignEnabled = false,
            resolvedAddress = value.resolvedAddress,
        )
    }
}

private data class InputRowRecipientPreviewData(
    val value: String,
    val isError: Boolean,
    val isLoading: Boolean = false,
    val resolvedAddress: String? = null,
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
            InputRowRecipientPreviewData(
                value = "vitalik.eth",
                isLoading = false,
                isError = true,
                resolvedAddress = "0x391316d97a07027a0702c8A002c8A0C25d8470",
            ),
        )
}
//endregion