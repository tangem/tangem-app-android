package com.tangem.features.send.impl.presentation.ui.recipient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.inputrow.inner.CrossIcon
import com.tangem.core.ui.components.inputrow.inner.PasteButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.ui.common.FooterContainer

@Composable
internal fun TextFieldWithPaste(
    value: String,
    placeholder: TextReference,
    label: TextReference,
    onValueChange: (String) -> Unit,
    onPasteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    footer: String? = null,
    error: TextReference? = null,
    isError: Boolean = false,
    isReadOnly: Boolean = false,
) {
    val (title, color) = when {
        isError && error != null -> error to TangemTheme.colors.text.warning
        isReadOnly -> label to TangemTheme.colors.text.disabled
        else -> label to TangemTheme.colors.text.secondary
    }
    FooterContainer(modifier, footer) {
        Box(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                )
                .padding(end = TangemTheme.dimens.spacing16),
        ) {
            Row {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(TangemTheme.dimens.spacing12),
                ) {
                    Text(
                        text = title.resolveReference(),
                        style = TangemTheme.typography.body2,
                        color = color,
                    )
                    SimpleTextField(
                        value = value,
                        placeholder = placeholder,
                        onValueChange = onValueChange,
                        readOnly = isReadOnly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = TangemTheme.dimens.spacing6),
                    )
                }
                AnimatedVisibility(
                    visible = !isReadOnly,
                    label = "Animate read only status change",
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(CenterVertically),
                ) {
                    CrossIcon(
                        onClick = onPasteClick,
                    )
                }
            }
            AnimatedVisibility(
                visible = !isReadOnly,
                label = "Animate read only status change",
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(CenterEnd),
            ) {
                PasteButton(
                    isPasteButtonVisible = value.isBlank(),
                    onClick = onPasteClick,
                )
            }
        }
    }
}