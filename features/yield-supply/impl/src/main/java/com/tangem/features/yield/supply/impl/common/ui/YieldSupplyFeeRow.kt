package com.tangem.features.yield.supply.impl.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.R

@Composable
internal fun YieldSupplyFeeRow(title: TextReference, value: TextReference?, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()
        AnimatedContent(
            targetState = value,
        ) { targetValue ->
            if (targetValue != null) {
                Text(
                    text = targetValue.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.tertiary,
                    textAlign = TextAlign.End,
                )
            } else {
                TextShimmer(
                    style = TangemTheme.typography.body1,
                    text = stringResourceSafe(R.string.common_fee_error),
                )
            }
        }
    }
}