@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.cryptochart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_up_12
import com.tangem.core.ui.res.generated.icons.ic_chevron_down_12

@Composable
internal fun BoxScope.CryptoChartTarget(
    price: Double,
    label: TextReference,
    frame: State<CryptoChartFrame>,
    labelHeight: Dp,
) {
    val placement by remember(price, frame) {
        derivedStateOf {
            val scale = frame.value.scale
            when {
                price > scale.max -> 1
                price < scale.min -> -1
                else -> 0
            }
        }
    }
    val targetColor = TangemTheme.colors3.border.secondary
    BoxWithConstraints(Modifier.matchParentSize()) {
        val pillMaxWidth = maxWidth * 0.8f
        Row(
            Modifier.fillMaxWidth().offset {
                val y = labelHeight / 2 + (maxHeight - labelHeight) * frame.value.scale.y(price)
                val captionHeight = labelHeight + 16.dp
                IntOffset(0, (y - captionHeight / 2).coerceIn(0.dp, maxHeight - captionHeight).roundToPx())
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TangemSurface(
                modifier = Modifier.widthIn(max = pillMaxWidth),
                isMaterial = true,
                shape = RoundedCornerShape(50),
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = label.resolveReference(),
                        modifier = Modifier.weight(1f, fill = false),
                        color = TangemTheme.colors3.text.primary,
                        style = TangemTheme.typography3.caption.medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (placement != 0) {
                        Icon(
                            imageVector = if (placement > 0) Icons.ic_chevron_up_12 else Icons.ic_chevron_down_12,
                            contentDescription = null,
                            tint = TangemTheme.colors3.icon.primary,
                        )
                    }
                }
            }
            Canvas(Modifier.weight(1f).height(1.dp)) {
                if (placement == 0) {
                    drawLine(
                        color = targetColor,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
        }
    }
}