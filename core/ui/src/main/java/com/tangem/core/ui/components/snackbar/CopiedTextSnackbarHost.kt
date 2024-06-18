package com.tangem.core.ui.components.snackbar

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * SnackbarHost to inform the user about copying text to the clipboard
 * Based on Material3 component. It's best way to show [CopiedTextSnackbar].
 *
 * @param hostState snackbar host state
 * @param modifier  modifier
 *
 * @see <a href = https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2001-728&t=kDzSZDx0m0sk4iYz-4
 * >Figma</a>
 *
[REDACTED_AUTHOR]
 */
@Composable
fun CopiedTextSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) {
        CopiedTextSnackbar(snackbarData = it)
    }
}