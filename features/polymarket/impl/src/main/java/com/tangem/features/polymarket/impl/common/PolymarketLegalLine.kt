package com.tangem.features.polymarket.impl.common

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
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

internal object PolymarketLegalUrls {

    const val TANGEM_TERMS = "https://tangem.com/tangem_tos.html"

    val polymarketTerms: String
        get() = PolymarketUrlBuilder.build(page = PolymarketUrlBuilder.Page.Terms)
}

/**
 * The feature's consent line, shown wherever the user commits to something: before onboarding starts and
 * before an order is placed.
 */
@Composable
internal fun PolymarketLegalLine(
    onPolymarketTermsClick: () -> Unit,
    onTangemTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkStyle = SpanStyle(color = TangemTheme.colors3.text.primary)
    val polymarketTitle = stringResourceSafe(R.string.prediction_onboarding_legal_polymarket_terms)
    val tangemTitle = stringResourceSafe(R.string.prediction_onboarding_legal_tangem_terms)
    val fullText = stringResourceSafe(R.string.prediction_onboarding_legal, polymarketTitle, tangemTitle)

    val links = listOf(
        Triple(fullText.indexOf(polymarketTitle), polymarketTitle, onPolymarketTermsClick),
        Triple(fullText.indexOf(tangemTitle), tangemTitle, onTangemTermsClick),
    )
        .filter { it.first >= 0 }
        .sortedBy { it.first }

    val text = buildAnnotatedString {
        var cursor = 0
        links.forEach { (index, title, onClick) ->
            if (index < cursor) return@forEach
            append(fullText.substring(cursor, index))
            withLink(LinkAnnotation.Clickable(tag = title, linkInteractionListener = { onClick() })) {
                withStyle(linkStyle) { append(title) }
            }
            cursor = index + title.length
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