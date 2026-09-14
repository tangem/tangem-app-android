@file:Suppress("MagicNumber")

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.ambient.TangemAmbientBackground
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.StatusScreenTestTags
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
    val entrance = rememberEntranceProgress(durationMs = 1400, animated = animated)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        TangemAmbientBackground(
            variant = variant.ambientVariant,
            placement = TangemAmbientBackground.Placement.Screen,
            progress = { entrance.segment(startMs = 1000, endMs = 1400, easing = SymmetricEase) },
            modifier = Modifier.matchParentSize(),
        )
        TangemGlowRing(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = entrance.segment(startMs = 1000, endMs = 1300, easing = PulseEase) },
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
                    .graphicsLayer {
                        val settled = entrance.segment(startMs = 1000, endMs = 1300, easing = DecelerateEase)
                        translationY = bottomBlockHeight / 2f * (1f - settled)
                    }
                    .verticalScroll(rememberScrollState()),
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
                                alpha = entrance.segment(startMs = 1000, endMs = 1200, easing = SymmetricEase)
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
                                alpha = entrance.segment(startMs = 1150, endMs = 1350, easing = SymmetricEase)
                                translationY = 90.dp.toPx() *
                                    (1f - entrance.segment(startMs = 1150, endMs = 1400, easing = DecelerateEase))
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
            asset()
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(StatusScreenTestTags.TITLE)
                    .graphicsLayer {
                        translationY = (-17).dp.toPx() *
                            (1f - entrance.segment(startMs = 400, endMs = 700, easing = SpringEase))
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
                        .testTag(StatusScreenTestTags.DESCRIPTION)
                        .graphicsLayer {
                            alpha = entrance.segment(startMs = 550, endMs = 850, easing = SpringEase)
                            translationY = (-16).dp.toPx() *
                                (1f - entrance.segment(startMs = 400, endMs = 700, easing = SpringEase))
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
                    alpha = entrance.segment(startMs = 550, endMs = 850, easing = SymmetricEase)
                },
            ) {
                extraBottom()
            }
        }
    }
}

object TangemStatusScreen {

    /** Color mood of the screen — picks the ambient light's hue and the glow ring's variant. */
    enum class Variant(
        internal val glowVariant: TangemGlowRing.Variant,
        internal val ambientVariant: TangemAmbientBackground.Variant,
    ) {
        Neutral(
            glowVariant = TangemGlowRing.Variant.Magic,
            ambientVariant = TangemAmbientBackground.Variant.Neutral,
        ),
        Success(
            glowVariant = TangemGlowRing.Variant.Success,
            ambientVariant = TangemAmbientBackground.Variant.Green,
        ),
        Error(
            glowVariant = TangemGlowRing.Variant.Error,
            ambientVariant = TangemAmbientBackground.Variant.Red,
        ),
        Warning(
            glowVariant = TangemGlowRing.Variant.Warning,
            ambientVariant = TangemAmbientBackground.Variant.Yellow,
        ),
        Info(
            glowVariant = TangemGlowRing.Variant.Info,
            ambientVariant = TangemAmbientBackground.Variant.Blue,
        ),
    }
}

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