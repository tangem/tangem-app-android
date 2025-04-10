package com.tangem.features.nft.receive.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.nft.impl.R

@Composable
internal fun NFTReceiveNetworksEmpty(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResourceSafe(id = R.string.nft_empty_search),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.tertiary,
    )
}