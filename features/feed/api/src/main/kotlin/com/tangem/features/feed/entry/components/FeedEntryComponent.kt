package com.tangem.features.feed.entry.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableContentComponent

@Stable
interface FeedEntryComponent : ComposableContentComponent {

    @Composable
    fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    )

    interface Factory : ComponentFactory<FeedEntryRoute, FeedEntryComponent> {
        fun create(context: AppComponentContext, entryRoute: FeedEntryRoute?): FeedEntryComponent
    }
}