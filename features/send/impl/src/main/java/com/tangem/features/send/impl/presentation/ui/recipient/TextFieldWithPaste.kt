package com.tangem.features.send.impl.presentation.ui.recipient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.fields.SimpleTextField
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
) {
    val (title, color) = if (isError && error != null) {
        error to TangemTheme.colors.text.warning
    } else {
        label to TangemTheme.colors.text.secondary
    }
    FooterContainer(modifier, footer) {
        Row(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                ),
        ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TangemTheme.dimens.spacing6),
                )
            }
            PasteButton(
                isPasteButtonVisible = value.isBlank(),
                onClick = onPasteClick,
                modifier = Modifier
                    .align(CenterVertically)
                    .padding(end = TangemTheme.dimens.spacing16),
            )
        }
    }
}