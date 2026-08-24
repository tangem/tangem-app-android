package com.tangem.features.hotwallet.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.hotwallet.impl.R

/**
 * Masked password input with an eye toggle, shared by the create- and restore-cloud-backup flows.
 * Holds no state of its own — the caller owns the [value] and visibility, so nothing secret lingers here.
 *
 * [contentType] wires the field to the platform password manager: [ContentType.NewPassword] makes it
 * offer a generated password (FR-02), [ContentType.Password] enables autofill on restore (FR-04).
 *
 * [REDACTED_TODO_COMMENT]
 */
@Suppress("LongParameterList")
@Composable
internal fun CloudBackupPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    placeholder: TextReference = resourceReference(R.string.hw_cloud_backup_password_hint),
    contentType: ContentType? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors.background.action)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = placeholder,
            color = if (isError) TangemTheme.colors.text.warning else TangemTheme.colors.text.primary1,
            visualTransformation = if (isVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            readOnly = !enabled,
            modifier = Modifier
                .weight(1f)
                .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                .then(
                    contentType?.let { type -> Modifier.semantics { this.contentType = type } } ?: Modifier,
                ),
        )
        Icon(
            painter = painterResource(
                id = if (isVisible) R.drawable.ic_eye_outline_24 else R.drawable.ic_eye_off_outline_24,
            ),
            contentDescription = stringResourceSafe(
                id = if (isVisible) R.string.hw_cloud_backup_hide_password else R.string.hw_cloud_backup_show_password,
            ),
            tint = TangemTheme.colors.icon.informative,
            modifier = Modifier
                .padding(start = 12.dp)
                .clickable(enabled = enabled, onClick = onToggleVisibility),
        )
    }
}