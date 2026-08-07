@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.messagebubble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_16_filled
import com.tangem.core.ui.res.generated.icons.ic_info_16

/**
 * Design-system v2 (DS3) **Message Bubble** — a compact caption pill with an optional leading icon,
 * an optional close button and an optional "tip" tail on top pointing at the anchored element.
 * DS3 replacement for the legacy `TokenRowPromoBanner`.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=6327-22326)
 *
 * @param text Bubble message, rendered in caption/medium typography.
 * @param modifier Modifier applied to the bubble. The bubble hugs its content in both dimensions.
 * @param variant Visual appearance — background, text and tip colors.
 * @param showTip Whether the tail on top of the bubble is drawn. `false` shows only the pill.
 * @param icon Leading 16dp icon, tinted with the [variant] content color. `null` hides it.
 * @param onClick Invoked when the bubble body is tapped. `null` makes the bubble non-interactive.
 * @param onClose Invoked when the close button is tapped. `null` hides the button.
 * @param closeContentDescription Accessibility label for the close button announced by TalkBack
 * (e.g. `"Dismiss"`). Supply it whenever [onClose] is set.
 */
@Suppress("LongParameterList")
@Composable
fun TangemMessageBubble(
    text: TextReference,
    modifier: Modifier = Modifier,
    variant: TangemMessageBubble.Variant = TangemMessageBubble.Variant.Neutral,
    showTip: Boolean = true,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    closeContentDescription: String? = null,
) {
    val tokens = variant.tokens()
    val shape = if (showTip) MessageBubbleShape else RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(tokens.background)
            .conditionalCompose(onClick != null) {
                clickableSingle(role = Role.Button, onClick = requireNotNull(onClick))
            }
            .padding(
                start = 8.dp,
                top = if (showTip) 12.dp else 4.dp,
                end = if (onClose != null) 4.dp else 8.dp,
                bottom = 4.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tokens.content,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text.resolveAnnotatedReference(),
            style = TangemTheme.typography3.caption.medium,
            color = tokens.content,
        )
        if (onClose != null) {
            CloseButton(
                onClick = onClose,
                tint = tokens.closeIcon,
                contentDescription = closeContentDescription,
            )
        }
    }
}

/** Public API surface of [TangemMessageBubble]. */
object TangemMessageBubble {

    /** Visual appearance — background, text and tip colors. */
    enum class Variant {
        /** Neutral tertiary background with secondary text. */
        Neutral,

        /** Subtle success-green background with success text. */
        Success,

        /** Subtle info-blue background with info text. */
        Info,
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, tint: Color, contentDescription: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(start = 4.dp)
            .size(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(28.dp)
                .clip(RoundedCornerShape(percent = 50))
                .clickableSingle(onClick = onClick)
                .semantics {
                    role = Role.Button
                    contentDescription?.let { this.contentDescription = it }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_cross_circle_16_filled,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/**
 * Pill with the tip tail as a single-path [Shape]: an 8x8 swoosh (apex at the top-start corner,
 * concave curve down to the bottom-end — the geometry of the legacy `shape_triangular` drawable)
 * sitting on a 12dp-rounded rect. One shape means no anti-aliasing seam between tail and pill, no
 * double-painting of translucent background tokens, and a ripple clipped to the full silhouette.
 */
private object MessageBubbleShape : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path.combine(
            operation = PathOperation.Union,
            path1 = density.pillPath(size),
            path2 = density.tipPath(size, layoutDirection),
        )
        return Outline.Generic(path)
    }

    private fun Density.pillPath(size: Size): Path = Path().apply {
        addRoundRect(
            RoundRect(
                left = 0f,
                top = 8.dp.toPx(),
                right = size.width,
                bottom = size.height,
                cornerRadius = CornerRadius(12.dp.toPx()),
            ),
        )
    }

    private fun Density.tipPath(size: Size, layoutDirection: LayoutDirection): Path {
        val tipWidth = 8.dp.toPx()
        val tipHeight = 8.dp.toPx()
        // Extends below the tip band, into the pill, to guarantee the union shapes overlap.
        val skirtHeight = 2.dp.toPx()
        val startX = when (layoutDirection) {
            LayoutDirection.Ltr -> 16.dp.toPx()
            LayoutDirection.Rtl -> size.width - 16.dp.toPx() - tipWidth
        }
        // Maps a 0..1 fraction of the tip width to an x coordinate, mirrored in RTL.
        fun x(fraction: Float): Float = when (layoutDirection) {
            LayoutDirection.Ltr -> startX + fraction * tipWidth
            LayoutDirection.Rtl -> startX + (1 - fraction) * tipWidth
        }

        // Vector drawable path "M8,8 L0,8 L0,0 C0,0 2,6 8,8 Z" in an 8x8 viewport.
        return Path().apply {
            moveTo(x(1f), tipHeight + skirtHeight)
            lineTo(x(0f), tipHeight + skirtHeight)
            lineTo(x(0f), 0f)
            cubicTo(
                x1 = x(0f),
                y1 = 0f,
                x2 = x(0.25f),
                y2 = tipHeight * 0.75f,
                x3 = x(1f),
                y3 = tipHeight,
            )
            close()
        }
    }
}

/** Resolved appearance tokens for a [TangemMessageBubble.Variant]. */
private data class MessageBubbleTokens(val background: Color, val content: Color, val closeIcon: Color)

@Composable
@ReadOnlyComposable
private fun TangemMessageBubble.Variant.tokens(): MessageBubbleTokens {
    val colors = TangemTheme.colors3
    return when (this) {
        TangemMessageBubble.Variant.Neutral -> MessageBubbleTokens(
            background = colors.bg.tertiary,
            content = colors.text.secondary,
            closeIcon = colors.icon.secondary,
        )
        TangemMessageBubble.Variant.Success -> MessageBubbleTokens(
            background = colors.bg.status.successSubtle,
            content = colors.text.status.success,
            closeIcon = colors.icon.status.success,
        )
        TangemMessageBubble.Variant.Info -> MessageBubbleTokens(
            background = colors.bg.status.infoSubtle,
            content = colors.text.status.info,
            closeIcon = colors.icon.status.info,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemMessageBubblePreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemMessageBubble.Variant.entries.forEach { variant ->
                TangemMessageBubble(
                    text = stringReference("Description"),
                    variant = variant,
                    icon = Icons.ic_info_16,
                    onClick = {},
                    onClose = {},
                    closeContentDescription = "Dismiss",
                )
            }
            TangemMessageBubble(
                text = stringReference("Text only"),
                showTip = false,
            )
            TangemMessageBubble(
                text = stringReference("No icon"),
                variant = TangemMessageBubble.Variant.Info,
                onClose = {},
                closeContentDescription = "Dismiss",
            )
        }
    }
}