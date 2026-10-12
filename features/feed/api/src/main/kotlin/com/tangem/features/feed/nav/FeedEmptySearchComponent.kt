package com.tangem.features.feed.nav

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState

interface FeedEmptySearchComponent {

    @Composable
    fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
        header: @Composable () -> Unit,
    )
}