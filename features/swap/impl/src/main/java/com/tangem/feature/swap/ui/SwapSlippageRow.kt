package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_double_vertical_16
import com.tangem.feature.swap.models.states.SwapSlippageUM
import com.tangem.feature.swap.presentation.R

/** Detailed-mode card grouping the slippage row with the network fee block. */
@Composable
internal fun SwapSlippageAndFeeBlock(
    slippageUM: SwapSlippageUM.Content,
    modifier: Modifier = Modifier,
    feeBlock: @Composable ((Modifier) -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        SwapSlippageRow(slippageUM = slippageUM)
        if (feeBlock != null) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = TangemTheme.colors3.border.secondary,
            )
            feeBlock(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SwapSlippageRow(slippageUM: SwapSlippageUM.Content) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = slippageUM.onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResourceSafe(R.string.common_slippage),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        SpacerWMax()
        Text(
            text = slippageUM.value.resolveReference(),
            style = TangemTheme.typography.body2,
            color = if (slippageUM.isHighlighted) {
                TangemTheme.colors3.text.status.warning
            } else {
                TangemTheme.colors.text.tertiary
            },
            modifier = Modifier.padding(end = 4.dp),
        )
        Icon(
            imageVector = Icons.ic_chevron_double_vertical_16,
            contentDescription = null,
            tint = TangemTheme.colors.icon.secondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SwapSlippageAndFeeBlock_Preview() {
    TangemThemePreview {
        SwapSlippageAndFeeBlock(
            slippageUM = SwapSlippageUM.Content(
                value = resourceReference(R.string.common_auto),
                isHighlighted = false,
                onClick = {},
            ),
            feeBlock = { modifier ->
                Text(
                    text = stringResourceSafe(R.string.common_network_fee_title),
                    modifier = modifier.padding(12.dp),
                    color = TangemTheme.colors.text.primary1,
                )
            },
        )
    }
}