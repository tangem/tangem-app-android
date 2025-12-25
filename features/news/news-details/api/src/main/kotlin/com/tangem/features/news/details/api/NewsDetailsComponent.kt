package com.tangem.features.news.details.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableContentComponent

interface NewsDetailsComponent : ComposableContentComponent {

    data class Params(val selectedArticleId: Int = 0)

    @Composable
    fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    )

    interface Factory : ComponentFactory<Params, NewsDetailsComponent>
}