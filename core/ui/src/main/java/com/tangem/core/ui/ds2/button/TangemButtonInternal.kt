package com.tangem.core.ui.ds2.button

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.animation.TangemTransition
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.BaseButtonTestTags
import kotlinx.coroutines.launch

/**
 * Shared body of [TangemButton] and [TangemButton.HoldToConfirm]: the [TangemSurface] shell with
 * semantics, focus ring and disabled fade around [TangemButtonInternal].
 *
 * Exactly one of [onClick] and [hold] drives the interaction. With [hold] the surface is not
 * clickable (no press ripple — the hold overlay is the press feedback), the background is painted
 * here so the overlay can sit between it and the content, and the confirm action is exposed to
 * accessibility services as the click action.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun TangemButtonContainer(
    variant: TangemButton.Variant,
    size: TangemButton.Size,
    isLoading: Boolean,
    isEnabled: Boolean,
    isDimmed: Boolean,
    iconStart: TangemIconUM?,
    iconEnd: TangemIconUM?,
    text: TextReference?,
    loadingText: TextReference?,
    contentDescription: String?,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    hold: HoldToConfirmSpec? = null,
) {
    val isLoaderInline = isLoading && loadingText != null
    val isIconOnly = text == null && !isLoaderInline
    val shouldShowLoader = isLoading || iconStart == null && iconEnd == null && text == null
    val colorTokens = variant.tokens()
    val sizeTokens = size.tokens()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val hasEnabledAppearance = isEnabled && !isDimmed
    val disabledAlpha = if (hasEnabledAppearance) 1f else colorTokens.disabledAlpha
    val backgroundColor =
        if (hasEnabledAppearance) colorTokens.backgroundColor else colorTokens.disabledBackgroundColor
    val accessibilityLabel = contentDescription ?: resolveLoadingLabel(shouldShowLoader, loadingText, text)

    TangemSurface(
        modifier = modifier
            .buttonSemantics(isEnabled = isEnabled, label = accessibilityLabel, hold = hold)
            .conditional(isFocused) {
                border(width = 2.dp, color = colorTokens.focusRingColor, shape = CircleShape)
            }
            .alpha(disabledAlpha)
            .then(
                if (hold != null) {
                    Modifier.holdChrome(
                        hold = hold,
                        backgroundColor = backgroundColor,
                        isEnabled = isEnabled,
                        interactionSource = interactionSource,
                    )
                } else {
                    Modifier
                },
            ),
        onClick = onClick,
        enabled = isEnabled,
        color = if (hold != null) Color.Transparent else backgroundColor,
        border = colorTokens.defaultBorderColor?.let { BorderStroke(width = 1.dp, color = it) },
        shape = CircleShape,
        interactionSource = interactionSource,
        isMaterial = variant == TangemButton.Variant.Material,
    ) {
        TangemButtonInternal(
            isIconOnly = isIconOnly,
            isEnabled = hasEnabledAppearance,
            isLoading = shouldShowLoader,
            isLoaderInline = isLoaderInline,
            iconStart = iconStart,
            iconEnd = iconEnd,
            text = if (isLoaderInline) loadingText else text,
            colorTokens = colorTokens,
            sizeTokens = sizeTokens,
        )
    }
}

private fun Modifier.buttonSemantics(isEnabled: Boolean, label: String?, hold: HoldToConfirmSpec?): Modifier =
    semantics(mergeDescendants = true) {
        role = Role.Button
        if (!isEnabled) disabled()
        label?.let { contentDescription = it }
        if (hold?.isActionEnabled == true) {
            onClick(
                action = {
                    hold.onConfirm()
                    true
                },
            )
        }
    }

/**
 * Focusability, background and hold overlay of a hold-to-confirm button. The background is painted
 * here rather than by [TangemSurface] so the overlay lands between the fill and the content.
 */
