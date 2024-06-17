package com.tangem.core.ui.components.snackbar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Snackbar to inform the user about copying text to the clipboard
 *
 * @param message  message
 * @param modifier modifier
 *
 * @see <a href = https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2001-728&t=kDzSZDx0m0sk4iYz-4
 * >Figma</a>
 *
* [REDACTED_AUTHOR]
 */
@Composable
fun CopiedTextSnackbar(message: TextReference, modifier: Modifier = Modifier) {
    BaseSnackbar(message = message, modifier = modifier)
}

/**
 * Snackbar to inform the user about copying text to the clipboard.
 *
 * @param snackbarData this is needed to better support Material3.SnackbarHost, but only supports the message field
 * @param modifier     modifier
 *
 * @see <a href = https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2001-728&t=kDzSZDx0m0sk4iYz-4
 * >Figma</a>
 *
* [REDACTED_AUTHOR]
 */
@Composable
fun CopiedTextSnackbar(snackbarData: SnackbarData, modifier: Modifier = Modifier) {
    BaseSnackbar(message = stringReference(snackbarData.visuals.message), modifier = modifier)
}

@Composable
private fun BaseSnackbar(message: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(color = TangemTheme.colors.icon.secondary, shape = TangemTheme.shapes.roundedCorners8)
            .heightIn(min = TangemTheme.dimens.size48)
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing14,
            ),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarkIcon()

        MessageText(text = message, modifier = Modifier.weight(weight = 1f, fill = false))
    }
}

@Composable
private fun MarkIcon() {
    Icon(
        painter = painterResource(id = R.drawable.ic_check_24),
        contentDescription = null,
        modifier = Modifier.size(TangemTheme.dimens.size20),
        tint = TangemTheme.colors.icon.accent,
    )
}

@Composable
private fun MessageText(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = text.resolveReference(),
        modifier = modifier,
        color = TangemTheme.colors.text.disabled,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTheme.typography.body2,
    )
}

@Preview(widthDp = 344, showBackground = true, fontScale = 1f)
@Preview(widthDp = 344, showBackground = true, fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(widthDp = 344, showBackground = true, fontScale = 2f)
@Composable
private fun Preview_CopiedTextSnackbar(
    @PreviewParameter(CopiedTextSnackbarDataProvider::class) message: TextReference,
) {
    TangemThemePreview {
        CopiedTextSnackbar(message = message)
    }
}

private class CopiedTextSnackbarDataProvider : CollectionPreviewParameterProvider<TextReference>(
    collection = listOf(
        stringReference(value = "Copied!"),
        stringReference(value = "Contract address copied!"),
        stringReference(value = "Coooooooooooontract addreeeeeeeeeeeeeeeess coooooooooooooooopied!"),
    ),
)
