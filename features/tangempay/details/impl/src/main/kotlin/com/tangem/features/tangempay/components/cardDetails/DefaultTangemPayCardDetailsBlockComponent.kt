package com.tangem.features.tangempay.components.cardDetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.model.TangemPayCardDetailsBlockModel
import com.tangem.features.tangempay.ui.TangemPayCard
import kotlinx.coroutines.flow.StateFlow

internal class DefaultTangemPayCardDetailsBlockComponent(
    appComponentContext: AppComponentContext,
    params: TangemPayCardDetailsBlockComponent.Params,
) : AppComponentContext by appComponentContext, TangemPayCardDetailsBlockComponent {

    private val model: TangemPayCardDetailsBlockModel = getOrCreateModel(params = params)
    override val state: StateFlow<TangemPayCardDetailsUM> = model.uiState

    @Composable
    override fun CardDetailsBlockContent(state: TangemPayCardDetailsUM, modifier: Modifier) {
        TangemPayCard(state, modifier)
    }
}