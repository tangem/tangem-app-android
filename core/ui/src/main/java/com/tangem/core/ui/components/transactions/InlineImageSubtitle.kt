package com.tangem.core.ui.components.transactions

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.res.TangemTheme

internal const val INLINE_IMAGE_PLACEHOLDER = "%image%"
private const val INLINE_IMAGE_ID = "inline_subtitle_icon"

/**
 * Single-line caption with an inline icon between two text parts.
 *
 * Use a string resource of the shape `"prefix %%image%% %1\$s"` (escaped `%` so the marker
 * survives Lokalise round-trips), pre-format it via `stringResourceSafe`, and pass the result
 * here — [INLINE_IMAGE_PLACEHOLDER] is replaced with an [InlineTextContent] driven by [icon].
 */
@Composable
internal fun InlineImageSubtitle(
    template: String,
    color: Color,
    modifier: Modifier = Modifier,
    afterIconColor: Color = color,
    iconSize: Dp = TangemTheme.dimens2.x4,
    icon: @Composable () -> Unit,
) {
    val parts = remember(template) {
        val split = template.split(INLINE_IMAGE_PLACEHOLDER, limit = 2)
        if (split.size == 2) split[0] to split[1] else template to ""
    }
    val iconSizeSp = with(LocalDensity.current) { iconSize.toSp() }
    val inlineContent = remember(iconSizeSp) {
        mapOf(
            INLINE_IMAGE_ID to InlineTextContent(
                placeholder = Placeholder(
                    width = iconSizeSp,
                    height = iconSizeSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = { icon() },
            ),
        )
    }
    val annotated = remember(parts, afterIconColor) {
        buildAnnotatedString {
            append(parts.first)
            appendInlineContent(INLINE_IMAGE_ID, INLINE_IMAGE_PLACEHOLDER)
            withStyle(SpanStyle(color = afterIconColor)) {
                append(parts.second)
            }
        }
    }
    Text(
        text = annotated,
        inlineContent = inlineContent,
        color = color,
        style = TangemTheme.typography2.captionMedium12,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}