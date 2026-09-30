@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.avatar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.res.TangemTheme

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

/** Figma maps the three transaction states onto three of the [TangemAvatar.Appearance] colour schemes. */
internal fun TangemAvatar.Status.toAppearance(): TangemAvatar.Appearance = when (this) {
    TangemAvatar.Status.Progress -> TangemAvatar.Appearance.Info
    TangemAvatar.Status.Finished -> TangemAvatar.Appearance.Neutral
    TangemAvatar.Status.Failed -> TangemAvatar.Appearance.Error
}