private fun Modifier.holdChrome(
    hold: HoldToConfirmSpec,
    backgroundColor: Color,
    isEnabled: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier = focusable(enabled = isEnabled, interactionSource = interactionSource)
    .clip(CircleShape)
    .background(backgroundColor)
    .drawBehind {
        val progress = hold.progress().coerceIn(0f, 1f)
        if (progress > 0f) {
            drawRect(color = hold.overlayColor, size = size.copy(width = size.width * progress))
        }
    }

@Composable
private fun resolveLoadingLabel(isLoading: Boolean, loadingText: TextReference?, text: TextReference?): String? {
    return if (isLoading) (loadingText ?: text)?.resolveReference() else null
}

/**
 * Hold-to-confirm wiring for [TangemButtonContainer].
 *
 * @property progress Hold progress in `0..1`, read at draw time; the overlay covers that fraction
 *   of the width from the start edge.
 * @property isActionEnabled Whether accessibility services may trigger [onConfirm] directly.
 */
@Immutable
internal class HoldToConfirmSpec(
    val progress: () -> Float,
    val overlayColor: Color,
    val isActionEnabled: Boolean,
    val onConfirm: () -> Unit,
)

/**
 * Press-and-hold gesture driving [progress]: the fill runs `0 → 1` over [HOLD_DURATION_MS] while
 * the pointer stays down, then [onConfirm] fires and the fill resets. Releasing (or a parent
 * scroll stealing the pointer) before the end retracts the fill and confirms nothing.
 *
 * The fill runs in a scope outside the pointer handler, so turning [isEnabled] off mid-hold has to
 * stop it explicitly — otherwise a button that just became disabled or loading could still confirm.
 */
@Composable
internal fun rememberHoldToConfirmGesture(
    isEnabled: Boolean,
    progress: Animatable<Float, AnimationVector1D>,
    onConfirm: () -> Unit,
): Modifier {
    val scope = rememberCoroutineScope()
    val hapticManager = LocalHapticManager.current
    val currentOnConfirm by rememberUpdatedState(onConfirm)

    LaunchedEffect(isEnabled) {
        if (!isEnabled) {
            progress.stop()
            progress.snapTo(0f)
        }
    }

    return Modifier.pointerInput(isEnabled) {
        if (!isEnabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown()
            val fillJob = scope.launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = HOLD_DURATION_MS, easing = LinearEasing),
                )
                hapticManager.perform(TangemHapticEffect.View.Confirm)
                currentOnConfirm()
                progress.snapTo(0f)
            }
            waitForUpOrCancellation()
            if (fillJob.isActive) {
                fillJob.cancel()
                hapticManager.perform(TangemHapticEffect.View.Reject)
                scope.launch {
                    progress.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = RETRACT_DURATION_MS))
                }
            }
        }
    }
}

private const val HOLD_DURATION_MS = 1500
private const val RETRACT_DURATION_MS = 200

internal fun TangemButton.HoldVariant.toVariant(): TangemButton.Variant = when (this) {
    TangemButton.HoldVariant.Brand -> TangemButton.Variant.Brand
    TangemButton.HoldVariant.Primary -> TangemButton.Variant.Primary
}

@Composable
@ReadOnlyComposable
internal fun TangemButton.HoldVariant.holdOverlayColor(): Color = when (this) {
    TangemButton.HoldVariant.Brand -> TangemTheme.colors3.interaction.press.staticLight
    TangemButton.HoldVariant.Primary -> TangemTheme.colors3.interaction.press.inverse
}

/**
 * Inner content of [TangemButton]: an icon-text-icon row with a cross-fading loader overlay. With
 * [isLoaderInline] the loader takes the trailing icon slot instead, after the loading message.
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
    isLoaderInline: Boolean,
    iconStart: TangemIconUM?,
    iconEnd: TangemIconUM?,
    text: TextReference?,
    colorTokens: ColorTokens,
    sizeTokens: SizeTokens,
    modifier: Modifier = Modifier,
) {
    val resolvedIconStart = iconStart?.resolveTint(colorTokens, isEnabled)
    val resolvedIconEnd = iconEnd?.resolveTint(colorTokens, isEnabled)

    val isLoaderOverlay = isLoading && !isLoaderInline
    val contentAlpha by animateFloatAsState(if (isLoaderOverlay) 0f else 1f, label = "contentAlpha")
    val loaderAlpha by animateFloatAsState(if (isLoaderOverlay) 1f else 0f, label = "loaderAlpha")
    val loaderColor = if (isEnabled) colorTokens.iconTint else colorTokens.disabledIconTint

    Box(
        modifier = modifier
            .testTag(BaseButtonTestTags.BUTTON)
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
            isLoaderInline = isLoaderInline,
            loaderColor = loaderColor,
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
                color = loaderColor,
            )
        }
    }
}

/**
 * Row of `[iconStart] [text] [iconEnd]` where each slot animates in/out independently. With
 * [isLoaderInline] the end slot shows the loader and the start slot is hidden.
 *
 * Last non-null values are cached so that `AnimatedVisibility` exit transitions still have content
 * to render once the caller flips a slot back to `null`.
 */
