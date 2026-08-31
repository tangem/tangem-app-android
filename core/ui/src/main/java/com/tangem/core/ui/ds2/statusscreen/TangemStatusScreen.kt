package com.tangem.core.ui.ds2.statusscreen

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.TangemColorPalette
import com.tangem.core.ui.utils.rememberScreenCornerRadius

/**
 * Design-system v2 full-screen status template: an ambient-lit screen with a status asset, title,
 * description and optional detail rows pinned above the footer. The header block is vertically centered
 * in the space the details and footer leave free.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=9267-11586)
 *
 * On first composition the screen plays its entrance choreography (asset and texts settle in, then the
 * ambient light, details and footer arrive); pass [animated] = `false` to render the final frame
 * immediately.
 *
 * @param variant Color mood of the screen — drives the ambient circles and the glow ring.
 * @param title Headline, centered.
 * @param modifier Modifier for the root container.
 * @param description Secondary line under the title.
 * @param asset Slot above the title, sized by its content: a single 40dp status icon, or wider content
 * such as a pair of overlapping token icons.
 * @param extraBottom Slot under the texts (a badge in the design).
 * @param details Slot for the Title—Value rows block above the footer.
 * @param footer Slot pinned to the bottom (a button block).
 * @param animated Whether to play the entrance animation.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun TangemStatusScreen(
    variant: TangemStatusScreen.Variant,
    title: TextReference,
    modifier: Modifier = Modifier,
    description: TextReference? = null,
    asset: (@Composable () -> Unit)? = null,
    extraBottom: (@Composable () -> Unit)? = null,
    details: (@Composable ColumnScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    animated: Boolean = true,
) {
    val entrance = rememberEntranceProgress(durationMs = ENTRANCE_DURATION_MS, animated = animated)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        AmbientBackground(
            variant = variant,
            entrance = entrance,
            modifier = Modifier.matchParentSize(),
        )
        TangemGlowRing(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = entrance.segment(GLOW_START_MS, GLOW_END_MS, SymmetricEase) },
            variant = variant.glowVariant,
            cornerRadius = rememberScreenCornerRadius(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            var bottomBlockHeight by remember { mutableIntStateOf(0) }

            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .graphicsLayer {
                        val settled = entrance.segment(REVEAL_START_MS, REVEAL_END_MS, DecelerateEase)
                        translationY = bottomBlockHeight / 2f * (1f - settled)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Header(
                    entrance = entrance,
                    title = title,
                    description = description,
                    asset = asset,
                    extraBottom = extraBottom,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { bottomBlockHeight = it.height },
            ) {
                if (details != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .graphicsLayer {
                                alpha = entrance.segment(DETAILS_START_MS, DETAILS_END_MS, SymmetricEase)
                            },
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                        content = details,
                    )
                }
                if (footer != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = entrance.segment(FOOTER_START_MS, FOOTER_FADE_END_MS, SymmetricEase)
                                translationY = FooterEnterOffset.toPx() *
                                    (1f - entrance.segment(FOOTER_START_MS, FOOTER_END_MS, DecelerateEase))
                            },
                    ) {
                        footer()
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    entrance: EntranceProgress,
    title: TextReference,
    description: TextReference?,
    asset: (@Composable () -> Unit)?,
    extraBottom: (@Composable () -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 48.dp, bottom = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
    ) {
        if (asset != null) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = entrance.segment(ASSET_START_MS, ASSET_END_MS, SymmetricEase)
                    translationY = AssetEnterOffset.toPx() *
                        (1f - entrance.segment(ASSET_START_MS, ASSET_END_MS, SpringEase))
                },
            ) {
                asset()
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = TextEnterOffset.toPx() *
                            (1f - entrance.segment(TEXT_START_MS, TEXT_END_MS, SpringEase))
                    },
                text = title.resolveReference(),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
            if (description != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = entrance.segment(DESCRIPTION_START_MS, DESCRIPTION_END_MS, SymmetricEase)
                            translationY = TextEnterOffset.toPx() *
                                (1f - entrance.segment(TEXT_START_MS, TEXT_END_MS, SpringEase))
                        },
                    text = description.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (extraBottom != null) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = entrance.segment(DESCRIPTION_START_MS, DESCRIPTION_END_MS, SymmetricEase)
                },
            ) {
                extraBottom()
            }
        }
    }
}

@Composable
private fun AmbientBackground(
    variant: TangemStatusScreen.Variant,
    entrance: EntranceProgress,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AmbientCircle(
            color = variant.ambientFarColor,
            maxAlpha = FAR_CIRCLE_ALPHA,
            blurRadius = FarCircleBlur,
            alignEnd = false,
            center = FarCircleCenter,
            entrance = entrance,
            modifier = Modifier.matchParentSize(),
        )
        AmbientCircle(
            color = variant.ambientNearColor,
            maxAlpha = NEAR_CIRCLE_ALPHA,
            blurRadius = NearCircleBlur,
            alignEnd = true,
            center = NearCircleCenter,
            entrance = entrance,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun AmbientCircle(
    color: Color,
    maxAlpha: Float,
    blurRadius: Dp,
    alignEnd: Boolean,
    center: DpOffset,
    entrance: EntranceProgress,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.drawBehind {
            val settled = entrance.segment(AMBIENT_START_MS, AMBIENT_END_MS, SymmetricEase)
            val scale = AMBIENT_START_SCALE + (1f - AMBIENT_START_SCALE) * settled
            val radius = (AmbientCircleSize.toPx() / 2f + blurRadius.toPx()) * scale
            val origin = Offset(
                x = if (alignEnd) size.width + center.x.toPx() else center.x.toPx(),
                y = center.y.toPx(),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = maxAlpha * settled), Color.Transparent),
                    center = origin,
                    radius = radius,
                ),
                radius = radius,
                center = origin,
            )
        },
    )
}

object TangemStatusScreen {

    /**
     * Color mood of the screen. The ambient colors are palette primitives rather than semantic tokens:
     * the DS figma pins them to `palette/<hue>/70` and `/40` identically in both themes.
     */
    enum class Variant(
        internal val glowVariant: TangemGlowRing.Variant,
        internal val ambientFarColor: Color,
        internal val ambientNearColor: Color,
    ) {
        Neutral(
            glowVariant = TangemGlowRing.Variant.Magic,
            ambientFarColor = TangemColorPalette.Neutral.`70`,
            ambientNearColor = TangemColorPalette.Neutral.`40`,
        ),
        Success(
            glowVariant = TangemGlowRing.Variant.Success,
            ambientFarColor = TangemColorPalette.Green.`70`,
            ambientNearColor = TangemColorPalette.Green.`40`,
        ),
        Error(
            glowVariant = TangemGlowRing.Variant.Error,
            ambientFarColor = TangemColorPalette.Red.`70`,
            ambientNearColor = TangemColorPalette.Red.`40`,
        ),
        Warning(
            glowVariant = TangemGlowRing.Variant.Warning,
            ambientFarColor = TangemColorPalette.Yellow.`70`,
            ambientNearColor = TangemColorPalette.Yellow.`40`,
        ),
        Info(
            glowVariant = TangemGlowRing.Variant.Info,
            ambientFarColor = TangemColorPalette.Blue.`70`,
            ambientNearColor = TangemColorPalette.Blue.`40`,
        ),
    }
}

