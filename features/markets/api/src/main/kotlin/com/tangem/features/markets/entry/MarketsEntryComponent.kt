package com.tangem.features.markets.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState

@Stable
interface MarketsEntryComponent {

    @Composable
    fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    )

    interface Factory {
        fun create(context: AppComponentContext): MarketsEntryComponent
    }
}