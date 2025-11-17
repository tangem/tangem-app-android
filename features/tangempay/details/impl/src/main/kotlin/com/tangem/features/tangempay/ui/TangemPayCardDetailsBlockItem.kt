package com.tangem.features.tangempay.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM

@Composable
internal fun TangemPayCardDetailsBlockItem(
    component: TangemPayCardDetailsBlockComponent,
    state: TangemPayCardDetailsUM,
    modifier: Modifier = Modifier,
) {
    component.CardDetailsBlockContent(state = state, modifier = modifier)
}