@Suppress("LongParameterList")
@Composable
private fun ContentRow(
    isEnabled: Boolean,
    isLoaderInline: Boolean,
    loaderColor: Color,
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
            visible = iconStart != null && !isLoaderInline,
            enter = TangemTransition.SlotEnterHorizontally,
            exit = TangemTransition.SlotExitHorizontally,
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
            enter = TangemTransition.SlotEnterHorizontally,
            exit = TangemTransition.SlotExitHorizontally,
        ) {
            displayedText?.let { textRef ->
                ButtonLabel(text = textRef, isEnabled = isEnabled, colorTokens = colorTokens, sizeTokens = sizeTokens)
            }
        }

        AnimatedVisibility(
            visible = iconEnd != null || isLoaderInline,
            enter = TangemTransition.SlotEnterHorizontally,
            exit = TangemTransition.SlotExitHorizontally,
        ) {
            if (isLoaderInline) {
                TangemLoader(color = loaderColor, size = sizeTokens.loaderSize)
            } else {
                displayedIconEnd?.let { icon ->
                    TangemIcon(
                        modifier = Modifier.size(sizeTokens.iconSize),
                        tangemIconUM = icon,
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonLabel(text: TextReference, isEnabled: Boolean, colorTokens: ColorTokens, sizeTokens: SizeTokens) {
    CompositionLocalProvider(LocalDensity provides cappedFontScaleDensity()) {
        Text(
            modifier = Modifier
                .padding(horizontal = sizeTokens.textPadding)
                .wrapContentHeight(unbounded = true)
                .testTag(BaseButtonTestTags.TEXT),
            text = text.resolveReference(),
            textAlign = TextAlign.Center,
            color = if (isEnabled) colorTokens.textColor else colorTokens.disabledTextColor,
            style = sizeTokens.textStyle.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = TangemTheme.typography3.caption.medium.fontSize,
                maxFontSize = sizeTokens.textStyle.fontSize,
            ),
        )
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
            backgroundColor = TangemTheme.colors3.bg.transparent.primary,
            textColor = TangemTheme.colors3.text.primary,
            iconTint = TangemTheme.colors3.icon.primary,
            disabledBackgroundColor = TangemTheme.colors3.bg.transparent.primary,
            disabledTextColor = TangemTheme.colors3.text.primary,
            disabledIconTint = TangemTheme.colors3.icon.primary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
            disabledAlpha = 0.4f,
        )
        TangemButton.Variant.SecondaryError -> ColorTokens(
            backgroundColor = TangemTheme.colors3.bg.status.errorSubtle,
            textColor = TangemTheme.colors3.text.accent.red,
            iconTint = TangemTheme.colors3.icon.accent.red,
            disabledBackgroundColor = TangemTheme.colors3.bg.status.errorSubtle,
            disabledTextColor = TangemTheme.colors3.text.accent.red,
            disabledIconTint = TangemTheme.colors3.icon.accent.red,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
            disabledAlpha = 0.4f,
        )
        TangemButton.Variant.SecondaryInfo -> ColorTokens(
            backgroundColor = TangemTheme.colors3.bg.status.infoSubtle,
            textColor = TangemTheme.colors3.text.accent.blue,
            iconTint = TangemTheme.colors3.icon.accent.blue,
            disabledBackgroundColor = TangemTheme.colors3.bg.status.infoSubtle,
            disabledTextColor = TangemTheme.colors3.text.accent.blue,
            disabledIconTint = TangemTheme.colors3.icon.accent.blue,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
            disabledAlpha = 0.4f,
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
            disabledAlpha = 0.4f,
        )
        TangemButton.Variant.Outline -> ColorTokens(
            backgroundColor = Color.Transparent,
            textColor = TangemTheme.colors3.text.primary,
            iconTint = TangemTheme.colors3.icon.primary,
            disabledBackgroundColor = Color.Transparent,
            disabledTextColor = TangemTheme.colors3.text.primary,
            disabledIconTint = TangemTheme.colors3.icon.primary,
            focusRingColor = TangemTheme.colors3.interaction.focusRing.brand,
            disabledAlpha = 0.4f,
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
            disabledAlpha = 0.4f,
        )
    }
}

/** Resolved per-size dimensions used by [TangemButton]. */
internal data class SizeTokens(
    val minHeight: Dp,
    val minWidth: Dp,
    val minSizeIconOnly: Dp,
    val textPadding: Dp,
    val textStyle: TextStyle,
    val containerHorizontalPadding: Dp,
    val containerVerticalPadding: Dp,
    val iconSize: Dp,
    val loaderSize: TangemLoaderSize,
)

@Suppress("LongMethod")
@Composable
@ReadOnlyComposable
internal fun TangemButton.Size.tokens(): SizeTokens {
    return when (this) {
        TangemButton.Size.X14 -> SizeTokens(
            minHeight = 56.dp,
            minWidth = 88.dp,
            minSizeIconOnly = 56.dp,
            textPadding = 8.dp,
            textStyle = TangemTheme.typography3.body.medium,
            containerHorizontalPadding = 16.dp,
            containerVerticalPadding = 16.dp,
            iconSize = 24.dp,
            loaderSize = TangemLoaderSize.X24,
        )
        TangemButton.Size.X12 -> SizeTokens(
            minHeight = 48.dp,
            minWidth = 80.dp,
            minSizeIconOnly = 48.dp,
            textPadding = 8.dp,
            textStyle = TangemTheme.typography3.body.medium,
            containerHorizontalPadding = 12.dp,
            containerVerticalPadding = 12.dp,
            iconSize = 24.dp,
            loaderSize = TangemLoaderSize.X24,
        )
        TangemButton.Size.X11 -> SizeTokens(
            minHeight = 44.dp,
            minWidth = 72.dp,
            minSizeIconOnly = 44.dp,
            textPadding = 6.dp,
            textStyle = TangemTheme.typography3.body.medium,
            containerHorizontalPadding = 12.dp,
            containerVerticalPadding = 12.dp,
            iconSize = 20.dp,
            loaderSize = TangemLoaderSize.X20,
        )
        TangemButton.Size.X10 -> SizeTokens(
            minHeight = 40.dp,
            minWidth = 64.dp,
            minSizeIconOnly = 40.dp,
            textPadding = 6.dp,
            textStyle = TangemTheme.typography3.body.medium,
            containerHorizontalPadding = 10.dp,
            containerVerticalPadding = 10.dp,
            iconSize = 20.dp,
            loaderSize = TangemLoaderSize.X20,
        )
        TangemButton.Size.X9 -> SizeTokens(
            minHeight = 36.dp,
            minWidth = 56.dp,
            minSizeIconOnly = 36.dp,
            textPadding = 6.dp,
            textStyle = TangemTheme.typography3.body.medium,
            containerHorizontalPadding = 8.dp,
            containerVerticalPadding = 8.dp,
            iconSize = 20.dp,
            loaderSize = TangemLoaderSize.X20,
        )
        TangemButton.Size.X8 -> SizeTokens(
            minHeight = 32.dp,
            minWidth = 48.dp,
            minSizeIconOnly = 32.dp,
            textPadding = 6.dp,
            textStyle = TangemTheme.typography3.body.medium,
            containerHorizontalPadding = 6.dp,
            containerVerticalPadding = 6.dp,
            iconSize = 20.dp,
            loaderSize = TangemLoaderSize.X20,
        )
        TangemButton.Size.X7 -> SizeTokens(
            minHeight = 28.dp,
            minWidth = 40.dp,
            minSizeIconOnly = 28.dp,
            textPadding = 4.dp,
            textStyle = TangemTheme.typography3.subheading.medium,
            containerHorizontalPadding = 6.dp,
            containerVerticalPadding = 4.dp,
            iconSize = 16.dp,
            loaderSize = TangemLoaderSize.X16,
        )
    }
}