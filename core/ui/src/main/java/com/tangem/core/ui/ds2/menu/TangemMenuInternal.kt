@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.animation.TangemAnimationSpec
import com.tangem.core.ui.ds2.shape.SmoothCornerShape
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_checkmark_20
import com.tangem.core.ui.test.PopUpMenuTestTags
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Figma `Has Selection` of the enclosing menu, shared by its items and dividers. */
private val LocalTangemMenuSelectable = staticCompositionLocalOf { false }

private val LocalTangemMenuVariant = staticCompositionLocalOf { TangemMenu.Variant.Button }

/** Whether the enclosing menu is animating out; its items then take no more clicks from any input method. */
internal val LocalTangemMenuClosing = compositionLocalOf { false }

private val IconSize = 20.dp

/**
 * Material container of [TangemMenu] without the popup window — also used by previews.
 *
 * @param listModifier Applied to the item list inside the material, e.g. to fade it separately from the container.
 */
@Composable
internal fun TangemMenuPanel(
    isSelectable: Boolean,
    modifier: Modifier = Modifier,
    variant: TangemMenu.Variant = TangemMenu.Variant.Button,
    listModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalTangemMenuSelectable provides isSelectable,
        LocalTangemMenuVariant provides variant,
    ) {
        TangemSurface(
            modifier = modifier,
            isMaterial = true,
            materialStyle = TangemSurface.MaterialStyle.Menu,
            shape = RoundedCornerShape(32.dp),
        ) {
            Column(
                modifier = listModifier
                    .widthIn(min = 240.dp)
                    .width(IntrinsicSize.Max)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
                    .testTag(PopUpMenuTestTags.CONTAINER),
                content = content,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
internal fun TangemMenuItemLayout(
    label: TextReference,
    onClick: () -> Unit,
    description: TextReference?,
    iconStart: ImageVector?,
    iconEnd: ImageVector?,
    variant: TangemMenu.ItemVariant,
    isEnabled: Boolean,
    isSelected: Boolean,
    interactionSource: MutableInteractionSource?,
    modifier: Modifier = Modifier,
) {
    val isSelectable = LocalTangemMenuSelectable.current
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val pressOverlay = rememberPressOverlay(
        interactionSource = resolvedInteractionSource,
        variant = LocalTangemMenuVariant.current,
    )
    val pressColor = TangemTheme.colors3.interaction.press.default
    val colors = itemColors(variant = variant, isEnabled = isEnabled)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val outline = SmoothCornerShape(
                    radius = 24.dp,
                    smoothing = 0.6f,
                ).createOutline(size = size, layoutDirection = layoutDirection, density = this)
                onDrawBehind {
                    val overlayAlpha = pressOverlay.value
                    if (overlayAlpha > 0f) drawOutline(outline = outline, color = pressColor, alpha = overlayAlpha)
                }
            }
            .clickable(
                interactionSource = resolvedInteractionSource,
                indication = null,
                enabled = isEnabled && !LocalTangemMenuClosing.current,
                role = Role.Button,
                onClick = onClick,
            )
            .conditionalCompose(isSelectable) { semantics { selected = isSelected } }
            .padding(10.dp)
            .testTag(PopUpMenuTestTags.BUTTON),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectable) {
            SelectionMark(isSelected = isSelected)
        }
        if (iconStart != null) {
            MenuIcon(imageVector = iconStart, tint = colors.iconStart)
        }
        ItemTexts(
            label = label,
            description = description,
            colors = colors,
            modifier = Modifier.weight(1f),
        )
        if (iconEnd != null) {
            MenuIcon(imageVector = iconEnd, tint = colors.iconEnd)
        }
    }
}

@Composable
private fun SelectionMark(isSelected: Boolean) {
    Box(modifier = Modifier.size(IconSize)) {
        // Qualified: inside the item Row the RowScope overload would be picked.
        androidx.compose.animation.AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(initialScale = 0.5f, animationSpec = TangemAnimationSpec.Bouncy) + fadeIn(),
            exit = scaleOut(targetScale = 0.5f) + fadeOut(),
            label = "checkmark",
        ) {
            MenuIcon(imageVector = Icons.ic_checkmark_20, tint = TangemTheme.colors3.icon.primary)
        }
    }
}

@Composable
private fun ItemTexts(
    label: TextReference,
    description: TextReference?,
    colors: ItemColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (description != null) {
            Text(
                text = description.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = colors.description,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun TangemMenuDividerLayout(title: TextReference?, modifier: Modifier = Modifier) {
    val isSelectable = LocalTangemMenuSelectable.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
        if (title != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(if (isSelectable) 12.dp else 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isSelectable) {
                    Spacer(modifier = Modifier.width(IconSize))
                }
                Text(
                    text = title.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.tertiary,
                )
            }
        }
    }
}

/**
 * Opacity of the item's press overlay: fades in on press and out on release, and never cuts the fade-in short, so
 * a quick tap — one that also closes the menu — still shows the whole highlight.
 */
@Composable
private fun rememberPressOverlay(
    interactionSource: InteractionSource,
    variant: TangemMenu.Variant,
): Animatable<Float, AnimationVector1D> {
    val overlay = remember { Animatable(0f) }
    val (fadeInEasing, fadeOutEasing) = when (variant) {
        TangemMenu.Variant.Button -> CubicBezierEasing(a = 0.5f, b = 0f, c = 0.5f, d = 1f).let { it to it }
        TangemMenu.Variant.Row -> EaseOut to EaseIn
    }
    LaunchedEffect(interactionSource, variant) {
        var fadeIn: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> fadeIn = launch {
                    overlay.animateTo(1f, tween(durationMillis = 100, easing = fadeInEasing))
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch {
                    fadeIn?.join()
                    overlay.animateTo(0f, tween(durationMillis = 100, easing = fadeOutEasing))
                }
            }
        }
    }
    return overlay
}

@Composable
private fun MenuIcon(imageVector: ImageVector, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(IconSize),
    )
}

private class ItemColors(
    val iconStart: Color,
    val label: Color,
    val description: Color,
    val iconEnd: Color,
)

@Composable
@ReadOnlyComposable
private fun itemColors(variant: TangemMenu.ItemVariant, isEnabled: Boolean): ItemColors {
    val colors = TangemTheme.colors3
    return when {
        !isEnabled -> ItemColors(
            iconStart = colors.icon.tertiary,
            label = colors.text.tertiary,
            description = colors.text.tertiary,
            iconEnd = colors.icon.tertiary,
        )
        variant == TangemMenu.ItemVariant.Error -> ItemColors(
            iconStart = colors.icon.status.error,
            label = colors.text.status.error,
            description = colors.text.status.error,
            iconEnd = colors.icon.primary,
        )
        else -> ItemColors(
            iconStart = colors.icon.primary,
            label = colors.text.primary,
            description = colors.text.secondary,
            iconEnd = colors.icon.primary,
        )
    }
}