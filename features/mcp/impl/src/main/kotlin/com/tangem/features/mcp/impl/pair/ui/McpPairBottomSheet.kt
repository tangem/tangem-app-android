package com.tangem.features.mcp.impl.pair.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.mcp.impl.pair.model.McpPairRoutes
import com.tangem.features.mcp.impl.pair.model.McpPairUM

/**
 * Shell of the pairing flow — a stub until the screen is built on the design system.
 *
 * The flow below it is wired end to end (routing, model, repository), only the layout is missing, so
 * reviewers do not read a throwaway MVP version and then the real one.
 */
@Suppress("UnusedParameter", "EmptyFunctionBlock")
@Composable
internal fun McpPairBottomSheet(
    stack: ChildStack<McpPairRoutes, ComposableContentComponent>,
    state: McpPairUM,
    title: TextReference,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
}