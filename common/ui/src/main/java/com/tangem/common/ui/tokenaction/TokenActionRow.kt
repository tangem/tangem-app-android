package com.tangem.common.ui.tokenaction

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme

private const val ACTION_BACKGROUND_ALPHA = .1f

/**
 * Single-row token action ("Buy", "Receive", etc.) with accent icon, title, description and a
 * customizable tail. Used in bottom sheets like Get Token / Add Funds and Add To Portfolio.
 *
 * @param iconRes        leading 20dp icon drawn over an accent-colored circle
 * @param title          row primary text
 * @param description    row secondary text
 * @param onClick        single-click callback; row is non-interactive if `null`. Fires regardless
 *                       of [isEnabled] — gating is the caller's responsibility (pass `null` to
 *                       make the row non-interactive)
 * @param onLongClick    long-press callback; pass `null` to disable long-press. Fires regardless
 *                       of [isEnabled]
 * @param isEnabled      controls visual styling only (disabled-tier colors when `false`)
 * @param tailContent    content placed at the row's end. Defaults to a chevron-right icon.
 */
@Composable
fun TokenActionRow(
    @DrawableRes iconRes: Int,
    title: TextReference,
    description: TextReference,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    tailContent: @Composable () -> Unit = { DefaultTokenActionRowChevron(isEnabled = isEnabled) },
) {
    val hapticManager = LocalHapticManager.current
    val accentColor = accentColor(isEnabled)
    TangemRowContainer(
        modifier = modifier
            .background(
                color = TangemTheme.colors2.surface.level3,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            )
            .clickableWithHaptic(
                onClick = onClick,
                onLongClick = onLongClick,
                hapticManager = hapticManager,
            ),
    ) {
        LeadingIcon(iconRes = iconRes, accentColor = accentColor)
        Text(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_TOP),
            text = title.resolveReference(),
            style = TangemTheme.typography2.bodyMedium16,
            color = titleColor(isEnabled),
        )
        Text(
            modifier = Modifier.layoutId(TangemRowLayoutId.START_BOTTOM),
            text = description.resolveReference(),
            style = TangemTheme.typography2.captionMedium12,
            color = descriptionColor(isEnabled),
        )
        Tail { tailContent() }
    }
}

@Composable
private fun LeadingIcon(@DrawableRes iconRes: Int, accentColor: Color) {
    Box(
        modifier = Modifier
            .layoutId(TangemRowLayoutId.HEAD)
            .padding(end = TangemTheme.dimens2.x3)
            .size(40.dp)
            .background(
                color = accentColor.copy(alpha = ACTION_BACKGROUND_ALPHA),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = null,
            tint = accentColor,
        )
    }
}

@Composable
private fun Tail(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .layoutId(TangemRowLayoutId.TAIL)
            .padding(start = TangemTheme.dimens2.x2)
            .size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun accentColor(isEnabled: Boolean): Color = if (isEnabled) {
    TangemTheme.colors2.graphic.status.accent
} else {
    TangemTheme.colors2.graphic.neutral.quaternary
}

@Composable
private fun titleColor(isEnabled: Boolean): Color = if (isEnabled) {
    TangemTheme.colors2.text.neutral.primary
} else {
    TangemTheme.colors2.text.status.disabled
}

@Composable
private fun descriptionColor(isEnabled: Boolean): Color = if (isEnabled) {
    TangemTheme.colors2.text.neutral.secondary
} else {
    TangemTheme.colors2.text.status.disabled
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.clickableWithHaptic(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    hapticManager: HapticManager,
): Modifier {
    if (onClick == null) return this
    return combinedClickable(
        onClick = hapticManager.withHaptic(TangemHapticEffect.View.SegmentTick, onClick),
        onLongClick = onLongClick?.let { hapticManager.withHaptic(TangemHapticEffect.View.LongPress, it) },
    )
}

private fun HapticManager.withHaptic(effect: TangemHapticEffect, action: () -> Unit): () -> Unit = {
    perform(effect)
    action()
}

@Composable
private fun DefaultTokenActionRowChevron(isEnabled: Boolean) {
    Icon(
        modifier = Modifier.size(24.dp),
        imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_small_right_24),
        tint = if (isEnabled) {
            TangemTheme.colors2.graphic.neutral.tertiary
        } else {
            TangemTheme.colors2.graphic.neutral.quaternary
        },
        contentDescription = null,
    )
}