package com.tangem.common.ui.cosign

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.R
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.core.ui.res.generated.icons.ic_info_20

private val RowShape = RoundedCornerShape(24.dp)
private val AvatarSize = 20.dp
private val AvatarRadius = AvatarSize / 2
private val AvatarOverlap = 12.dp
private val AvatarCutoutGap = 2.dp
private val AvatarIconSize = 16.dp

/** Compact cosign request row with a signature-progress avatar stack (Figma: "💠 Row"). */
@Composable
fun CosignRequestRow(state: CosignRequestRowUM, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier.fillMaxWidth(),
        color = TangemTheme.colors3.bg.status.warningSubtle,
        shape = RowShape,
        onClick = state.onClick,
    ) {
        TangemRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            includeInnerPaddings = false,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            titleSlot = {
                Text(
                    text = state.title.resolveAnnotatedReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (state.onInfoClick != null) {
                    Icon(
                        imageVector = Icons.ic_info_20,
                        contentDescription = stringResourceSafe(R.string.joint_operation_signers_title),
                        tint = TangemTheme.colors3.icon.secondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickableSingle(role = Role.Button, onClick = state.onInfoClick),
                    )
                }
            },
            endSlot = { SignatureAvatarStack(progress = state.progress) },
        )
    }
}

/**
 * Overlapping avatar stack for [progress.total] signers, the first [progress.collected] shown as
 * collected (filled orange) — only the frontmost avatar (index 0) also shows the checkmark, since
 * every other one is at least partly covered by the avatar in front of it. Rendering beyond 5
 * signers is not specified in Figma and is not handled here.
 */
@Composable
private fun SignatureAvatarStack(progress: SignatureProgressUM, modifier: Modifier = Modifier) {
    val total = progress.total.coerceIn(1, MAX_COSIGN_SIGNERS)
    val collected = progress.collected.coerceIn(0, total)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-AvatarOverlap),
    ) {
        repeat(total) { index ->
            SignatureAvatar(
                isCollected = index < collected,
                hasCutout = index > 0,
                modifier = Modifier.zIndex((total - index).toFloat()),
            )
        }
    }
}

/**
 * [hasCutout] punches a crescent-moon bite out of this avatar where the preceding (higher z-index)
 * avatar overlaps it, leaving an [AvatarCutoutGap]-wide sliver of the row background as a visible gap.
 */
@Composable
private fun SignatureAvatar(isCollected: Boolean, hasCutout: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(AvatarSize)
            .conditionalCompose(hasCutout) {
                graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            color = Color.Transparent,
                            radius = (AvatarRadius + AvatarCutoutGap).toPx(),
                            center = Offset(x = (AvatarOverlap - AvatarRadius).toPx(), y = AvatarRadius.toPx()),
                            blendMode = BlendMode.Clear,
                        )
                    }
            }
            .clip(CircleShape)
            .background(
                if (isCollected) TangemTheme.colors3.bg.status.warning else TangemTheme.colors3.bg.opaque.secondary,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val isCheckmarkVisible = isCollected && !hasCutout
        if (isCheckmarkVisible) {
            Icon(
                imageVector = Icons.ic_checkmark_20,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.inverse,
                modifier = Modifier.size(AvatarIconSize),
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CosignRequestRowPreview() {
    TangemThemePreviewRedesign {
        CosignRequestRow(
            state = CosignRequestRowUM(
                title = stringReference("Signatures 2 of 5"),
                progress = SignatureProgressUM(collected = 2, total = 5),
                onInfoClick = {},
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
// endregion