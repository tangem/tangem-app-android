package com.tangem.features.tangempay.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM

@Composable
internal fun CardDetailsItem(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier) {
    TangemPayCardDetailsBlock(
        modifier = modifier
            .padding(top = TangemTheme.dimens.spacing12)
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        state = state,
    )
}