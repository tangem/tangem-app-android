@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.avatar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_hands_12
import com.tangem.core.ui.res.generated.icons.ic_user_20

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
    val shape = state.shape.toComposeShape(cornerRadius = tokens.cornerRadius)
    val hasIndicator = state is TangemAvatar.UiState.Account &&
        state.hasIndicator &&
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
            when (val glyph = state.glyph) {
                is AvatarAccountGlyph.Icon -> Icon(
                    imageVector = glyph.icon.asImageVector(),
                    contentDescription = contentDescription,
                    tint = TangemTheme.colors3.text.staticDark.primary.copy(alpha = ACCOUNT_GLYPH_ALPHA),
                    modifier = Modifier.size(tokens.accountGlyphSize),
                )
                is AvatarAccountGlyph.Letter -> InitialText(
                    initial = glyph.text.resolveReference().firstSymbol(),
                    tokens = tokens,
                )
            }
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
            InitialText(initial = state.initial.toString(), tokens = tokens)
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
        is TangemAvatar.UiState.Asset -> AssetImage(
            source = state.source,
            tokens = tokens,
            contentDescription = contentDescription,
        )
        is TangemAvatar.UiState.Shimmer -> TangemShimmer(
            radius = tokens.cornerRadius,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun InitialText(initial: String, tokens: AvatarTokens) {
    Text(
        text = initial.uppercase(),
        style = tokens.initialStyle.textStyle().copy(fontSize = tokens.initialFontSize),
        color = TangemTheme.colors3.text.staticDark.primary,
        textAlign = TextAlign.Center,
    )
}

private fun String.firstSymbol(): String = if (isEmpty()) this else substring(0, offsetByCodePoints(0, 1))

@Composable
private fun BoxScope.AssetImage(source: AvatarAssetSource, tokens: AvatarTokens, contentDescription: String?) {
    when (source) {
        is AvatarAssetSource.Local -> Image(
            painter = source.icon.fold(
                ifLeft = { resId -> painterResource(id = resId) },
                ifRight = { imageVector -> rememberVectorPainter(image = imageVector) },
            ),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        is AvatarAssetSource.Url -> {
            val context = LocalContext.current
            val imageRequest = remember(source.url) {
                ImageRequest.Builder(context = context).data(source.url).crossfade(enable = true).build()
            }
            val painter = rememberAsyncImagePainter(model = imageRequest)
            Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            when (painter.state) {
                is AsyncImagePainter.State.Success -> Unit
                is AsyncImagePainter.State.Error -> AppearanceBackground(appearance = TangemAvatar.Appearance.Neutral)
                else -> TangemShimmer(radius = tokens.cornerRadius, modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun BoxScope.FilledBackground(color: TangemAvatar.FillColor) {
    Box(modifier = Modifier.matchParentSize().background(color = color.toColor()))
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
            .offset(x = 1.dp, y = 1.dp)
            .size(size)
            .background(color = TangemTheme.colors3.bg.accent.yellow, shape = CircleShape),
    ) {
        Icon(
            imageVector = Icons.ic_clock_hands_12,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticDark,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * Punches a transparent ring where the pending indicator overlaps the avatar, so the two never touch.
 * Same technique as the token icon's network badge.
 */
private fun Modifier.indicatorCutout(indicatorSize: Dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val center = size.width - (indicatorSize / 2 - 1.dp).toPx()
        drawCircle(
            color = Color.Transparent,
            radius = (indicatorSize / 2 + 1.dp).toPx(),
            center = Offset(x = center, y = center),
            blendMode = BlendMode.Clear,
        )
    }