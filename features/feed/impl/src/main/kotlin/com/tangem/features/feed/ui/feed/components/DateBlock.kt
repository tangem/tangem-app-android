package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun DateBlock(currentDate: String) {
    SpacerH(TangemTheme.dimens2.x1)
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x6),
        text = stringResourceSafe(R.string.feed_market_and_news),
        style = TangemTheme.typography2.headingSemibold28,
        color = TangemTheme.colors2.text.neutral.primary,
    )
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens2.x6),
        text = currentDate,
        style = TangemTheme.typography2.headingRegular28,
        color = TangemTheme.colors2.text.neutral.tertiary,
    )
}