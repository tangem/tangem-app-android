package com.tangem.features.feed.earn.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.features.feed.earn.model.filters.EarnNetworkFilterModel
import com.tangem.features.feed.earn.ui.components.EarnFilterByNetworkBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class EarnNetworkFilterComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableBottomSheetComponent {

    private val model = getOrCreateModel<EarnNetworkFilterModel, Params>(params = params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state = model.state.collectAsStateWithLifecycle()
        EarnFilterByNetworkBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = state.value,
            ),
        )
    }

    /**
     * @property networks every earn network; [EarnNetwork.isAdded] tells apart the user's own ones
     * @property selectedFilter the currently applied filter, restored as the sheet's initial selection
     */
    data class Params(
        val networks: List<EarnNetwork>,
        val selectedFilter: EarnFilterNetwork,
        val onFilterSelected: (EarnFilterNetwork) -> Unit,
        val onDismiss: () -> Unit,
    )
}