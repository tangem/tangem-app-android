package com.tangem.features.mcp.impl.pair.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.mcp.impl.R
import com.tangem.features.mcp.impl.pair.model.McpConnectButtonUM
import com.tangem.features.mcp.impl.pair.model.McpPairRoutes
import com.tangem.features.mcp.impl.pair.model.McpPairUM

/**
 * Single bottom sheet shell of the pairing flow: the title, the footer and the sheet itself survive
 * every navigation, only [stack]'s active content is swapped.
 *
 * The content is design-system v2 while the shell stays the v1 sheet, like `PortfolioSelectorBS`:
 * `TangemModal` probes its content's intrinsic height on every frame to drive the morph, which the
 * selector's `LazyColumn` cannot answer — and `scrollableContent` does not turn that probe off.
 */
@Composable
internal fun McpPairBottomSheet(
    stack: ChildStack<McpPairRoutes, ComposableContentComponent>,
    state: McpPairUM,
    title: TextReference,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        onBack = onBack,
        title = {
            TangemTopNavigation(
                title = title,
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onBack = onBack.takeIf { stack.active.configuration == McpPairRoutes.PortfolioSelector },
                onClose = onDismiss.takeIf { stack.backStack.isEmpty() },
            )
        },
        content = {
            AnimatedContent(targetState = stack, label = "McpPairRouteContent") { animatedStack ->
                RouteContent(stack = animatedStack)
            }
        },
        footer = { McpPairFooter(state = state, route = stack.active.configuration) },
    )
}

@Composable
private fun RouteContent(stack: ChildStack<McpPairRoutes, ComposableContentComponent>) {
    val contentModifier = Modifier.padding(horizontal = 16.dp)
    when (stack.active.configuration) {
        McpPairRoutes.PortfolioSelector -> stack.active.instance.Content(modifier = contentModifier)
        // The selector renders its own list, so only the other routes may scroll.
        McpPairRoutes.AgentInfo,
        McpPairRoutes.Success,
        -> {
            val scrollState = rememberScrollState()
            ReportContentScrollable(scrollState)

            val bottomInset = LocalTangemBottomSheetContentBottomInset.current
            Column(modifier = contentModifier.verticalScroll(scrollState)) {
                stack.active.instance.Content(modifier = Modifier)
                Spacer(modifier = Modifier.height(if (bottomInset > 0.dp) bottomInset else 16.dp))
            }
        }
    }
}

/**
 * Without this the sheet keeps reserving the footer's fade gradient under content that does not
 * scroll, which reads as an empty band between the content and the buttons.
 */
@Composable
private fun ReportContentScrollable(scrollState: ScrollState) {
    val contentScrollable = LocalBottomSheetContentScrollable.current ?: return
    val isScrollable by remember {
        derivedStateOf { scrollState.canScrollForward || scrollState.canScrollBackward }
    }

    LaunchedEffect(isScrollable) { contentScrollable.value = isScrollable }
}

@Composable
private fun McpPairFooter(state: McpPairUM, route: McpPairRoutes) {
    if (route == McpPairRoutes.PortfolioSelector) return

    TangemFooter(
        modifier = Modifier.fillMaxWidth(),
        windowInsets = WindowInsets(0),
        fadeEnabled = false,
    ) {
        TangemButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            orientation = TangemButtonGroup.Orientation.Horizontal,
        ) {
            when (state) {
                is McpPairUM.Success -> TangemButton(
                    text = resourceReference(R.string.common_done),
                    onClick = state.onDone,
                )
                // Nothing to accept or decline until the agent behind the user code is known.
                is McpPairUM.Loading -> ConnectButtons(connectButton = null, onDecline = {})
                is McpPairUM.AgentInfo -> ConnectButtons(
                    connectButton = state.connectButton,
                    onDecline = state.onDecline,
                )
            }
        }
    }
}

@Composable
private fun ConnectButtons(connectButton: McpConnectButtonUM?, onDecline: () -> Unit) {
    TangemButton(
        variant = TangemButton.Variant.Secondary,
        text = resourceReference(R.string.common_decline),
        size = TangemButton.Size.X12,
        isEnabled = connectButton != null,
        onClick = onDecline,
    )
    TangemButton(
        variant = TangemButton.Variant.Primary,
        text = resourceReference(R.string.wc_common_connect),
        size = TangemButton.Size.X12,
        isEnabled = connectButton?.isEnabled == true,
        isLoading = connectButton?.isLoading == true,
        onClick = { connectButton?.onClick?.invoke() },
    )
}