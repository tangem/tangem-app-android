package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.res.TangemTheme

internal const val INLINE_IMAGE_PLACEHOLDER = "%image%"
private const val INLINE_IMAGE_ID = "inline_subtitle_icon_"
private val DEFAULT_INLINE_ICON_SIZE = 16.dp
private val INLINE_IMAGE_GAP = 4.dp

/**
 * Width a history row gives an address. The row has more room than that, but the design draws the address short there
 * and spends the rest on the amount; only the details card lets an address take everything it can get.
 */
internal val HISTORY_ROW_ADDRESS_MAX_WIDTH = 87.dp

/**
 * Single-line caption whose text — a pre-formatted string resource — carries one or more [INLINE_IMAGE_PLACEHOLDER]
 * markers, each replaced in order by the matching entry of [icons]. All text is drawn in [color] and ellipsizes on a
 * single line.
 *
 * Use a resource shaped like `"prefix %%image%% %1\$s in %%image%% %2\$s"` (escaped `%` so the marker survives Lokalise),
 * pre-format it via `stringResourceSafe`, and pass the result as [template]. The `icons` list should have one entry per
 * marker; a marker beyond the last icon is skipped (renders nothing) and extra icons are ignored.
 */
@Composable
internal fun InlineImagesText(
    template: String,
    icons: List<@Composable () -> Unit>,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = DEFAULT_INLINE_ICON_SIZE,
    textStyle: TextStyle = TangemTheme.typography2.captionMedium12,
) {
    val parts = remember(template) { template.split(INLINE_IMAGE_PLACEHOLDER) }
    val iconSizeSp = with(LocalDensity.current) { iconSize.toSp() }
    val placeholder = remember(iconSizeSp) {
        Placeholder(width = iconSizeSp, height = iconSizeSp, placeholderVerticalAlign = PlaceholderVerticalAlign.Center)
    }
    val inlineContent = icons.mapIndexed { index, icon ->
        "$INLINE_IMAGE_ID$index" to InlineTextContent(placeholder = placeholder, children = { icon() })
    }.toMap()
    val annotated = remember(parts, icons.size) {
        buildAnnotatedString {
            parts.forEachIndexed { index, part ->
                // The icon preceding this part (none before the first part); skip a marker that has no matching icon.
                val iconIndex = index - 1
                if (iconIndex in icons.indices) {
                    appendInlineContent("$INLINE_IMAGE_ID$iconIndex", INLINE_IMAGE_PLACEHOLDER)
                }
                append(part)
            }
        }
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        color = color,
        style = textStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Single-line caption with exactly one inline [icon] between two text parts — the common one-marker case of
 * [InlineImagesText].
 */
@Composable
internal fun InlineImageSubtitle(
    template: String,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = DEFAULT_INLINE_ICON_SIZE,
    textStyle: TextStyle = TangemTheme.typography2.captionMedium12,
    icon: @Composable () -> Unit,
) {
    InlineImagesText(
        template = template,
        icons = listOf(icon),
        color = color,
        iconSize = iconSize,
        textStyle = textStyle,
        modifier = modifier,
    )
}

/**
 * [InlineImageSubtitle] whose value is a blockchain [address] — "to: {identicon} 33Bd321fS2p2o...1cEZga21412B".
 *
 * A row of three parts rather than the single [Text] of [InlineImageSubtitle]: the address takes whatever width the
 * prefix and the icon leave — capped at [HISTORY_ROW_ADDRESS_MAX_WIDTH] — and is shortened from its middle to fit
 * ([EllipsisText]), instead of the whole line being cut at its end. [template] is the direction resource pre-formatted with the **raw** address, so the prefix and the
 * marker position stay translator-controlled; the literal spacing around the marker is replaced by a fixed gap,
 * since a trailing space carries no width of its own once the line is split.
 */
@Composable
internal fun InlineAddressSubtitle(
    template: String,
    address: String,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = DEFAULT_INLINE_ICON_SIZE,
    textStyle: TextStyle = TangemTheme.typography2.captionMedium12,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(INLINE_IMAGE_GAP),
    ) {
        val prefix = template.substringBefore(INLINE_IMAGE_PLACEHOLDER).trim()
        if (prefix.isNotEmpty()) {
            Text(text = prefix, color = color, style = textStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(modifier = Modifier.size(iconSize)) { icon() }
        // The cap belongs on a wrapper, not on the EllipsisText itself: it hands its own modifier to the text it
        // measures, so a width there would wrap that measurement and leave the address unshortened.
        Box(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .widthIn(max = HISTORY_ROW_ADDRESS_MAX_WIDTH),
        ) {
            EllipsisText(
                text = address,
                color = color,
                style = textStyle,
                ellipsis = TextEllipsis.Middle,
            )
        }
    }
}