// Geometry and choreography from the DS figma (Status Screen page, 402dp reference frame).
private val AssetEnterOffset = 36.dp
private val TextEnterOffset = (-17).dp
private val FooterEnterOffset = 90.dp

private val AmbientCircleSize = 360.dp
private const val AMBIENT_START_SCALE = 0.4f
private val FarCircleCenter = DpOffset(x = 116.dp, y = 116.dp)
private val FarCircleBlur = 256.dp
private const val FAR_CIRCLE_ALPHA = 0.1f
private val NearCircleCenter = DpOffset(x = 32.dp, y = 32.dp)
private val NearCircleBlur = 192.dp
private const val NEAR_CIRCLE_ALPHA = 0.4f

private const val ENTRANCE_DURATION_MS = 1400
private const val ASSET_START_MS = 300
private const val ASSET_END_MS = 600
private const val TEXT_START_MS = 400
private const val TEXT_END_MS = 700
private const val DESCRIPTION_START_MS = 550
private const val DESCRIPTION_END_MS = 850
private const val GLOW_START_MS = 1000
private const val GLOW_END_MS = 1200
private const val DETAILS_START_MS = 1000
private const val DETAILS_END_MS = 1200
private const val AMBIENT_START_MS = 1000
private const val AMBIENT_END_MS = 1400
private const val REVEAL_START_MS = 1000
private const val REVEAL_END_MS = 1300
private const val FOOTER_START_MS = 1150
private const val FOOTER_FADE_END_MS = 1350
private const val FOOTER_END_MS = 1400

@Preview(showBackground = true, widthDp = 402, heightDp = 874)
@Preview(showBackground = true, widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemStatusScreenPreview(@PreviewParameter(VariantProvider::class) variant: TangemStatusScreen.Variant) {
    TangemThemePreviewRedesign {
        TangemStatusScreen(
            variant = variant,
            title = stringReference(value = "Title"),
            description = stringReference(value = "Description"),
            animated = false,
            asset = { PreviewAsset(variant = variant) },
            details = {
                repeat(times = 3) { index ->
                    TangemRow(
                        divider = index < 2,
                        titleSlot = {
                            Text(
                                text = "Title",
                                style = TangemTheme.typography3.body.medium,
                                color = TangemTheme.colors3.text.secondary,
                            )
                        },
                        valueSlot = {
                            Text(
                                text = "Value",
                                style = TangemTheme.typography3.body.medium,
                                color = TangemTheme.colors3.text.primary,
                            )
                        },
                    )
                }
            },
            footer = {
                TangemButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = stringReference(value = "Label"),
                    onClick = {},
                )
            },
        )
    }
}

@Composable
private fun PreviewAsset(variant: TangemStatusScreen.Variant) {
    val kind = when (variant) {
        TangemStatusScreen.Variant.Success -> TangemStatusIcon.Kind.Checkmark
        TangemStatusScreen.Variant.Error -> TangemStatusIcon.Kind.HeartBroken
        TangemStatusScreen.Variant.Warning -> TangemStatusIcon.Kind.Warning
        TangemStatusScreen.Variant.Neutral,
        TangemStatusScreen.Variant.Info,
        -> TangemStatusIcon.Kind.Info
    }
    if (variant == TangemStatusScreen.Variant.Neutral) {
        TangemStatusIcon(
            kind = kind,
            modifier = Modifier.size(40.dp),
            tint = TangemTheme.colors3.icon.secondary,
            animated = false,
        )
    } else {
        TangemStatusIcon(kind = kind, modifier = Modifier.size(40.dp), animated = false)
    }
}

private class VariantProvider : CollectionPreviewParameterProvider<TangemStatusScreen.Variant>(
    collection = TangemStatusScreen.Variant.entries,
)