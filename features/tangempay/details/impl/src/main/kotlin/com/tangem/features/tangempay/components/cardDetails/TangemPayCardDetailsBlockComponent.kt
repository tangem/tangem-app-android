package com.tangem.features.tangempay.components.cardDetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import kotlinx.coroutines.flow.StateFlow

@Stable
internal interface TangemPayCardDetailsBlockComponent {
    val state: StateFlow<TangemPayCardDetailsUM>

    @Composable
    fun CardDetailsBlockContent(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier)

    data class Params(val params: TangemPayDetailsContainerComponent.Params)
}