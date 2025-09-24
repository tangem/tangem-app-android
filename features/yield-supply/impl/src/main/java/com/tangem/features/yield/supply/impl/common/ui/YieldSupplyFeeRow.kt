package com.tangem.features.yield.supply.impl.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.R

@Composable
internal fun YieldSupplyFeeRow(title: TextReference, value: TextReference?) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors.background.action)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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