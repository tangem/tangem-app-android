package com.tangem.core.ui.components.snackbar

import android.content.res.Configuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Tangem snackbar host.
 * Based on Material3 component. It's best way to show [TangemSnackbar].
 *
 * @param hostState       snackbar host state
 * @param modifier        modifier
 * @param actionOnNewLine flag that indicates if the action should be displayed on a new line (default: false)
 *
 * @see <a href = https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=682-761&t=kDzSZDx0m0sk4iYz-4
 * >Figma</a>
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TangemSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier, actionOnNewLine: Boolean = false) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        TangemSnackbar(data = data, actionOnNewLine = actionOnNewLine)
    }
}

@Preview(widthDp = 344, showBackground = true, fontScale = 1f)
@Preview(widthDp = 344, showBackground = true, fontScale = 1f, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(widthDp = 344, showBackground = true, fontScale = 2f)
@Composable
private fun Preview_TangemSnackbar(@PreviewParameter(TangemSnackbarModelProvider::class) model: TangemSnackbarModel) {
    TangemThemePreview {
        val snackbarHostState = remember(::SnackbarHostState)

        TangemSnackbarHost(hostState = snackbarHostState, actionOnNewLine = model.actionOnNewLine)

        LaunchedEffect(key1 = null) {
            snackbarHostState.showSnackbar(visuals = model.snackbarData.visuals)
        }
    }
}