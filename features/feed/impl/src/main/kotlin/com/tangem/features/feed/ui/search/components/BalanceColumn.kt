package com.tangem.features.feed.ui.search.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.flicker
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.search.state.BalanceDisplayState
import com.tangem.utils.StringsSigns

@Composable
internal fun BalanceColumn(
    balanceState: BalanceDisplayState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isBalanceHidden) {
        HiddenBalance(modifier)
        return
    }
    when (balanceState) {
        is BalanceDisplayState.Loading -> LoadingBalanceColumn(modifier)
        is BalanceDisplayState.Flickering -> FlickeringBalanceColumn(balanceState, modifier)
        is BalanceDisplayState.Stale -> StaleBalanceColumn(balanceState, modifier)
        is BalanceDisplayState.Unreachable -> UnreachableBalanceColumn(modifier)
        is BalanceDisplayState.Loaded -> LoadedBalanceColumn(balanceState, modifier)
    }
}

@Composable
private fun BalanceColumnLayout(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun LoadingBalanceColumn(modifier: Modifier = Modifier) {
    BalanceColumnLayout(modifier) {
        RectangleShimmer(modifier = Modifier.size(width = 108.dp, height = 20.dp), radius = 20.dp)
        RectangleShimmer(modifier = Modifier.size(width = 64.dp, height = 16.dp), radius = 20.dp)
    }
}

@Composable
private fun FlickeringBalanceColumn(state: BalanceDisplayState.Flickering, modifier: Modifier = Modifier) {
    val flickerModifier = Modifier.flicker(isFlickering = true)
    BalanceColumnLayout(modifier) {
        Text(
            modifier = flickerModifier,
            text = state.fiatBalance.resolveAnnotatedReference(),
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = flickerModifier,
            text = state.cryptoBalance.resolveReference(),
            style = TangemTheme.typography2.captionMedium12,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StaleBalanceColumn(state: BalanceDisplayState.Stale, modifier: Modifier = Modifier) {
    BalanceColumnLayout(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_error_sync_24),
                contentDescription = null,
                tint = TangemTheme.colors2.markers.iconGray,
            )
            SpacerW(TangemTheme.dimens2.x0_5)
            Text(
                text = state.fiatBalance.resolveAnnotatedReference(),
                style = TangemTheme.typography2.bodyMedium16,
                color = TangemTheme.colors2.text.neutral.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = state.cryptoBalance.resolveReference(),
            style = TangemTheme.typography2.captionMedium12,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UnreachableBalanceColumn(modifier: Modifier = Modifier) {
    BalanceColumnLayout(modifier) {
        Text(
            text = StringsSigns.DASH_SIGN,
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResourceSafe(R.string.common_unreachable),
                style = TangemTheme.typography2.captionMedium12,
                color = TangemTheme.colors2.text.status.attention,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SpacerW(TangemTheme.dimens2.x0_5)
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(id = R.drawable.ic_alert_triange_24),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.status.attention,
            )
        }
    }
}

@Composable
private fun LoadedBalanceColumn(state: BalanceDisplayState.Loaded, modifier: Modifier = Modifier) {
    BalanceColumnLayout(modifier) {
        Text(
            text = state.fiatBalance.resolveAnnotatedReference(),
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
        )
        Text(
            text = state.cryptoBalance.resolveReference(),
            style = TangemTheme.typography2.captionMedium12,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun HiddenBalance(modifier: Modifier = Modifier) {
    BalanceColumnLayout(modifier) {
        Text(
            text = StringsSigns.THREE_STARS,
            style = TangemTheme.typography2.bodySemibold16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
        )
        Text(
            text = StringsSigns.THREE_STARS,
            style = TangemTheme.typography2.captionMedium12,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
        )
    }
}