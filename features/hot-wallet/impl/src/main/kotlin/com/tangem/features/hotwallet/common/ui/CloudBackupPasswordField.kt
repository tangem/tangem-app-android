package com.tangem.features.hotwallet.common.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_eye_20
import com.tangem.core.ui.res.generated.icons.ic_eye_cross_20
import com.tangem.features.hotwallet.impl.R

private val EYE_ICON_SIZE = 20.dp

/**
 * Masked password input with an eye toggle, shared by the create- and restore-cloud-backup flows.
 * Holds no state of its own — the caller owns the [value] and visibility, so nothing secret lingers here.
 *
 * [contentType] wires the field to the platform password manager: [ContentType.NewPassword] makes it
 * offer a generated password (FR-02), [ContentType.Password] enables autofill on restore (FR-04).
 */
@Suppress("LongParameterList")
@Composable
internal fun CloudBackupPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    label: TextReference = resourceReference(R.string.hw_cloud_backup_password_hint),
    errorText: TextReference? = null,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    contentType: ContentType? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val error = errorText?.resolveReference()

    Column(modifier = modifier) {
        Text(
            text = label.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = if (enabled) TangemTheme.colors3.text.secondary else TangemTheme.colors3.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SpacerH(2.dp)
        Row(
            modifier = Modifier.heightIn(min = EYE_ICON_SIZE),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PasswordInputField(
                value = value,
                onValueChange = onValueChange,
                isVisible = isVisible,
                enabled = enabled,
                error = error,
                focusRequester = focusRequester,
                contentType = contentType,
                interactionSource = interactionSource,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (isVisible) Icons.ic_eye_20 else Icons.ic_eye_cross_20,
                contentDescription = stringResourceSafe(
                    id = if (isVisible) {
                        R.string.hw_cloud_backup_hide_password
                    } else {
                        R.string.hw_cloud_backup_show_password
                    },
                ),
                tint = TangemTheme.colors3.icon.tertiary,
                modifier = Modifier
                    .focusProperties { canFocus = false }
                    .clip(CircleShape)
                    .clickable(enabled = enabled, role = Role.Button, onClick = onToggleVisibility)
                    .padding(start = 8.dp)
                    .size(EYE_ICON_SIZE),
            )
        }
        SpacerH(12.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor(hasError = error != null, isFocused = isFocused, enabled = enabled)),
        )
        if (error != null) {
            SpacerH(12.dp)
            Text(
                text = error,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
            )
        }
    }
}

@Composable
private fun dividerColor(hasError: Boolean, isFocused: Boolean, enabled: Boolean): Color = when {
    hasError -> TangemTheme.colors3.border.status.error
    isFocused && enabled -> TangemTheme.colors3.border.brand
    else -> TangemTheme.colors3.border.tertiary
}

@Suppress("LongParameterList")
@Composable
private fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    enabled: Boolean,
    error: String?,
    focusRequester: FocusRequester?,
    contentType: ContentType?,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
) {
    val textStyle = TangemTheme.typography3.body.medium.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    val valueColor = if (enabled) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.tertiary

    var fieldValueState by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    val fieldValue = if (fieldValueState.text == value) {
        fieldValueState
    } else {
        TextFieldValue(text = value, selection = TextRange(value.length))
    }
    SideEffect { fieldValueState = fieldValue }

    BasicTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            fieldValueState = newValue
            if (newValue.text != value) onValueChange(newValue.text)
        },
        modifier = modifier
            .fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .then(contentType?.let { type -> Modifier.semantics { this.contentType = type } } ?: Modifier)
            .semantics {
                if (!enabled) disabled()
                if (error != null) error(error)
            },
        readOnly = !enabled,
        singleLine = true,
        textStyle = textStyle.copy(color = valueColor),
        cursorBrush = SolidColor(TangemTheme.colors3.icon.brand),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        interactionSource = interactionSource,
    )
}

@Preview(showBackground = true, widthDp = 360, name = "Light")
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Dark")
@Composable
private fun CloudBackupPasswordFieldPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            CloudBackupPasswordField(
                value = "",
                onValueChange = {},
                isVisible = false,
                onToggleVisibility = {},
            )
            CloudBackupPasswordField(
                value = "Str0ng!Pass",
                onValueChange = {},
                isVisible = true,
                onToggleVisibility = {},
            )
            CloudBackupPasswordField(
                value = "12345678",
                onValueChange = {},
                isVisible = true,
                onToggleVisibility = {},
                errorText = stringReference("Password doesn't match"),
            )
            CloudBackupPasswordField(
                value = "Str0ng!Pass",
                onValueChange = {},
                isVisible = false,
                onToggleVisibility = {},
                enabled = false,
            )
        }
    }
}