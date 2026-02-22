package com.tangem.features.tangempay.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.TangemPayMainBannerComponent
import com.tangem.features.tangempay.model.TangemPayMainBannerModel
import com.tangem.features.tangempay.TangemPayMainBannerState
import com.tangem.features.tangempay.ui.TangemPayMainScreenBlock
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class DefaultTangemPayMainBannerComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted params: TangemPayMainBannerComponent.Params,
) : AppComponentContext by context, TangemPayMainBannerComponent {

    private val model: TangemPayMainBannerModel = getOrCreateModel(params = params)
    override val state: StateFlow<TangemPayMainBannerState> = model.uiState

    override fun LazyListScope.tangemPayMainBannerContent(
        state: TangemPayMainBannerState,
        isHidingMode: Boolean,
        modifier: Modifier,
    ) {
        item(
            key = "TangemPayMainScreenBlock",
            contentType = state::class.java,
        ) {
            TangemPayMainScreenBlock(
                state = state,
                isBalanceHidden = isHidingMode,
                modifier = modifier,
            )
        }
    }

    @AssistedFactory
    interface Factory : TangemPayMainBannerComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: TangemPayMainBannerComponent.Params,
        ): DefaultTangemPayMainBannerComponent
    }
}