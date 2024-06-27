package com.tangem.core.ui.components.snackbar

import android.content.res.Configuration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Tangem snackbar.
 * Based on Material3 component. It can be presented as one line or multi lines snackbar – depends on text length.
 *
 * @param data            snackbar data
 * @param modifier        modifier
 * @param actionOnNewLine flag that indicates if the action should be displayed on a new line (default: false)
 *
 * @see <a href = https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=682-761&t=kDzSZDx0m0sk4iYz-4
 * >Figma</a>
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TangemSnackbar(data: SnackbarData, modifier: Modifier = Modifier, actionOnNewLine: Boolean = false) {
    Snackbar(
        modifier = modifier,
        action = {
            ActionButton(label = data.visuals.actionLabel, onClick = data::performAction)
        },
        actionOnNewLine = actionOnNewLine,
        shape = TangemTheme.shapes.roundedCorners8,
        containerColor = TangemTheme.colors.icon.secondary,
    ) {
        MessageText(text = data.visuals.message)
    }
}

@Composable
private fun ActionButton(label: String?, onClick: () -> Unit) {
    if (!label.isNullOrBlank()) {
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = TangemTheme.colors.text.primary2,
            ),
            content = {
                Text(
                    text = label,
                    maxLines = 1,
                    style = TangemTheme.typography.button,
                )
            },
        )
    }
}

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        color = TangemTheme.colors.text.disabled,
        textAlign = TextAlign.Start,
        overflow = TextOverflow.Ellipsis,
        style = TangemTheme.typography.body2,
    )
}

/**
 * IMPORTANT!
 * Preview doesn't work correctly, check on device or start [TangemSnackbarHost]'s preview in interactive mode *
 */
@Preview(widthDp = 344, showBackground = true, fontScale = 1f)
@Preview(widthDp = 344, showBackground = true, fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(widthDp = 344, showBackground = true, fontScale = 2f)
@Composable
private fun Preview_TangemSnackbar(@PreviewParameter(TangemSnackbarModelProvider::class) model: TangemSnackbarModel) {
    TangemThemePreview {
        TangemSnackbar(data = model.snackbarData, actionOnNewLine = model.actionOnNewLine)
    }
}