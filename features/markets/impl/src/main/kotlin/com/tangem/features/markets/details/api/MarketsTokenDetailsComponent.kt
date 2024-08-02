package com.tangem.features.markets.details.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.markets.component.BottomSheetState
import kotlinx.serialization.Serializable

@Stable
interface MarketsTokenDetailsComponent {

    @Serializable
    data class Params(
        val token: TokenMarketSerializable,
        val appCurrency: AppCurrency,
    )

    @Composable
    fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    )

    interface Factory {
        fun create(context: AppComponentContext, params: Params, onBack: () -> Unit): MarketsTokenDetailsComponent
    }
}
