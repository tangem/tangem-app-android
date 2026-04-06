package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.features.markets.token.block.TokenMarketBlockComponent

@Suppress("UnusedParameter")
@Composable
internal fun TokenDetailsScreen(
    tokenDetailsUM: TokenDetailsUM,
    tokenMarketBlockComponent: TokenMarketBlockComponent?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Token Details Redesign",
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}