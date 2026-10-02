@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.avatar

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import arrow.core.Either
import com.tangem.core.ui.res.TangemTheme

/**
 * Type style the [TangemAvatar.UiState.User] initial is set in. Figma gives each size its own style
 * rather than one style at three sizes, so the weight and letter spacing differ too.
 */
internal enum class AvatarInitialStyle { HeadingSmall, Body, Caption }

@Composable
@ReadOnlyComposable
internal fun AvatarInitialStyle.textStyle(): TextStyle = when (this) {
    AvatarInitialStyle.HeadingSmall -> TangemTheme.typography3.heading.small
    AvatarInitialStyle.Body -> TangemTheme.typography3.body.medium
    AvatarInitialStyle.Caption -> TangemTheme.typography3.caption.medium
}

/** Every dimension one [TangemAvatar.Size] resolves to. */
@Immutable
internal data class AvatarTokens(
    val size: Dp,
    val cornerRadius: Dp,
    val accountGlyphSize: Dp,
    val iconGlyphSize: Dp,
    val initialFontSize: TextUnit,
    val initialStyle: AvatarInitialStyle,
    /** `0.dp` on the sizes Figma gives no pending indicator. */
    val indicatorSize: Dp,
)

internal fun TangemAvatar.Size.tokens(): AvatarTokens = when (this) {
    TangemAvatar.Size.X72 -> AvatarTokens(
        size = 72.dp,
        cornerRadius = 20.dp,
        accountGlyphSize = 36.dp,
        iconGlyphSize = 32.dp,
        initialFontSize = 20.sp,
        initialStyle = AvatarInitialStyle.HeadingSmall,
        indicatorSize = 0.dp,
    )
    TangemAvatar.Size.X44 -> AvatarTokens(
        size = 44.dp,
        cornerRadius = 12.dp,
        accountGlyphSize = 22.dp,
        iconGlyphSize = 20.dp,
        initialFontSize = 16.sp,
        initialStyle = AvatarInitialStyle.Body,
        indicatorSize = 16.dp,
    )
    TangemAvatar.Size.X40 -> AvatarTokens(
        size = 40.dp,
        cornerRadius = 12.dp,
        accountGlyphSize = 20.dp,
        iconGlyphSize = 20.dp,
        initialFontSize = 16.sp,
        initialStyle = AvatarInitialStyle.Body,
        indicatorSize = 16.dp,
    )
    TangemAvatar.Size.X16 -> AvatarTokens(
        size = 16.dp,
        cornerRadius = 4.dp,
        accountGlyphSize = 8.dp,
        iconGlyphSize = 12.dp,
        initialFontSize = 12.sp,
        initialStyle = AvatarInitialStyle.Caption,
        indicatorSize = 0.dp,
    )
}

/**
 * Interpolates between two size presets so a host can morph an avatar continuously — the account row
 * shrinks its 40dp avatar to 16dp while expanding. The indicator is dropped as soon as either end lacks
 * one, because Figma only draws it at [TangemAvatar.Size.X40] and [TangemAvatar.Size.X44].
 */
internal fun lerpAvatarTokens(start: AvatarTokens, stop: AvatarTokens, fraction: Float) = AvatarTokens(
    size = lerp(start = start.size, stop = stop.size, fraction = fraction),
    cornerRadius = lerp(start = start.cornerRadius, stop = stop.cornerRadius, fraction = fraction),
    accountGlyphSize = lerp(start = start.accountGlyphSize, stop = stop.accountGlyphSize, fraction = fraction),
    iconGlyphSize = lerp(start = start.iconGlyphSize, stop = stop.iconGlyphSize, fraction = fraction),
    initialFontSize = lerp(start = start.initialFontSize, stop = stop.initialFontSize, fraction = fraction),
    initialStyle = if (fraction < 0.5f) start.initialStyle else stop.initialStyle,
    indicatorSize = if (start.indicatorSize > 0.dp && stop.indicatorSize > 0.dp) {
        lerp(start = start.indicatorSize, stop = stop.indicatorSize, fraction = fraction)
    } else {
        0.dp
    },
)

