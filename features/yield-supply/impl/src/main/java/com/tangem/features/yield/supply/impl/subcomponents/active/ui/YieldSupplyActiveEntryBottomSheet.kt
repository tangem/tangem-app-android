package com.tangem.features.yield.supply.impl.subcomponents.active.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.subcomponents.active.model.YieldSupplyActiveRoute

@Composable
internal fun YieldSupplyActiveEntryBottomSheet(
    stackState: ChildStack<YieldSupplyActiveRoute, ComposableModularContentComponent>,
    onDismiss: () -> Unit,
) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        title = { state ->
            AnimatedContent(
                stackState.active.instance,
            ) { currentState ->
                currentState.Title()
            }
        },
        footer = { state ->
            AnimatedContent(
                stackState.active.instance,
            ) { currentState ->
                currentState.Footer()
            }
        },
        content = { state ->
            AnimatedContent(
                stackState.active.instance,
            ) { currentState ->
                currentState.Content(modifier = Modifier)
            }
        },
    )
}