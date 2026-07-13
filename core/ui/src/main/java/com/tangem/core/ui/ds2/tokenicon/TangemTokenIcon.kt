package com.tangem.core.ui.ds2.tokenicon

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon.State.Indicator
import com.tangem.core.ui.extensions.ColorReference2
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.assets.il_token_custom
import com.tangem.core.ui.res.generated.assets.il_token_error
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.utils.ImageBackgroundContrastChecker
import com.tangem.core.ui.utils.getGreyScaleColorFilter
import kotlinx.coroutines.launch

/**
 * Design-system v2 token icon rendered from a high-level [TangemTokenIcon.UiState] — a loaded token,
 * a loading shimmer, or an error placeholder.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=3901-470)
 *
 * @param state High-level rendering state; see [TangemTokenIcon.UiState].
 * @param size One of the fixed [TangemTokenIcon.Size] presets driving the icon and overlay dimensions.
 * @param modifier Modifier applied to the icon root. The icon is fixed-size per [size]; use this to
 * position it within the parent.
 * @param contentDescription Accessibility label describing the token this icon represents.
 */
@Composable
fun TangemTokenIcon(
    state: TangemTokenIcon.UiState,
    size: TangemTokenIcon.Size,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    when (state) {
        TangemTokenIcon.UiState.Error -> {
            Image(
                modifier = modifier.size(size.tokens().size),
                imageVector = Icons.il_token_error,
                contentDescription = contentDescription,
            )
        }
        is TangemTokenIcon.UiState.Token -> {
            TangemTokenIcon(
                state = state.tokenState,
                size = size,
                modifier = modifier,
                contentDescription = contentDescription,
            )
        }
        TangemTokenIcon.UiState.Shimmer -> {
            TangemShimmer(radius = 999.dp, modifier = modifier.size(size.tokens().size))
        }
    }
}

/**
 * Design-system v2 token icon rendered from a fully-resolved [TangemTokenIcon.State].
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=3901-470)
 *
 * @param state Resolved icon state; see [TangemTokenIcon.State].
 * @param size One of the fixed [TangemTokenIcon.Size] presets driving the icon and overlay dimensions.
 * @param modifier Modifier applied to the icon root. The icon is fixed-size per [size]; use this to
 * position it within the parent.
 * @param contentDescription Accessibility label describing the token
 */
