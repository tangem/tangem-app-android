package com.tangem.features.polymarket.impl.history.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.AccountResIcon
import com.tangem.core.ui.components.currency.icon.TangemCurrencyIcon
import com.tangem.core.ui.extensions.appendColored
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM.Direction
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivitySubtitleUM.Funding

@Composable
internal fun PolymarketActivitySubtitle(subtitle: PolymarketActivitySubtitleUM, modifier: Modifier = Modifier) {
    when (subtitle) {
        is PolymarketActivitySubtitleUM.Plain -> Text(
            text = subtitle.text.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        is Funding -> FundingSubtitle(subtitle = subtitle, modifier = modifier)
    }
}

@Composable
private fun FundingSubtitle(subtitle: Funding, modifier: Modifier = Modifier) {
    val iconSize = with(LocalDensity.current) { 16.dp.toSp() }
    val placeholder = Placeholder(iconSize, iconSize, PlaceholderVerticalAlign.Center)
    val icons = buildMap {
        subtitle.icon?.let { icon ->
            put(
                TOKEN_ICON_ID,
                InlineTextContent(placeholder) {
                    TangemCurrencyIcon(icon, modifier = Modifier.fillMaxSize(), shouldDisplayNetwork = false)
                },
            )
        }
        subtitle.account?.let { account ->
            put(
                ACCOUNT_ICON_ID,
                InlineTextContent(placeholder) {
                    AccountResIcon(
                        resId = account.iconResId,
                        color = account.iconBackgroundColor,
                        size = AccountIconSize.ExtraSmall,
                    )
                },
            )
        }
    }
    Text(
        text = fundingText(subtitle),
        inlineContent = icons,
        color = TangemTheme.colors3.text.secondary,
        style = TangemTheme.typography3.caption.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun fundingText(subtitle: Funding): AnnotatedString {
    val primary = TangemTheme.colors3.text.primary
    val symbol = buildAnnotatedString { appendColored(subtitle.symbol, primary) }
    val asset = subtitle.account?.let { account ->
        val name = account.name.resolveReference()
        resourceReference(
            R.string.token_details_toolbar_title_token_in_account,
            wrappedList(symbol, buildAnnotatedString { appendColored(name, primary) }),
        ).resolveAnnotatedReference().withInlineIcon(ACCOUNT_ICON_ID)
    } ?: symbol
    val direction = when (subtitle.direction) {
        Direction.FROM -> R.string.transaction_history_from_inline_address
        Direction.TO -> R.string.transaction_history_to_inline_address
    }
    return resourceReference(direction, wrappedList(asset)).resolveAnnotatedReference()
        .withInlineIcon(TOKEN_ICON_ID.takeIf { subtitle.icon != null })
}

private fun AnnotatedString.withInlineIcon(id: String?): AnnotatedString {
    val source = this
    val start = text.indexOf(INLINE_IMAGE_MARKER)
    if (start < 0) return this
    val end = start + INLINE_IMAGE_MARKER.length
    return buildAnnotatedString {
        append(source.subSequence(0, start))
        if (id != null) appendInlineContent(id)
        val tailStart = if (id == null && text.getOrNull(end) == ' ') end + 1 else end
        append(source.subSequence(tailStart, source.length))
    }
}

private const val INLINE_IMAGE_MARKER = "%image%"
private const val TOKEN_ICON_ID = "activity_token"
private const val ACCOUNT_ICON_ID = "activity_account"