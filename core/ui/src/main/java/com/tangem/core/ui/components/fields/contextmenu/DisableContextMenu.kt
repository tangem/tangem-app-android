package com.tangem.core.ui.components.fields.contextmenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Composable component that used to disable context menu
 *
 * @param content content
 */
@Composable
@NonSkippableComposable
fun DisableContextMenu(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        value = LocalTextToolbar provides EmptyTextToolbar,
        content = content,
    )
}

private object EmptyTextToolbar : TextToolbar {

    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit
}