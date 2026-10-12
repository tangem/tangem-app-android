package com.tangem.features.tangempay.addfunds

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.tangem.core.ui.res.TangemTheme

internal data class LegalLink(val title: String, val onClick: () -> Unit)

@Composable
internal fun TangemPayLegalLinksText(fullText: String, links: List<LegalLink>, modifier: Modifier = Modifier) {
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)

    // Locate each link title in the resolved (localized) string and splice them in appearance order.
    // Handles translations that reorder the %1$s/%2$s placeholders and skips a title that a translation
    // does not contain verbatim — falling back to plain text instead of crashing on an invalid substring range.
    val placedLinks = links
        .map { link -> fullText.indexOf(link.title) to link }
        .filter { (index, _) -> index >= 0 }
        .sortedBy { (index, _) -> index }

    val text = buildAnnotatedString {
        var cursor = 0
        placedLinks.forEach { (index, link) ->
            if (index < cursor) return@forEach
            append(fullText.substring(cursor, index))
            withLink(LinkAnnotation.Clickable(tag = link.title, linkInteractionListener = { link.onClick() })) {
                withStyle(linkStyle) { append(link.title) }
            }
            cursor = index + link.title.length
        }
        append(fullText.substring(cursor))
    }
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
        textAlign = TextAlign.Center,
    )
}