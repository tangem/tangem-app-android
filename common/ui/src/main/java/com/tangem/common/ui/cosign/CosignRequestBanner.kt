package com.tangem.common.ui.cosign

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20

private val RingSize = 40.dp
private val RingStrokeWidth = 3.dp
private val RingSegmentGap = 3.dp
private const val RING_FULL_CIRCLE_DEGREES = 360f
private const val RING_START_ANGLE_DEGREES = -90f

@Composable
fun CosignRequestBanner(state: CosignRequestBannerUM, modifier: Modifier = Modifier) {
    val slotEnd: (@Composable () -> Unit)? = when {
        state.progress != null -> {
            { SignatureProgressRing(progress = state.progress) }
        }
        state.trailingIcon != null -> {
            { TangemIcon(tangemIconUM = state.trailingIcon) }
        }
        else -> null
    }
    TangemMessageBanner(
        modifier = modifier,
        title = state.title,
        description = state.subtitle,
        variant = TangemMessageBanner.Variant.Warning,
        onClick = state.onClick,
        slotEnd = slotEnd,
    )
}

/**
 * Circular "collected/total" signature progress badge (Figma: banner trailing slot, progress-ring
 * instance). The ring is split into [progress.total] equal segments separated by a [RingSegmentGap]
 * gap — the first [progress.collected] segments are drawn in the accent color, the rest as track.
 */
@Composable
private fun SignatureProgressRing(progress: SignatureProgressUM, modifier: Modifier = Modifier) {
    val total = progress.total.coerceIn(1, MAX_COSIGN_SIGNERS)
    val collected = progress.collected.coerceIn(0, total)
    val trackColor = TangemTheme.colors3.border.secondary
    val progressColor = TangemTheme.colors3.border.status.warning
    Box(
        modifier = modifier.size(RingSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(RingSize)) {
            val strokeWidthPx = RingStrokeWidth.toPx()
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            val radiusPx = arcSize.width / 2f
            val gapPx = RingSegmentGap.toPx() + strokeWidthPx
            val gapDegrees = Math.toDegrees((gapPx / radiusPx).toDouble()).toFloat()
            val segmentDegrees = (RING_FULL_CIRCLE_DEGREES / total - gapDegrees).coerceAtLeast(0f)
            repeat(total) { index ->
                drawArc(
                    color = if (index < collected) progressColor else trackColor,
                    startAngle = RING_START_ANGLE_DEGREES +
                        index * (RING_FULL_CIRCLE_DEGREES / total) +
                        gapDegrees / 2f,
                    sweepAngle = segmentDegrees,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "$collected/$total",
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.status.warning,
        )
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CosignRequestBannerPreview(
    @PreviewParameter(CosignRequestBannerPreviewProvider::class) state: CosignRequestBannerUM,
) {
    TangemThemePreviewRedesign {
        CosignRequestBanner(state = state)
    }
}

private class CosignRequestBannerPreviewProvider : CollectionPreviewParameterProvider<CosignRequestBannerUM>(
    collection = listOf(
        CosignRequestBannerUM(
            title = stringReference("3 of 5 signatures required"),
            subtitle = stringReference("Sends automatically once collected"),
            trailingIcon = TangemIconUM.Icon(
                imageVector = Icons.ic_info_20,
                tintReference = { TangemTheme.colors3.icon.secondary },
            ),
        ),
        CosignRequestBannerUM(
            title = stringReference("Jacob wants to send Ethereum"),
            subtitle = stringReference("0.5 ETH → 0x1a2...b3c4"),
            progress = SignatureProgressUM(collected = 2, total = 3),
        ),
    ),
)
// endregion