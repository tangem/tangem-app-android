package com.tangem.features.feed.components.news.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent

internal class DefaultNewsListComponent(
    appComponentContext: AppComponentContext,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
    }

    @Composable
    override fun Content(bottomSheetState: State<BottomSheetState>, modifier: Modifier) {
    }
}