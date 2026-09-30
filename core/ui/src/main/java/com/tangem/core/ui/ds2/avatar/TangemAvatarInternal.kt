@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import arrow.core.Either
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_12
import com.tangem.core.ui.res.generated.icons.ic_user_20

/**
 * Type style the [TangemAvatar.UiState.User] initial is set in. Figma gives each size its own style
 * rather than one style at three sizes, so the weight and letter spacing differ too.
 */
internal enum class AvatarInitialStyle { HeadingSmall, Body, Caption }

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

/** Transparent ring cut out of the avatar around the pending indicator. */
private val INDICATOR_CUTOUT = 1.dp

/** How far the pending indicator overhangs the avatar's bottom-end corner. */
private val INDICATOR_OVERHANG = 1.dp

/** Opacity of the white sheen laid over a [TangemAvatar.FillColor], and of an account glyph. */
private const val FILL_SHEEN_ALPHA = 0.2f
private const val ACCOUNT_GLYPH_ALPHA = 0.8f

/**
 * Renders an avatar at already-resolved dimensions. Public callers go through `TangemAvatar(state, size)`;
 * this overload exists so a host that animates between two presets can pass interpolated [tokens].
 */
@Composable
internal fun TangemAvatarContent(
    state: TangemAvatar.UiState,
    tokens: AvatarTokens,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val shape = state.shape(cornerRadius = tokens.cornerRadius)
    val hasIndicator = state is TangemAvatar.UiState.Transaction &&
        state.hasPendingIndicator &&
        tokens.indicatorSize > 0.dp

    Box(modifier = modifier.size(tokens.size)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .matchParentSize()
                .conditionalCompose(hasIndicator) { indicatorCutout(indicatorSize = tokens.indicatorSize) }
                .clip(shape),
        ) {
            AvatarBody(state = state, tokens = tokens, contentDescription = contentDescription)
        }
        if (hasIndicator) {
            PendingIndicator(size = tokens.indicatorSize, modifier = Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun BoxScope.AvatarBody(state: TangemAvatar.UiState, tokens: AvatarTokens, contentDescription: String?) {
    when (state) {
        is TangemAvatar.UiState.Account -> {
            FilledBackground(color = state.color)
            Icon(
                imageVector = state.icon.asImageVector(),
                contentDescription = contentDescription,
                tint = TangemTheme.colors3.text.staticDark.primary.copy(alpha = ACCOUNT_GLYPH_ALPHA),
                modifier = Modifier.size(tokens.accountGlyphSize),
            )
        }
        is TangemAvatar.UiState.User -> if (state.initial == null) {
            AppearanceBackground(appearance = TangemAvatar.Appearance.Neutral)
            Icon(
                imageVector = Icons.ic_user_20,
                contentDescription = contentDescription,
                tint = TangemAvatar.Appearance.Neutral.tokens().tint,
                modifier = Modifier.size(tokens.iconGlyphSize),
            )
        } else {
            FilledBackground(color = state.color)
            Text(
                text = state.initial.uppercase(),
                style = tokens.initialStyle.textStyle().copy(fontSize = tokens.initialFontSize),
                color = TangemTheme.colors3.text.staticDark.primary,
                textAlign = TextAlign.Center,
            )
        }
        is TangemAvatar.UiState.Address -> IdentIcon(
            address = state.address,
            modifier = Modifier.matchParentSize(),
        )
        is TangemAvatar.UiState.Icon -> {
            AppearanceBackground(appearance = state.appearance)
            Icon(
                imageVector = state.icon.asImageVector(),
                contentDescription = contentDescription,
                tint = state.appearance.tokens().tint,
                modifier = Modifier.size(tokens.iconGlyphSize),
            )
        }
        is TangemAvatar.UiState.Transaction -> {
            val appearance = state.status.toAppearance()
            AppearanceBackground(appearance = appearance)
            Icon(
                imageVector = state.icon.asImageVector(),
                contentDescription = contentDescription,
                tint = appearance.tokens().tint,
                modifier = Modifier.size(tokens.iconGlyphSize),
            )
        }
        is TangemAvatar.UiState.Shimmer -> TangemShimmer(
            radius = if (state.shape == TangemAvatar.UiState.Shimmer.Shape.RoundedSquare) {
                tokens.cornerRadius
            } else {
                999.dp
            },
            modifier = Modifier.matchParentSize(),
        )
    }
}

/** Solid palette colour with the white top-to-bottom sheen Figma lays over it. */
@Composable
@ReadOnlyComposable
private fun AvatarInitialStyle.textStyle(): TextStyle = when (this) {
    AvatarInitialStyle.HeadingSmall -> TangemTheme.typography3.heading.small
    AvatarInitialStyle.Body -> TangemTheme.typography3.body.medium
    AvatarInitialStyle.Caption -> TangemTheme.typography3.caption.medium
}

@Composable
private fun BoxScope.FilledBackground(color: TangemAvatar.FillColor) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(color = color.toColor())
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = FILL_SHEEN_ALPHA)),
                ),
            ),
    )
}

@Composable
private fun BoxScope.AppearanceBackground(appearance: TangemAvatar.Appearance) {
    Box(modifier = Modifier.matchParentSize().background(color = appearance.tokens().background))
}

@Composable
private fun PendingIndicator(size: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(x = INDICATOR_OVERHANG, y = INDICATOR_OVERHANG)
            .size(size)
            .background(color = TangemTheme.colors3.bg.status.warningSubtle, shape = CircleShape),
    ) {
        Icon(
            imageVector = Icons.ic_clock_12,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.warning,
            modifier = Modifier.size(size * INDICATOR_GLYPH_FRACTION),
        )
    }
}

private const val INDICATOR_GLYPH_FRACTION = 0.75f

/**
 * Punches a transparent ring where the pending indicator overlaps the avatar, so the two never touch.
 * Same technique as the token icon's network badge.
 */
private fun Modifier.indicatorCutout(indicatorSize: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val center = size.width - (indicatorSize / 2 - INDICATOR_OVERHANG).toPx()
        drawCircle(
            color = Color.Transparent,
            radius = (indicatorSize / 2 + INDICATOR_CUTOUT).toPx(),
            center = Offset(x = center, y = center),
            blendMode = BlendMode.Clear,
        )
    }

private fun TangemAvatar.UiState.shape(cornerRadius: Dp): Shape = when {
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