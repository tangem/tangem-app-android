package com.tangem.core.ui.components.bottomsheets

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Tangem bottom sheet with custom draggable header and config
 *
 * @param config  data model containing logic and ui models
 * @param
 * @param content custom bottom sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color = TangemTheme.colors.background.primary,
    addBottomInsets: Boolean = true,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    var isVisible by remember { mutableStateOf(value = config.isShow) }
    val statusBarHeight = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (isVisible && config.content is T) {
        ModalBottomSheet(
            // FIXME temporary solution to fix height of the bottom sheet
            modifier = Modifier.sizeIn(maxHeight = LocalWindowSize.current.height - statusBarHeight),
            onDismissRequest = config.onDismissRequest,
            sheetState = sheetState,
            containerColor = containerColor,
            shape = TangemTheme.shapes.bottomSheetLarge,
            windowInsets = WindowInsetsZero,
            dragHandle = { TangemBottomSheetDraggableHeader(color = containerColor) },
        ) {
            if (addBottomInsets) {
                Column(
                    // FIXME temporary solution to fix height of the bottom sheet
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    content(config.content)
                }
            } else {
                content(config.content)
            }
        }
    }

    LaunchedEffect(key1 = config.isShow) {
        if (config.isShow) {
            isVisible = true
        } else {
            sheetState.collapse { isVisible = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun SheetState.collapse(onCollapsed: () -> Unit) {
    coroutineScope {
        launch { hide() }.invokeOnCompletion { onCollapsed() }
    }
}