@Suppress("LongMethod")
@Composable
fun TangemTokenIcon(
    state: TangemTokenIcon.State,
    size: TangemTokenIcon.Size,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val (alpha, colorFilter) = remember(state.isGrayscale) {
        getGreyScaleColorFilter(state.isGrayscale)
    }

    val itemBackgroundColor = TangemTheme.colors.background.primary.toArgb()
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()
    val sizeTokens = size.tokens()
    val pixelsSize = with(LocalDensity.current) { sizeTokens.size.roundToPx() }
    val context = LocalContext.current

    val iconUrl = state.url?.takeIf(String::isNotBlank)
    val iconData: Any = iconUrl.orEmpty()

    var iconBackgroundColor by remember(iconData) {
        mutableStateOf(iconUrl?.let(contrastColorCache::get) ?: Color.Transparent)
    }
    var isBackgroundColorDefined by remember(iconData) {
        mutableStateOf(iconUrl != null && contrastColorCache.get(iconUrl) != null)
    }

    val imageRequest = remember(iconData, pixelsSize, isDarkTheme, itemBackgroundColor) {
        ImageRequest.Builder(context = context)
            .data(iconData)
            .size(size = pixelsSize)
            .memoryCacheKey(key = iconData.toString() + pixelsSize)
            .crossfade(enable = true)
            .allowHardware(enable = !isDarkTheme) // Hardware bitmaps can't be read for Palette quantization.
            .listener(
                onSuccess = { _, result ->
                    if (!isBackgroundColorDefined && isDarkTheme && iconUrl != null) {
                        coroutineScope.launch {
                            val color = ImageBackgroundContrastChecker(
                                drawable = result.drawable,
                                backgroundColor = itemBackgroundColor,
                                size = pixelsSize,
                            ).getContrastColor(isDarkTheme = true)
                            contrastColorCache.put(iconUrl, color)
                            iconBackgroundColor = color
                            isBackgroundColorDefined = true
                        }
                    }
                },
            ).build()
    }

    Box(
        modifier = modifier.size(sizeTokens.size),
    ) {
        val hasCutout = state.topIcon != null || state.indicator != null
        val baseModifier = Modifier
            .matchParentSize()
            .conditionalCompose(hasCutout) {
                graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        val drawSize = this.size
                        drawContent()
                        if (state.topIcon != null) {
                            drawCircle(
                                color = Color.Transparent,
                                radius = (sizeTokens.topIconSize / 2 + CUTOUT).toPx(),
                                center = Offset(
                                    x = drawSize.width -
                                        (sizeTokens.topIconSize / 2 + sizeTokens.topIconOffset).toPx(),
                                    y = (sizeTokens.topIconOffset + sizeTokens.topIconSize / 2).toPx(),
                                ),
                                blendMode = BlendMode.Clear,
                            )
                        }
                        if (state.indicator != null) {
                            val indicatorCenter = drawSize.width -
                                (sizeTokens.indicatorOffset + sizeTokens.indicatorSize / 2).toPx()
                            drawCircle(
                                color = Color.Transparent,
                                radius = (sizeTokens.indicatorSize / 2 + CUTOUT).toPx(),
                                center = Offset(x = indicatorCenter, y = indicatorCenter),
                                blendMode = BlendMode.Clear,
                            )
                        }
                    }
            }
            .background(
                color = iconBackgroundColor,
                shape = RoundedCornerShape(8.dp),
            )
            .clip(RoundedCornerShape(8.dp))

        if (state.url == null) {
            Image(
                modifier = baseModifier,
                imageVector = Icons.il_token_custom,
                contentDescription = contentDescription,
                alpha = alpha,
                colorFilter = colorFilter,
            )
        } else {
            TokenAsyncImage(
                imageRequest = imageRequest,
                alpha = alpha,
                colorFilter = colorFilter,
                contentDescription = contentDescription,
                modifier = baseModifier,
            )
        }

        if (state.topIcon != null) {
            Image(
                modifier = Modifier
                    .offset(x = -sizeTokens.topIconOffset, y = sizeTokens.topIconOffset)
                    .align(Alignment.TopEnd)
                    .size(size = sizeTokens.topIconSize),
                imageVector = state.topIcon,
                contentDescription = null,
                colorFilter = colorFilter,
                alpha = alpha,
            )
        }

        if (state.indicator != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = sizeTokens.indicatorOffset, bottom = sizeTokens.indicatorOffset)
                    .size(size = sizeTokens.indicatorSize)
                    .background(
                        color = state.indicator.colorReference2(),
                        shape = CircleShape,
                    )
                    .padding(1.dp),
            )
        }
    }
}

@Composable
private fun TokenAsyncImage(
    imageRequest: ImageRequest,
    alpha: Float,
    colorFilter: ColorFilter?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val painter = rememberAsyncImagePainter(model = imageRequest)
    when (painter.state) {
        is AsyncImagePainter.State.Success -> {
            Image(
                modifier = modifier,
                painter = painter,
                contentDescription = contentDescription,
                alpha = alpha,
                colorFilter = colorFilter,
            )
        }
        is AsyncImagePainter.State.Error -> {
            Image(
                modifier = modifier,
                imageVector = Icons.il_token_error,
                contentDescription = contentDescription,
                alpha = alpha,
                colorFilter = colorFilter,
            )
        }
        else -> {
            TangemShimmer(radius = 999.dp, modifier = modifier)
        }
    }
}

/** Public API namespace for [TangemTokenIcon]. */
object TangemTokenIcon {

