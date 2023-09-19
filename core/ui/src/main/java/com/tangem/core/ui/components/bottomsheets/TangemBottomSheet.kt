package com.tangem.core.ui.components.bottomsheets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import com.tangem.core.ui.res.TangemTheme

/**
 * Tangem bottom sheet with custom draggable header and config
 *
 * @param config data model containing logic and ui models
 * @param content custom bottom sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TangemBottomSheet(
    config: TangemBottomSheetConfig,
    content: @Composable ColumnScope.(TangemBottomSheetConfigContent) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = config.onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TangemTheme.colors.background.primary,
        shape = TangemTheme.shapes.bottomSheetLarge,
        dragHandle = { TangemBottomSheetDraggableHeader() },
    ) {
        content(config.content)
    }
}
