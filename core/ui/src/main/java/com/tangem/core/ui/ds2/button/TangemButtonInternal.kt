package com.tangem.core.ui.ds2.button

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.rememberLastNonNull
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

/**
 * Inner content of [TangemButton]: an icon-text-icon row with a cross-fading loader overlay.
 *
 * Designed to be hosted inside a [com.tangem.core.ui.ds2.surface.TangemSurface] which owns sizing,
 * shape, color, border and click handling. This composable only renders the content.
 */
@Suppress("LongParameterList")
@Composable
internal fun TangemButtonInternal(
    isIconOnly: Boolean,
    isEnabled: Boolean,
    isLoading: Boolean,
    iconStart: TangemIconUM?,
    iconEnd: TangemIconUM?,
    text: TextReference?,
    colorTokens: ColorTokens,
    sizeTokens: SizeTokens,
    modifier: Modifier = Modifier,
) {
    val resolvedIconStart = iconStart?.resolveTint(colorTokens, isEnabled)
    val resolvedIconEnd = iconEnd?.resolveTint(colorTokens, isEnabled)

    val contentAlpha by animateFloatAsState(if (isLoading) 0f else 1f, label = "contentAlpha")
    val loaderAlpha by animateFloatAsState(if (isLoading) 1f else 0f, label = "loaderAlpha")

    Box(
        modifier = modifier
            .conditionalCompose(
                condition = isIconOnly,
                otherModifier = {
                    height(sizeTokens.minHeight)
                        .widthIn(min = sizeTokens.minWidth)
                },
                modifier = { size(sizeTokens.minSizeIconOnly) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        ContentRow(
            modifier = Modifier
                .alpha(contentAlpha)
                .padding(
                    horizontal = sizeTokens.containerHorizontalPadding,
                    vertical = sizeTokens.containerVerticalPadding,
                ),
            isEnabled = isEnabled,
            iconStart = resolvedIconStart,
            iconEnd = resolvedIconEnd,
            text = text,
            colorTokens = colorTokens,
            sizeTokens = sizeTokens,
        )

        if (loaderAlpha > 0f) {
            TangemLoader(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(loaderAlpha),
                color = if (isEnabled) colorTokens.iconTint else colorTokens.disabledIconTint,
            )
        }
    }
}

/**
 * Row of `[iconStart] [text] [iconEnd]` where each slot animates in/out independently.
 *
 * Last non-null values are cached so that `AnimatedVisibility` exit transitions still have content
 * to render once the caller flips a slot back to `null`.
 */
@Suppress("LongParameterList")
@Composable
private fun ContentRow(
    isEnabled: Boolean,
    iconStart: TangemIconUM?,
    iconEnd: TangemIconUM?,
    text: TextReference?,
    colorTokens: ColorTokens,
    sizeTokens: SizeTokens,
    modifier: Modifier = Modifier,
) {
    val displayedIconStart = rememberLastNonNull(iconStart)
    val displayedIconEnd = rememberLastNonNull(iconEnd)
    val displayedText = rememberLastNonNull(text)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = iconStart != null,
            enter = SlotEnterTransition,
            exit = SlotExitTransition,
        ) {
            displayedIconStart?.let { icon ->
                TangemIcon(
                    modifier = Modifier.size(sizeTokens.iconSize),
                    tangemIconUM = icon,
                )
            }
        }

        AnimatedVisibility(
            visible = text != null,
            enter = SlotEnterTransition,
            exit = SlotExitTransition,
        ) {
            displayedText?.let { textRef ->
                CompositionLocalProvider(LocalDensity provides cappedFontScaleDensity()) {
                    Text(
                        modifier = Modifier.padding(horizontal = sizeTokens.textPadding),
                        text = textRef.resolveReference(),
                        textAlign = TextAlign.Center,
                        color = if (isEnabled) colorTokens.textColor else colorTokens.disabledTextColor,
                        style = TangemTheme.typography3.body.medium.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = iconEnd != null,
            enter = SlotEnterTransition,
            exit = SlotExitTransition,
        ) {
            displayedIconEnd?.let { icon ->
                TangemIcon(
                    modifier = Modifier.size(sizeTokens.iconSize),
                    tangemIconUM = icon,
                )
            }
        }
    }
}

/**
 * Returns a [Density] derived from [LocalDensity] with [Density.fontScale] capped at
 * [MAX_BUTTON_FONT_SCALE]. The button's height is fixed by design tokens, so unbounded user font
 * scales would clip the label vertically; capping the scale keeps the text visible while still
 * honoring user preferences up to a point. When the user's scale is already within the cap, the
 * current [LocalDensity] is returned unchanged.
 */
@Composable
private fun cappedFontScaleDensity(): Density {
    val baseDensity = LocalDensity.current
    return remember(baseDensity.density, baseDensity.fontScale) {
        if (baseDensity.fontScale <= MAX_BUTTON_FONT_SCALE) {
            baseDensity
        } else {
            Density(density = baseDensity.density, fontScale = MAX_BUTTON_FONT_SCALE)
        }
    }
}

private const val MAX_BUTTON_FONT_SCALE = 1.3f

// Shared, snappy specs so size and alpha animations stay in sync across the three slots.
private val SlotSizeSpec = spring<IntSize>(stiffness = Spring.StiffnessMediumLow)
private val SlotAlphaSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val SlotEnterTransition: EnterTransition =
    fadeIn(animationSpec = SlotAlphaSpec) + expandHorizontally(animationSpec = SlotSizeSpec)
private val SlotExitTransition: ExitTransition =
    fadeOut(animationSpec = SlotAlphaSpec) + shrinkHorizontally(animationSpec = SlotSizeSpec)

/**
 * Forces the variant's icon color (or its disabled variant when [isEnabled] is `false`) onto
 * [TangemIconUM.Icon] — the button's variant always drives icon color, so any caller-supplied
 * tint is overridden. Other icon types pass through unchanged.
 *
 * Note: we cannot honor a caller-supplied tint conditionally, because [TangemIconUM.Icon]'s
 * convenience constructor defaults `tint` to a non-null `ColorReference2`, making "no tint
 * supplied" indistinguishable from "tint explicitly set" at the call site.
 */
@Composable
private fun TangemIconUM.resolveTint(colorTokens: ColorTokens, isEnabled: Boolean): TangemIconUM {
    return when (this) {
        is TangemIconUM.Icon -> copy(
            tint = ColorReference2 {
                if (isEnabled) colorTokens.iconTint else colorTokens.disabledIconTint
            },
        )
        else -> this
    }
}

/** Resolved per-variant colors used by [TangemButton]. */
internal data class ColorTokens(
    val backgroundColor: Color,
    val textColor: Color,
    val iconTint: Color,
    val disabledBackgroundColor: Color,
    val disabledTextColor: Color,
    val disabledIconTint: Color,
    val focusRingColor: Color,
    val disabledAlpha: Float = 1f,
    val defaultBorderColor: Color? = null,
)

@Suppress("LongMethod")
@Composable
@ReadOnlyComposable
internal fun TangemButton.Variant.tokens(): ColorTokens {
    return when (this) {
        TangemButton.Variant.Brand -> ColorTokens(
            backgroundColor = TangemTheme.colors3.bg.brand,
            textColor = TangemTheme.colors3.text.staticDark.primary,
            iconTint = TangemTheme.colors3.icon.staticDark,
            disabledBackgroundColor = TangemTheme.colors3.bg.disabled,
            disabledTextColor = TangemTheme.colors3.text.tertiary,
            disabledIconTint = TangemTheme.colors3.icon.tertiary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.default,
        )
        TangemButton.Variant.Primary -> ColorTokens(
            backgroundColor = TangemTheme.colors3.bg.inverse,
            textColor = TangemTheme.colors3.text.inverse.primary,
            iconTint = TangemTheme.colors3.icon.inverse,
            disabledBackgroundColor = TangemTheme.colors3.bg.disabled,
            disabledTextColor = TangemTheme.colors3.text.tertiary,
            disabledIconTint = TangemTheme.colors3.icon.tertiary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
        )
        TangemButton.Variant.Secondary -> ColorTokens(
            backgroundColor = TangemTheme.colors3.bg.opaque.primary,
            textColor = TangemTheme.colors3.text.primary,
            iconTint = TangemTheme.colors3.icon.primary,
            disabledBackgroundColor = TangemTheme.colors3.bg.opaque.primary,
            disabledTextColor = TangemTheme.colors3.text.primary,
            disabledIconTint = TangemTheme.colors3.icon.primary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
            disabledAlpha = TangemTheme.dimens3.opacity.disabled,
        )
        TangemButton.Variant.Material -> ColorTokens(
            // Background is the haze fill (FILL/MATERIAL) rendered by TangemSurface when isMaterial = true;
            // this slot is unused in that path.
            backgroundColor = Color.Transparent,
            textColor = TangemTheme.colors3.text.primary,
            iconTint = TangemTheme.colors3.icon.primary,
            disabledBackgroundColor = Color.Transparent,
            disabledTextColor = TangemTheme.colors3.text.tertiary,
            disabledIconTint = TangemTheme.colors3.icon.tertiary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
        )
        TangemButton.Variant.Success -> ColorTokens(
            backgroundColor = TangemTheme.colors3.bg.status.success,
            textColor = TangemTheme.colors3.text.staticDark.primary,
            iconTint = TangemTheme.colors3.icon.staticDark,
            disabledBackgroundColor = TangemTheme.colors3.bg.status.success,
            disabledTextColor = TangemTheme.colors3.text.staticDark.primary,
            disabledIconTint = TangemTheme.colors3.icon.staticDark,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.default,
            disabledAlpha = TangemTheme.dimens3.opacity.disabled,
        )
        TangemButton.Variant.Outline -> ColorTokens(
            backgroundColor = Color.Transparent,
            textColor = TangemTheme.colors3.text.primary,
            iconTint = TangemTheme.colors3.icon.primary,
            disabledBackgroundColor = Color.Transparent,
            disabledTextColor = TangemTheme.colors3.text.primary,
            disabledIconTint = TangemTheme.colors3.icon.primary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
            disabledAlpha = TangemTheme.dimens3.opacity.disabled,
            defaultBorderColor = TangemTheme.colors3.border.secondary,
        )
        TangemButton.Variant.Ghost -> ColorTokens(
            backgroundColor = Color.Transparent,
            textColor = TangemTheme.colors3.text.primary,
            iconTint = TangemTheme.colors3.icon.primary,
            disabledBackgroundColor = Color.Transparent,
            disabledTextColor = TangemTheme.colors3.text.primary,
            disabledIconTint = TangemTheme.colors3.icon.primary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
            disabledAlpha = TangemTheme.dimens3.opacity.disabled,
        )
    }
}

/** Resolved per-size dimensions used by [TangemButton]. */
internal data class SizeTokens(
    val minHeight: Dp,
    val minWidth: Dp,
    val minSizeIconOnly: Dp,
    val textPadding: Dp,
    val containerHorizontalPadding: Dp,
    val containerVerticalPadding: Dp,
    val iconSize: Dp,
)

@Composable
@ReadOnlyComposable
internal fun TangemButton.Size.tokens(): SizeTokens {
    val dimens = TangemTheme.dimens3
    return when (this) {
        TangemButton.Size.X14 -> SizeTokens(
            minHeight = dimens.size.s700,
            minWidth = dimens.size.s1100,
            minSizeIconOnly = dimens.size.s700,
            textPadding = dimens.spacing.s100,
            containerHorizontalPadding = dimens.spacing.s200,
            containerVerticalPadding = dimens.spacing.s200,
            iconSize = 24.dp,
        )
        TangemButton.Size.X12 -> SizeTokens(
            minHeight = dimens.size.s600,
            minWidth = dimens.size.s1000,
            minSizeIconOnly = dimens.size.s600,
            textPadding = dimens.spacing.s100,
            containerHorizontalPadding = dimens.spacing.s150,
            containerVerticalPadding = dimens.spacing.s150,
            iconSize = 24.dp,
        )
        TangemButton.Size.X11 -> SizeTokens(
            minHeight = dimens.size.s550,
            minWidth = dimens.size.s900,
            minSizeIconOnly = dimens.size.s550,
            textPadding = dimens.spacing.s075,
            containerHorizontalPadding = dimens.spacing.s150,
            containerVerticalPadding = dimens.spacing.s150,
            iconSize = 20.dp,
        )
        TangemButton.Size.X10 -> SizeTokens(
            minHeight = dimens.size.s500,
            minWidth = dimens.size.s800,
            minSizeIconOnly = dimens.size.s500,
            textPadding = dimens.spacing.s075,
            containerHorizontalPadding = dimens.spacing.s125,
            containerVerticalPadding = dimens.spacing.s125,
            iconSize = 20.dp,
        )
        TangemButton.Size.X9 -> SizeTokens(
            minHeight = dimens.size.s450,
            minWidth = dimens.size.s700,
            minSizeIconOnly = dimens.size.s450,
            textPadding = dimens.spacing.s075,
            containerHorizontalPadding = dimens.spacing.s100,
            containerVerticalPadding = dimens.spacing.s100,
            iconSize = 20.dp,
        )
        TangemButton.Size.X8 -> SizeTokens(
            minHeight = dimens.size.s400,
            minWidth = dimens.size.s600,
            minSizeIconOnly = dimens.size.s400,
            textPadding = dimens.spacing.s075,
            containerHorizontalPadding = dimens.spacing.s075,
            containerVerticalPadding = dimens.spacing.s075,
            iconSize = 20.dp,
        )
        TangemButton.Size.X7 -> SizeTokens(
            minHeight = dimens.size.s350,
            minWidth = dimens.size.s500,
            minSizeIconOnly = dimens.size.s350,
            textPadding = dimens.spacing.s075,
            containerHorizontalPadding = dimens.spacing.s075,
            containerVerticalPadding = dimens.spacing.s050,
            iconSize = 16.dp,
        )
    }
}