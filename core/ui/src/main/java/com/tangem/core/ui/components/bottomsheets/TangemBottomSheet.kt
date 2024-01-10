package com.tangem.core.ui.components.bottomsheets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Tangem bottom sheet with custom draggable header and config
 *
 * @param config  data model containing logic and ui models
 * @param content custom bottom sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
inline fun <reified T : TangemBottomSheetConfigContent> TangemBottomSheet(
    config: TangemBottomSheetConfig,
    containerColor: Color = TangemTheme.colors.background.primary,
    crossinline content: @Composable ColumnScope.(T) -> Unit,
) {
    var isVisible by remember { mutableStateOf(value = config.isShow) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (isVisible && config.content is T) {
        ModalBottomSheet(
            onDismissRequest = config.onDismissRequest,
            sheetState = sheetState,
            containerColor = containerColor,
            shape = TangemTheme.shapes.bottomSheetLarge,
            dragHandle = { TangemBottomSheetDraggableHeader(color = containerColor) },
        ) {
            content(config.content)
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