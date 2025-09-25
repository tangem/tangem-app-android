package com.tangem.features.yield.supply.impl.subcomponents.startearning.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.ChildStack
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningRoute

@Composable
internal fun YieldSupplyStartEarningBottomSheet(
    stackState: ChildStack<YieldSupplyStartEarningRoute, ComposableModularContentComponent>,
    onDismiss: () -> Unit,
) {
    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            AnimatedContent(
                stackState.active.instance,
            ) { currentState ->
                currentState.Title()
            }
        },
        footer = {
            AnimatedContent(
                stackState.active.instance,
            ) { currentState ->
                currentState.Footer()
            }
        },
        content = {
            AnimatedContent(
                stackState.active.instance,
            ) { currentState ->
                currentState.Content(modifier = Modifier)
            }
        },
    )
}