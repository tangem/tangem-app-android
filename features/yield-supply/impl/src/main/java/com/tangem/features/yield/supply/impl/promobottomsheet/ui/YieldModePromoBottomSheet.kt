package com.tangem.features.yield.supply.impl.promobottomsheet.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.YieldSupplyTestTags
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.promobottomsheet.entity.YieldModePromoBottomSheetUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.exp

private val ASSET_ICON_SIZE = 36.dp
private val ASSET_ICON_STEP = 24.dp
private val ASSET_ICON_RING = 2.dp
private val TOKENS_COUNT_MAX_WIDTH = 139.dp
private val GRAPH_GLOW_COLOR = Color(0xFF0C9F3D)
private val GRAPH_GLOW_OFFSET = DpOffset(x = 0.dp, y = 7.dp)
private val GRAPH_HALO_SIGMA = 69.dp
private val GRAPH_HALO_CENTER_OFFSET = DpOffset(x = 10.dp, y = 16.dp)
private const val GRAPH_HALO_PEAK_ALPHA = 0.098f
private const val GRAPH_HALO_RADIUS_IN_SIGMAS = 3f
private const val GRAPH_HALO_STOPS = 12

private const val GRAPH_VIEWPORT_WIDTH = 103.6f
private const val GRAPH_VIEWPORT_HEIGHT = 92.5f

// The bars of img_yield_promo_graph_104 in its viewport, so the glow follows the chart's outline
private val GraphBarsShape = GenericShape { size, _ ->
    val scaleX = size.width / GRAPH_VIEWPORT_WIDTH
    val scaleY = size.height / GRAPH_VIEWPORT_HEIGHT
    listOf(0f to 48.1f, 40.7f to 25.9f, 81.4f to 0f).forEach { (left, top) ->
        addRoundRect(
            RoundRect(
                left = left * scaleX,
                top = top * scaleY,
                right = (left + 22.2f) * scaleX,
                bottom = size.height,
                cornerRadius = CornerRadius(x = 11.1f * scaleX, y = 11.1f * scaleY),
            ),
        )
    }
}

@Composable
internal fun YieldModePromoBottomSheet(state: YieldModePromoBottomSheetUM) {
    TangemModal<YieldModePromoBottomSheetUM>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismissRequest,
            content = state,
        ),
        onBack = state.onBack,
        scrollableContent = false,
        chromePlacement = TangemModal.ChromePlacement.Overlay,
        containerColor = TangemTheme.colors3.bg.primary,
        showDragIndicator = false,
        title = { content ->
            TangemModal.TopNavigation(
                endButton = {
                    val closeLabel = stringResourceSafe(R.string.common_close)
                    TangemButton.Close(
                        modifier = Modifier
                            .testTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_CLOSE_BUTTON)
                            .semantics { contentDescription = closeLabel },
                        onClick = content.onCloseClick,
                    )
                },
            )
        },
        content = { content ->
            Content(state = content, modifier = Modifier.weight(weight = 1f, fill = false))
        },
    )
}

@Composable
private fun Content(state: YieldModePromoBottomSheetUM, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { state.onShown() }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            HeaderArt()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Texts(title = state.title)
                AssetsCapsule(tokensCount = state.tokensCount, iconUrls = state.tokenIconUrls)
            }
        }

        TangemFooter(
            modifier = Modifier.fillMaxWidth(),
            windowInsets = TangemFooter.ModalWindowInsets,
            fadeEnabled = false,
        ) {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_TRY_BUTTON),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.yield_module_main_promo_button),
                onClick = state.onTryClick,
            )
        }
    }
}

@Composable
private fun HeaderArt() {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .height(273.dp),
    ) {
        // VectorDrawable can't blur, so the design's glow is drawn in code
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.img_yield_promo_graph_104),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 111.dp)
                .size(width = 104.dp, height = 93.dp)
                .graphHalo()
                .softLayerShadow(
                    radius = 18.dp,
                    color = GRAPH_GLOW_COLOR.copy(alpha = 0.2f),
                    shape = GraphBarsShape,
                    offset = GRAPH_GLOW_OFFSET,
                ),
        )
    }
}

// The design's 126dp-blur shadow exceeds Skia's 128px blur sigma cap, so its Gaussian is drawn as a gradient
private fun Modifier.graphHalo(): Modifier = drawWithCache {
    val sigma = GRAPH_HALO_SIGMA.toPx()
    val radius = sigma * GRAPH_HALO_RADIUS_IN_SIGMAS
    val haloCenter = size.center + Offset(x = GRAPH_HALO_CENTER_OFFSET.x.toPx(), y = GRAPH_HALO_CENTER_OFFSET.y.toPx())
    val colorStops = Array(GRAPH_HALO_STOPS + 1) { index ->
        val fraction = index / GRAPH_HALO_STOPS.toFloat()
        val distance = fraction * radius
        val alpha = GRAPH_HALO_PEAK_ALPHA * exp(-distance * distance / (2 * sigma * sigma))
        fraction to GRAPH_GLOW_COLOR.copy(alpha = alpha)
    }
    val brush = Brush.radialGradient(colorStops = colorStops, center = haloCenter, radius = radius)
    onDrawBehind { drawCircle(brush = brush, radius = radius, center = haloCenter) }
}

@Composable
private fun Texts(title: TextReference) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .testTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_TITLE)
                .semantics { heading() },
            text = title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = stringResourceSafe(R.string.yield_module_main_promo_subtitle),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun AssetsCapsule(tokensCount: TextReference, iconUrls: ImmutableList<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .conditional(!LocalIsInDarkTheme.current) {
                softLayerShadow(
                    radius = 20.dp,
                    color = Color.Black.copy(alpha = 0.12f),
                    shape = CircleShape,
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                    isAlphaContentClip = true,
                )
            }
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.transparent.onMaterial.primary)
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .widthIn(max = TOKENS_COUNT_MAX_WIDTH),
            text = tokensCount.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp),
        )
        OverlappingAssetIcons(iconUrls = iconUrls)
    }
}

@Composable
private fun OverlappingAssetIcons(iconUrls: ImmutableList<String>) {
    val ringColor = TangemTheme.colors3.bg.secondary
    val placeholder = ColorPainter(TangemTheme.colors3.bg.tertiary)
    val context = LocalContext.current

    Box(modifier = Modifier.width(ASSET_ICON_SIZE + ASSET_ICON_STEP * (iconUrls.size - 1).coerceAtLeast(0))) {
        iconUrls.forEachIndexed { index, url ->
            AsyncImage(
                model = ImageRequest.Builder(context).data(url).crossfade(enable = true).build(),
                contentDescription = null,
                placeholder = placeholder,
                error = placeholder,
                modifier = Modifier
                    .zIndex((iconUrls.size - index).toFloat())
                    .padding(start = ASSET_ICON_STEP * index)
                    .size(ASSET_ICON_SIZE)
                    .border(width = ASSET_ICON_RING, color = ringColor, shape = CircleShape)
                    .padding(ASSET_ICON_RING)
                    .clip(CircleShape),
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, fontScale = 2f)
@Composable
private fun YieldModePromoBottomSheetPreview() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = true) {
        YieldModePromoBottomSheet(
            state = YieldModePromoBottomSheetUM(
                title = stringReference("Earn with your crypto up to 9.20% APY"),
                tokensCount = stringReference("Choose from 999 tokens"),
                tokenIconUrls = persistentListOf("", "", ""),
                onShown = {},
                onCloseClick = {},
                onBack = {},
                onDismissRequest = {},
                onTryClick = {},
            ),
        )
    }
}
// endregion