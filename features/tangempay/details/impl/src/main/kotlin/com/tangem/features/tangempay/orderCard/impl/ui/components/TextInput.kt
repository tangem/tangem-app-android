package com.tangem.features.tangempay.orderCard.impl.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_20_filled
import com.tangem.features.tangempay.details.impl.R

private val CLEAR_ICON_SIZE = 20.dp

@Immutable
internal sealed interface TextInputState {

    data object Default : TextInputState

    data object Disabled : TextInputState

    data class Error(val text: TextReference) : TextInputState
}

internal fun shouldReportFocusChange(wasFocused: Boolean?, isFocused: Boolean): Boolean =
    if (wasFocused == null) isFocused else wasFocused != isFocused

@Suppress("LongParameterList", "MagicNumber")
@Composable
internal fun TextInput(
    label: TextReference,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: TextInputState = TextInputState.Default,
    isRequired: Boolean = false,
    placeholder: TextReference? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onFocusChange: (Boolean) -> Unit = {},
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isDisabled = state is TextInputState.Disabled
    val requiredMarkColor = TangemTheme.colors3.text.status.error
    val labelText = buildAnnotatedString {
        append(label.resolveReference())
        if (isRequired) {
            withStyle(SpanStyle(color = requiredMarkColor)) { append('*') }
        }
    }
    val errorText = (state as? TextInputState.Error)?.text?.resolveReference()

    Column(modifier = modifier) {
        Text(
            text = labelText,
            style = TangemTheme.typography3.caption.medium,
            color = if (isDisabled) TangemTheme.colors3.text.tertiary else TangemTheme.colors3.text.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerH(4.dp)
        Row(
            modifier = Modifier.heightIn(min = CLEAR_ICON_SIZE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InputField(
                value = value,
                onValueChange = onValueChange,
                isDisabled = isDisabled,
                errorText = errorText,
                placeholder = placeholder?.takeUnless { isDisabled },
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                onFocusChange = onFocusChange,
                modifier = Modifier.weight(1f),
            )
            if (isFocused && value.isNotEmpty() && !isDisabled) {
                Icon(
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .clickable(role = Role.Button) { onValueChange("") }
                        .padding(start = 8.dp)
                        .clip(CircleShape)
                        .size(CLEAR_ICON_SIZE),
                    imageVector = Icons.ic_cross_circle_20_filled,
                    tint = TangemTheme.colors3.icon.tertiary,
                    contentDescription = stringResourceSafe(R.string.tangempay_order_data_clear_field),
                )
            }
        }
        SpacerH(12.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor(errorText = errorText, isFocused = isFocused)),
        )
        if (errorText != null) {
            SpacerH(4.dp)
            Text(
                text = errorText,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
            )
        }
    }
}

@Composable
private fun dividerColor(errorText: String?, isFocused: Boolean): Color = when {
    errorText != null -> TangemTheme.colors3.border.status.error
    isFocused -> TangemTheme.colors3.border.brand
    else -> TangemTheme.colors3.border.tertiary
}

@Suppress("LongParameterList")
@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    isDisabled: Boolean,
    errorText: String?,
    placeholder: TextReference?,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    interactionSource: MutableInteractionSource,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var wasFocused by remember { mutableStateOf<Boolean?>(null) }
    val textStyle = TangemTheme.typography3.body.medium.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    val valueColor = if (isDisabled) TangemTheme.colors3.text.tertiary else TangemTheme.colors3.text.primary

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                val isFocused = focusState.isFocused
                if (shouldReportFocusChange(wasFocused = wasFocused, isFocused = isFocused)) {
                    wasFocused = isFocused
                    onFocusChange(isFocused)
                }
            }
            .semantics {
                if (isDisabled) disabled()
                if (errorText != null) error(errorText)
            },
        value = value,
        onValueChange = onValueChange,
        enabled = !isDisabled,
        singleLine = true,
        textStyle = textStyle.copy(color = valueColor),
        cursorBrush = SolidColor(TangemTheme.colors3.icon.brand),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder.resolveReference(),
                        style = textStyle,
                        color = TangemTheme.colors3.text.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360, name = "Light")
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun TextInputPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextInput(
                label = stringReference("Label"),
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = stringReference("Placeholder"),
            )
            TextInput(
                label = stringReference("Label"),
                value = "Value",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                isRequired = true,
            )
            TextInput(
                label = stringReference("Country"),
                value = "United States",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                state = TextInputState.Disabled,
            )
            TextInput(
                label = stringReference("Label"),
                value = "Value",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                state = TextInputState.Error(stringReference("Error text")),
            )
        }
    }
}