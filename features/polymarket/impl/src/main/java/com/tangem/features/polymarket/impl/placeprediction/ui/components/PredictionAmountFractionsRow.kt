package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.common.formatPolymarketPercent
import com.tangem.features.polymarket.impl.placeprediction.entity.BuyAmountFractionUM

@Composable
internal fun PredictionAmountFractionsRow(
    onFractionClick: (BuyAmountFractionUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BuyAmountFractionUM.entries.forEach { fraction ->
            TangemButton(
                modifier = Modifier.weight(1f),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X8,
                text = fraction.label(),
                onClick = { onFractionClick(fraction) },
            )
        }
    }
}

private fun BuyAmountFractionUM.label(): TextReference = when (this) {
    BuyAmountFractionUM.Max -> resourceReference(R.string.send_max_amount)
    else -> stringReference(percent.formatPolymarketPercent())
}