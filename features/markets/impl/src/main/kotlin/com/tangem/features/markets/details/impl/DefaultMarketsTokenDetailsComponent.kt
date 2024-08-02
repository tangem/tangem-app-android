package com.tangem.features.markets.details.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.details.api.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.impl.model.MarketsTokenDetailsModel
import com.tangem.features.markets.details.impl.ui.MarketsTokenDetailsContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Stable
internal class DefaultMarketsTokenDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: MarketsTokenDetailsComponent.Params,
    @Assisted private val onBack: () -> Unit,
) : AppComponentContext by appComponentContext, MarketsTokenDetailsComponent {

    private val model: MarketsTokenDetailsModel = getOrCreateModel(params)

    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    ) {
        val state by model.state.collectAsStateWithLifecycle()

        MarketsTokenDetailsContent(
            state = state,
            onBackClick = { onBack() },
            onHeaderSizeChange = onHeaderSizeChange,
            modifier = modifier,
        )
    }

    @AssistedFactory
    interface Factory : MarketsTokenDetailsComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: MarketsTokenDetailsComponent.Params,
            onBack: () -> Unit,
        ): DefaultMarketsTokenDetailsComponent
    }
}