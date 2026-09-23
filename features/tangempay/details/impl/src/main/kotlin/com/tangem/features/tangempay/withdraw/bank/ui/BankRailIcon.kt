package com.tangem.features.tangempay.withdraw.bank.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.pay.model.bankofframp.BankRail

/**
 * Rail mark from the Figma rail rows: a rounded square in the rail's brand colour with a short label. The fills are
 * brand constants with no design-system equivalent, and FedWire has no vector mark yet, hence the text label.
 */
@Composable
internal fun BankRailIcon(rail: BankRail, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * CORNER_RATIO))
            .background(rail.brandColor()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rail.label(),
            color = rail.labelColor(),
            fontSize = (size.value * LABEL_RATIO).sp,
            lineHeight = (size.value * LABEL_RATIO).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private fun BankRail.brandColor(): Color = when (this) {
    BankRail.ACH -> Color(ACH_BRAND_COLOR)
    BankRail.WIRE -> Color(FEDWIRE_BRAND_COLOR)
}

@Composable
private fun BankRail.labelColor(): Color = when (this) {
    BankRail.ACH -> TangemTheme.colors3.icon.staticDark
    BankRail.WIRE -> TangemTheme.colors3.icon.staticLight
}

private fun BankRail.label(): String = when (this) {
    BankRail.ACH -> ACH_LABEL
    BankRail.WIRE -> FEDWIRE_LABEL
}

private const val ACH_BRAND_COLOR = 0xFF704AF1
private const val FEDWIRE_BRAND_COLOR = 0xFFFBEE76
private const val ACH_LABEL = "ACH"
private const val FEDWIRE_LABEL = "Fed"
private const val CORNER_RATIO = 0.225f
private const val LABEL_RATIO = 0.325f