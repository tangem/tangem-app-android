package com.tangem.features.tangempay.components.cardDetails

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.ui.TangemPayCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class PreviewTangemPayCardDetailsBlockComponent(
    state: TangemPayCardDetailsUM,
) : TangemPayCardDetailsBlockComponent {

    override val state: StateFlow<TangemPayCardDetailsUM> = MutableStateFlow(state)

    @Composable
    override fun CardDetailsBlockContent(state: TangemPayCardDetailsUM, modifier: Modifier) {
        TangemPayCard(state, modifier)
    }
}