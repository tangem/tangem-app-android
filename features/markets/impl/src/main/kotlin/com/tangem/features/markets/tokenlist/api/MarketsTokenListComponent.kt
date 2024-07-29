package com.tangem.features.markets.tokenlist.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.component.BottomSheetState

@Stable
interface MarketsTokenListComponent {

    @Composable
    fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    )

    interface Factory {
        fun create(context: AppComponentContext, onTokenSelected: (TokenMarket) -> Unit): MarketsTokenListComponent
    }
}
