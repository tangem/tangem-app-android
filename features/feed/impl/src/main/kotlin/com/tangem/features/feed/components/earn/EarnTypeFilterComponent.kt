package com.tangem.features.feed.components.earn

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.features.feed.model.earn.filters.EarnTypeFilterModel
import com.tangem.features.feed.ui.earn.components.EarnFilterByTypeBottomSheet
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal class EarnTypeFilterComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: Params,
) : AppComponentContext by context, ComposableBottomSheetComponent {

    private val model = getOrCreateModel<EarnTypeFilterModel, Params>(params = params)

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state = model.state.collectAsStateWithLifecycle()

        EarnFilterByTypeBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = state.value,
            ),
        )
    }

    data class Params(
        val selectedFilter: EarnFilterType,
        val onFilterSelected: (EarnFilterType) -> Unit,
        val onDismiss: () -> Unit,
    )
}