package com.tangem.features.markets.tokenlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams

@Stable
interface MarketsTokenListComponent : ComposableContentComponent {

    @Composable
    fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    )

    interface FactoryScreen : ComponentFactory<Unit, MarketsTokenListComponent>

    interface FactoryBottomSheet {
        fun create(
            context: AppComponentContext,
            params: Unit,
            onTokenClick: ((TokenMarketParams, AppCurrency) -> Unit)?,
        ): MarketsTokenListComponent
    }
}