@Immutable
internal data class AvatarAppearanceTokens(val background: Color, val tint: Color)

@Composable
@ReadOnlyComposable
internal fun TangemAvatar.Appearance.tokens(): AvatarAppearanceTokens {
    val colors = TangemTheme.colors3
    return when (this) {
        TangemAvatar.Appearance.Neutral -> AvatarAppearanceTokens(
            background = colors.bg.opaque.primary,
            tint = colors.icon.secondary,
        )
        TangemAvatar.Appearance.Success -> AvatarAppearanceTokens(
            background = colors.bg.status.successSubtle,
            tint = colors.icon.status.success,
        )
        TangemAvatar.Appearance.Error -> AvatarAppearanceTokens(
            background = colors.bg.status.errorSubtle,
            tint = colors.icon.status.error,
        )
        TangemAvatar.Appearance.Warning -> AvatarAppearanceTokens(
            background = colors.bg.status.warningSubtle,
            tint = colors.icon.status.warning,
        )
        TangemAvatar.Appearance.Info -> AvatarAppearanceTokens(
            background = colors.bg.status.infoSubtle,
            tint = colors.icon.status.info,
        )
    }
}

/**
 * Avatar fill palette, as raw brand hexes. These are deliberately not DS3 tokens: the Figma
 * `Avatar Module [Fill Color]` fills carry no variable bindings, so an avatar keeps the same colour in
 * light and dark.
 */
@Suppress("CyclomaticComplexMethod")
internal fun TangemAvatar.FillColor.toColor(): Color = when (this) {
    TangemAvatar.FillColor.DarkBlue -> Color(color = 0xFF001EFF)
    TangemAvatar.FillColor.LightBlue -> Color(color = 0xFF2EB8EA)
    TangemAvatar.FillColor.Lavender -> Color(color = 0xFFA38CF2)
    TangemAvatar.FillColor.Violet -> Color(color = 0xFF734DF1)
    TangemAvatar.FillColor.Golden -> Color(color = 0xFFCD8742)
    TangemAvatar.FillColor.LightPurple -> Color(color = 0xFF9F38E3)
    TangemAvatar.FillColor.DarkPurple -> Color(color = 0xFF8333EC)
    TangemAvatar.FillColor.Pink -> Color(color = 0xFFFF42A6)
    TangemAvatar.FillColor.Red -> Color(color = 0xFFFF3531)
    TangemAvatar.FillColor.Yellow -> Color(color = 0xFFFF932D)
    TangemAvatar.FillColor.LightGreen -> Color(color = 0xFF96AF30)
    TangemAvatar.FillColor.DarkGreen -> Color(color = 0xFF2D8F1C)
    TangemAvatar.FillColor.Predictions -> Color(color = 0xFFA967FD)
    TangemAvatar.FillColor.Gacha -> Color(color = 0xFFE68A03)
    TangemAvatar.FillColor.PayVisa -> Color(color = 0xFF593BF3)
    TangemAvatar.FillColor.Money -> Color(color = 0xFF2DA30D)
}

/** Figma maps the three transaction states onto three of the [TangemAvatar.Appearance] colour schemes. */
internal fun TangemAvatar.Status.toAppearance(): TangemAvatar.Appearance = when (this) {
    TangemAvatar.Status.Progress -> TangemAvatar.Appearance.Info
    TangemAvatar.Status.Finished -> TangemAvatar.Appearance.Neutral
    TangemAvatar.Status.Failed -> TangemAvatar.Appearance.Error
}

internal fun TangemAvatar.UiState.shape(cornerRadius: Dp): Shape = when {
    this is TangemAvatar.UiState.Account -> RoundedCornerShape(cornerRadius)
    this is TangemAvatar.UiState.Shimmer &&
        shape == TangemAvatar.UiState.Shimmer.Shape.RoundedSquare -> RoundedCornerShape(cornerRadius)
    else -> CircleShape
}

@Composable
internal fun Either<Int, ImageVector>.asImageVector(): ImageVector = fold(
    ifLeft = { resId -> ImageVector.vectorResource(id = resId) },
    ifRight = { it },
)