package com.tangem.features.yield.supply.impl.common.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.R

@Composable
internal fun YieldSupplyFeeRow(
    title: TextReference,
    value: TextReference?,
    modifier: Modifier = Modifier,
    badge: TextReference? = null,
    isSelectable: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors3.text.primary,
        )
        SpacerWMax()
        badge?.let { Label(state = LabelUM(text = it)) }
        AnimatedContent(
            targetState = value,
        ) { targetValue ->
            if (targetValue != null) {
                Text(
                    text = targetValue.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.End,
                )
            } else {
                TextShimmer(
                    style = TangemTheme.typography.body1,
                    text = stringResourceSafe(R.string.common_fee_error),
                )
            }
        }
        if (isSelectable) {
            Icon(
                painter = painterResource(id = R.drawable.ic_select_18_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(width = 18.dp, height = 24.dp),
            )
        }
    }
}