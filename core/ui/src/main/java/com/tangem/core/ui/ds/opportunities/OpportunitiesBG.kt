package com.tangem.core.ui.ds.opportunities

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.haze.hazeForegroundEffectTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import dev.chrisbanes.haze.HazeStyle

/**
 * Container that draws a blurred background (from URL or solid color) and
 * applies a semi‑transparent overlay on top of it, then renders foreground content.
 *
 * Figma https://www.figma.com/design/X0IMgSMOT5rWWgiSIeZQwC/Bottom-sheet--Redesign-?node-id=3360-64755&m=dev
 *
 * @param icon Background configuration (URL, solid color or none).
 * @param modifier Modifier applied to the outer container.
 * @param content Foreground content rendered on top of the overlay.
 * @param shape Shape used for inner shadow and border (e.g. rounded corners).
 */
@Suppress("MagicNumber")
@Composable
fun OpportunitiesBG(
    icon: TangemIconUM,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val isInDarkTheme = LocalIsInDarkTheme.current
    val overlayColor = remember(isInDarkTheme) {
        if (isInDarkTheme) {
            Color(OVERLAY_DARK)
        } else {
            Color.White
        }
    }

    Box(modifier = modifier) {
        BackgroundLayer(icon = icon)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .innerShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = 30.dp,
                        spread = 5.dp,
                        color = Color(INNER_SHADOW_COLOR_START).copy(alpha = .3f),
                        offset = DpOffset(0.dp, 0.dp),
                    ),
                )
                .innerShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = 100.dp,
                        spread = (-39).dp,
                        color = Color(INNER_SHADOW_COLOR_END).copy(.3f),
                        offset = DpOffset(0.dp, (-56).dp),
                    ),
                )
                .innerShadow(
                    shape = shape,
                    shadow = Shadow(
                        radius = 40.dp,
                        spread = (-19).dp,
                        color = Color(INNER_SHADOW_COLOR_END).copy(alpha = .25f),
                        offset = DpOffset(0.dp, (-16).dp),
                    ),
                )
                .drawWithContent {
                    drawRect(color = overlayColor.copy(alpha = .7f))
                    drawContent()
                    val outline = shape.createOutline(size, layoutDirection, this)
                    drawOutline(outline, Color(BORDER_COLOR).copy(alpha = .1f), style = Stroke(width = 1.dp.toPx()))
                },
            content = content,
        )
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun BoxScope.BackgroundLayer(icon: TangemIconUM, blurRadius: Dp = 26.dp) {
    when (icon) {
        is TangemIconUM.Currency -> CurrencyIconBackgroundLayer(icon.currencyIconState, blurRadius)
        is TangemIconUM.Icon -> SolidColorBackground(icon.tintReference(), blurRadius)
        is TangemIconUM.Ident -> Unit
        is TangemIconUM.Image -> ResBackground(icon.imageRes, blurRadius)
        is TangemIconUM.Url -> UrlColorBackground(icon.url, blurRadius)
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
private fun BoxScope.CurrencyIconBackgroundLayer(state: CurrencyIconState, blurRadius: Dp) {
    when (state) {
        is CurrencyIconState.CryptoPortfolio.Icon -> SolidColorBackground(
            color = state.color,
            blurRadius = blurRadius,
        )
        is CurrencyIconState.CryptoPortfolio.Letter -> SolidColorBackground(
            color = state.color,
            blurRadius = blurRadius,
        )
        is CurrencyIconState.CustomTokenIcon -> SolidColorBackground(
            color = state.background,
            blurRadius = blurRadius,
        )
        is CurrencyIconState.Empty -> ResBackground(res = state.resId, blurRadius = blurRadius)
        is CurrencyIconState.CoinIcon -> {
            state.url?.let {
                UrlBackground(imageUrl = state.url, blurRadius = blurRadius)
            } ?: run {
                ResBackground(res = state.fallbackResId, blurRadius = blurRadius)
            }
        }
        is CurrencyIconState.FiatIcon -> state.url?.let {
            UrlBackground(imageUrl = state.url, blurRadius = blurRadius)
        } ?: run {
            ResBackground(res = state.fallbackResId, blurRadius = blurRadius)
        }
        is CurrencyIconState.TokenIcon -> state.url?.let {
            UrlBackground(imageUrl = state.url, blurRadius = blurRadius)
        } ?: run {
            SolidColorBackground(
                color = state.fallbackBackground,
                blurRadius = blurRadius,
            )
        }
        CurrencyIconState.Loading -> Unit
        CurrencyIconState.Locked -> Unit
    }
}

@Composable
private fun BoxScope.UrlBackground(imageUrl: String?, blurRadius: Dp) {
    val context = LocalContext.current

    val imageRequest = remember(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build()
        }
    }

    if (imageRequest != null) {
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .scale(SCALE_FACTOR)
                .hazeForegroundEffectTangem(style = HazeStyle(blurRadius = blurRadius, tint = null)),
        )
    }
}

@Composable
private fun BoxScope.ResBackground(res: Int, blurRadius: Dp) {
    Image(
        painter = painterResource(res),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .matchParentSize()
            .scale(SCALE_FACTOR)
            .hazeForegroundEffectTangem(style = HazeStyle(blurRadius = blurRadius, tint = null)),
    )
}

@Composable
private fun BoxScope.SolidColorBackground(color: Color, blurRadius: Dp) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(color = color)
            .hazeForegroundEffectTangem(style = HazeStyle(blurRadius = blurRadius, tint = null)),
    )
}

@Composable
private fun BoxScope.UrlColorBackground(url: String, blurRadius: Dp) {
    SubcomposeAsyncImage(
        modifier = Modifier
            .matchParentSize()
            .hazeForegroundEffectTangem(style = HazeStyle(blurRadius = blurRadius, tint = null)),
        model = ImageRequest.Builder(context = LocalContext.current)
            .data(url)
            .crossfade(enable = true)
            .allowHardware(enable = false)
            .build(),
        loading = { CircleShimmer() },
        error = {
            Box(
                modifier = Modifier
                    .background(
                        color = TangemTheme.colors2.surface.level3,
                        shape = CircleShape,
                    ),
            )
        },
        contentDescription = null,
    )
}

private const val SCALE_FACTOR = 1.5f
private const val INNER_SHADOW_COLOR_START = 0x00000000
private const val INNER_SHADOW_COLOR_END = 0xFFFFFFFF

private const val BORDER_COLOR = 0xFFF0F0F0
private const val OVERLAY_DARK = 0xFF141414

// region Previews

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OpportunitiesBGPreview() {
    TangemThemePreview {
        OpportunitiesBG(
            modifier = Modifier.size(400.dp),
            icon = TangemIconUM.Currency(
                CurrencyIconState.CoinIcon(
                    url = null,
                    fallbackResId = R.drawable.img_solana_22,
                    isGrayscale = false,
                    shouldShowCustomBadge = false,
                ),
            ),
            content = {},
        )
    }
}

// endregion