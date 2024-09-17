package com.tangem.features.markets.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.markets.entry.BottomSheetState
import kotlinx.serialization.Serializable

@Stable
interface MarketsTokenDetailsComponent : ComposableContentComponent {

    @Serializable
    data class Params(
        val token: TokenMarketParams,
        val appCurrency: AppCurrency,
        val showPortfolio: Boolean,
        val analyticsParams: AnalyticsParams?,
    )

    @Serializable
    data class AnalyticsParams(
        val blockchain: String?,
        val source: String,
    )

    @Composable
    fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    )

    interface Factory : ComponentFactory<Params, MarketsTokenDetailsComponent>
}