    /** High-level rendering state for the [TangemTokenIcon] overload that accepts a [UiState]. */
    @Immutable
    sealed class UiState {
        /** A resolved token icon, described by [tokenState]. */
        data class Token(val tokenState: State) : UiState()

        /** Loading placeholder — a circular shimmer. */
        data object Shimmer : UiState()

        /** Failure placeholder — the static error illustration. */
        data object Error : UiState()
    }

    /**
     * Fully-resolved token-icon state.
     *
     * @param url Remote image URL. `null` renders the custom-token illustration; blank is treated as `null`.
     * @param topIcon Optional overlay icon drawn at the top-end corner (e.g. a network badge), with a
     * transparent cutout punched behind it.
     * @param isGrayscale When `true`, the whole icon is desaturated and dimmed.
     * @param indicator Optional small status dot drawn at the bottom-end corner, with a cutout behind it.
     */
    data class State(
        val url: String?,
        val topIcon: ImageVector? = null,
        val isGrayscale: Boolean = false,
        val indicator: Indicator? = null,
    ) {

        /**
         * Bottom-end status dot.
         *
         * @param colorReference2 Fill color of the dot, resolved from DS3 tokens.
         */
        data class Indicator(
            val colorReference2: ColorReference2 = ColorReference2 { TangemTheme.colors3.icon.tertiary },
        )
    }

    /** Fixed icon size presets, in dp (`X40` = 40.dp … `X72` = 72.dp). */
    enum class Size {
        X40, X44, X56, X72
    }
}

private fun TangemTokenIcon.Size.tokens(): Tokens {
    return when (this) {
        TangemTokenIcon.Size.X40 -> Tokens(
            size = 40.dp,
            topIconSize = 12.dp,
            topIconOffset = (-1).dp,
            indicatorSize = 6.dp,
            indicatorOffset = 3.dp,
        )
        TangemTokenIcon.Size.X44 -> Tokens(
            size = 44.dp,
            topIconSize = 16.dp,
            topIconOffset = (-2).dp,
            indicatorSize = 6.dp,
            indicatorOffset = 4.dp,
        )
        TangemTokenIcon.Size.X56 -> Tokens(
            size = 56.dp,
            topIconSize = 20.dp,
            topIconOffset = (-4).dp,
            indicatorSize = 6.dp,
            indicatorOffset = 5.dp,
        )
        TangemTokenIcon.Size.X72 -> Tokens(
            size = 72.dp,
            topIconSize = 24.dp,
            topIconOffset = (-4).dp,
            indicatorSize = 8.dp,
            indicatorOffset = 6.dp,
        )
    }
}

private class Tokens(
    val size: Dp,
    val topIconSize: Dp,
    val topIconOffset: Dp,
    val indicatorSize: Dp,
    val indicatorOffset: Dp,
)

/** Transparent gap cut out of the base icon around the top icon and the indicator. */
private val CUTOUT = 1.dp

private const val CONTRAST_COLOR_CACHE_SIZE = 100

/**
 * Caches the dark-theme contrast background color per icon URL so that revisiting an icon while
 * scrolling reuses the result instead of re-decoding the bitmap and re-running Palette quantization.
 */
private val contrastColorCache = LruCache<String, Color>(CONTRAST_COLOR_CACHE_SIZE)

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemTokenIconPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreviewRow(
                state = TangemTokenIcon.State(url = ""),
            )
            PreviewRow(
                state = TangemTokenIcon.State(
                    url = "",
                    indicator = Indicator(
                        colorReference2 = { TangemTheme.colors3.icon.brand },
                    ),
                    topIcon = ImageVector.vectorResource(R.drawable.img_btc_cash_22),
                ),
            )
            PreviewRow(
                state = TangemTokenIcon.State(url = null, isGrayscale = true),
            )
        }
    }
}

@Composable
private fun PreviewRow(state: TangemTokenIcon.State, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TangemTokenIcon.Size.entries.forEach { size ->
            TangemTokenIcon(state = state, size = size)
        }
    }
}