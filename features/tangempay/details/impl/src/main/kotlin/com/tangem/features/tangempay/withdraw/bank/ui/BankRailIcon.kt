package com.tangem.features.tangempay.withdraw.bank.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.features.tangempay.details.impl.R

/**
 * Rail mark from the Figma rail rows. SPEI shows the Mexican flag; ACH and FedWire show a rounded square in the rail's
 * brand colour with a short label, since the fills are brand constants with no design-system equivalent and FedWire has
 * no vector mark yet.
 */
@Composable
internal fun BankRailIcon(rail: BankRail, size: Dp, modifier: Modifier = Modifier) {
    when (rail) {
        BankRail.ACH -> LabelMark(
            label = ACH_LABEL,
            brandColor = Color(ACH_BRAND_COLOR),
            labelColor = TangemTheme.colors3.icon.staticDark,
            size = size,
            modifier = modifier,
        )
        BankRail.WIRE -> LabelMark(
            label = FEDWIRE_LABEL,
            brandColor = Color(FEDWIRE_BRAND_COLOR),
            labelColor = TangemTheme.colors3.icon.staticLight,
            size = size,
            modifier = modifier,
        )
        BankRail.SPEI -> Image(
            painter = painterResource(R.drawable.img_flag_mexico_20),
            contentDescription = null,
            modifier = modifier.size(size),
        )
    }
}

@Composable
private fun LabelMark(label: String, brandColor: Color, labelColor: Color, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * CORNER_RATIO))
            .background(brandColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = labelColor,
            fontSize = (size.value * LABEL_RATIO).sp,
            lineHeight = (size.value * LABEL_RATIO).sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private const val ACH_BRAND_COLOR = 0xFF704AF1
private const val FEDWIRE_BRAND_COLOR = 0xFFFBEE76
private const val ACH_LABEL = "ACH"
private const val FEDWIRE_LABEL = "Fed"
private const val CORNER_RATIO = 0.225f
private const val LABEL_RATIO = 0.325f