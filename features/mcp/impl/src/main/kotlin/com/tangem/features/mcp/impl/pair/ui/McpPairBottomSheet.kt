package com.tangem.features.mcp.impl.pair.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.mcp.impl.R
import com.tangem.features.mcp.impl.pair.model.McpPairRoutes
import com.tangem.features.mcp.impl.pair.model.McpPairTexts
import com.tangem.features.mcp.impl.pair.model.McpPairUM

/**
 * Single bottom sheet shell of the pairing flow: the title, the footer and the sheet itself survive
 * every navigation, only [stack]'s active content is swapped.
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
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = onBack,
        title = {
            TangemModalBottomSheetTitle(
                title = title,
                endIconRes = R.drawable.ic_close_24,
                onEndClick = onDismiss,
            )
        },
        content = {
            AnimatedContent(targetState = stack, label = "McpPairRouteContent") { animatedStack ->
                RouteContent(stack = animatedStack)
            }
        },
        footer = {
            if (stack.active.configuration == McpPairRoutes.AgentInfo) {
                McpPairButtons(state = state)
            }
        },
    )
}

@Composable
private fun RouteContent(stack: ChildStack<McpPairRoutes, ComposableContentComponent>) {
    val contentModifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16)
    when (stack.active.configuration) {
        McpPairRoutes.AgentInfo -> {
            // The selector renders its own list, so only this route may scroll.
            val bottomInset = LocalTangemBottomSheetContentBottomInset.current
            Column(modifier = contentModifier.verticalScroll(rememberScrollState())) {
                stack.active.instance.Content(modifier = Modifier)
                Spacer(
                    modifier = Modifier.height(
                        if (bottomInset > 0.dp) bottomInset else TangemTheme.dimens.spacing16,
                    ),
                )
            }
        }
        McpPairRoutes.PortfolioSelector -> stack.active.instance.Content(modifier = contentModifier)
    }
}

@Composable
private fun McpPairButtons(state: McpPairUM) {
    val connectButton = (state as? McpPairUM.Content)?.connectButton
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        SecondaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = McpPairTexts.decline.resolveReference(),
            onClick = state.onDecline,
        )
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            text = stringResourceSafe(R.string.wc_common_connect),
            onClick = { connectButton?.onClick?.invoke() },
            showProgress = connectButton?.isLoading == true,
            enabled = connectButton?.isEnabled == true,
        )
    }
}