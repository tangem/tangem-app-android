package com.tangem.features.polymarket.impl.common.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_top_right_24
import com.tangem.features.polymarket.impl.common.PolymarketResolutionSourceUM

@Composable
internal fun PolymarketResolutionSourceRow(
    state: PolymarketResolutionSourceUM,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    includeInnerPaddings: Boolean = true,
) {
    TangemRow(
        modifier = modifier,
        includeInnerPaddings = includeInnerPaddings,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.prediction_details_resolution_source),
                role = TangemRowTextRole.Title,
                maxLines = Int.MAX_VALUE,
            )
        },
        valueSlot = {
            Text(
                text = state.text,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.End,
            )
        },
        endSlot = if (state.url != null) {
            {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.ic_arrow_top_right_24,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                )
            }
        } else {
            null
        },
        onClick = state.url?.let { url -> { onOpenUrl(url) } },
    )
}