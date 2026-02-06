package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun DateBlock(currentDate: String) {
    SpacerH(20.dp)
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        text = stringResourceSafe(R.string.feed_market_and_news),
        style = TangemTheme.typography.h2,
        color = TangemTheme.colors.text.primary1,
    )
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        text = currentDate,
        style = TangemTheme.typography.h2,
        color = TangemTheme.colors.text.tertiary,
    )
}