package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

internal const val INLINE_IMAGE_PLACEHOLDER = "%image%"
private const val INLINE_IMAGE_ID = "inline_subtitle_icon_"
private val DEFAULT_INLINE_ICON_SIZE = 16.dp

/**
 * Single-line caption whose text — a pre-formatted string resource — carries one or more [INLINE_IMAGE_PLACEHOLDER]
 * markers, each replaced in order by the matching entry of [icons]. All text is drawn in [color]; each substring listed
 * in [highlights] is repainted in [highlightColor]. The value ellipsizes on a single line.
 *
 * Highlighting by substring (not by position) keeps the coloring translation-safe: pass the **formatted argument
 * values** — the symbol, the account/wallet name, an address — as [highlights], so the template literals a translator
 * controls (a "to:" / "in" prefix, connectors) stay in the base [color] regardless of word order per language.
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
    highlights: List<String> = emptyList(),
    highlightColor: Color = color,
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
    val annotated = remember(parts, highlights, color, highlightColor, icons.size) {
        buildAnnotatedString {
            parts.forEachIndexed { index, part ->
                // The icon preceding this part (none before the first part); skip a marker that has no matching icon.
                val iconIndex = index - 1
                if (iconIndex in icons.indices) {
                    appendInlineContent("$INLINE_IMAGE_ID$iconIndex", INLINE_IMAGE_PLACEHOLDER)
                }
                appendHighlighted(
                    text = part,
                    highlights = highlights,
                    baseColor = color,
                    highlightColor = highlightColor,
                )
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

/** Appends [text] in [baseColor], repainting every occurrence of any [highlights] substring in [highlightColor]. */
private fun AnnotatedString.Builder.appendHighlighted(
    text: String,
    highlights: List<String>,
    baseColor: Color,
    highlightColor: Color,
) {
    val terms = highlights.filter { it.isNotEmpty() }
    if (terms.isEmpty()) {
        withStyle(SpanStyle(color = baseColor)) { append(text) }
        return
    }
    var index = 0
    while (index < text.length) {
        val next = terms
            .mapNotNull { term -> text.indexOf(term, index).takeIf { it >= 0 }?.let { it to term } }
            .minByOrNull { it.first }
        if (next == null) {
            withStyle(SpanStyle(color = baseColor)) { append(text.substring(index)) }
            return
        }
        val (start, term) = next
        if (start > index) withStyle(SpanStyle(color = baseColor)) { append(text.substring(index, start)) }
        withStyle(SpanStyle(color = highlightColor)) { append(term) }
        index = start + term.length
    }
}

/**
 * Single-line caption with exactly one inline [icon] between two text parts — the common one-marker case of
 * [InlineImagesText]. The text after the icon (the value) is painted in [afterIconColor].
 */
@Composable
internal fun InlineImageSubtitle(
    template: String,
    color: Color,
    modifier: Modifier = Modifier,
    afterIconColor: Color = color,
    iconSize: Dp = DEFAULT_INLINE_ICON_SIZE,
    textStyle: TextStyle = TangemTheme.typography2.captionMedium12,
    icon: @Composable () -> Unit,
) {
    val afterIcon = template.substringAfter(INLINE_IMAGE_PLACEHOLDER, missingDelimiterValue = "")
    InlineImagesText(
        template = template,
        icons = listOf(icon),
        color = color,
        highlights = listOfNotNull(afterIcon.ifEmpty { null }),
        highlightColor = afterIconColor,
        iconSize = iconSize,
        textStyle = textStyle,
        modifier = modifier,
    )
}