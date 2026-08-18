package com.tangem.features.polymarket.impl.walletblock.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.account.AccountRow
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.polymarket.impl.R
import com.tangem.utils.StringsSigns.DASH_SIGN

@Composable
internal fun PolymarketWalletBlockContent(
    state: PolymarketWalletBlockUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is PolymarketWalletBlockUM.Hidden -> Unit
        is PolymarketWalletBlockUM.Loading -> PredictionAccountLoading(state, modifier)
        is PolymarketWalletBlockUM.Content -> PredictionAccountContent(state, isBalanceHidden, modifier)
        is PolymarketWalletBlockUM.Unavailable -> PredictionAccountUnavailable(state, modifier)
    }
}

@Composable
private fun PredictionAccountContent(
    state: PolymarketWalletBlockUM.Content,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    AccountRow(
        modifier = modifier.predictionAccountCard(),
        title = title,
        subtitle = state.subtitle,
        icon = AccountIconUM.Prediction,
        balance = state.balance.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
        isBalanceFlickering = state.isBalanceFlickering,
        isBalanceFromCache = state.isBalanceFromCache,
        onClick = state.onClick,
    )
}

/** A dash, not a zero: a zero would claim the account is empty, which is exactly what is unknown here. */
@Composable
private fun PredictionAccountUnavailable(state: PolymarketWalletBlockUM.Unavailable, modifier: Modifier = Modifier) {
    AccountRow(
        modifier = modifier.predictionAccountCard(),
        title = title,
        subtitle = state.subtitle,
        icon = AccountIconUM.Prediction,
        balance = stringReference(DASH_SIGN).resolveAnnotatedReference(),
        onClick = state.onClick,
    )
}

@Composable
private fun PredictionAccountLoading(state: PolymarketWalletBlockUM.Loading, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.predictionAccountCard(),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            AccountIcon(name = title, icon = AccountIconUM.Prediction, size = AccountIconSize.Default)
        },
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        subtitleSlot = { TangemRowText(text = state.subtitle, role = TangemRowTextRole.Subtitle) },
        valueSlot = {
            TextShimmer(
                style = TangemTheme.typography3.body.medium,
                radius = 10.dp,
                modifier = Modifier.width(width = 80.dp),
            )
        },
    )
}

private val title: TextReference
    get() = resourceReference(R.string.prediction_account_title)

@Composable
private fun Modifier.predictionAccountCard(): Modifier = this
    .clip(RoundedCornerShape(size = 24.dp))
    .background(TangemTheme.colors3.bg.secondary)

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketWalletBlockContent_Preview(
    @PreviewParameter(PolymarketWalletBlockPreviewProvider::class) state: PolymarketWalletBlockUM,
) {
    TangemThemePreviewRedesign {
        PolymarketWalletBlockContent(
            state = state,
            isBalanceHidden = false,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 16.dp)
                .size(width = 360.dp, height = 72.dp),
        )
    }
}

private class PolymarketWalletBlockPreviewProvider : CollectionPreviewParameterProvider<PolymarketWalletBlockUM>(
    collection = listOf(
        PolymarketWalletBlockUM.Loading(subtitle = stringReference("Prediction markets")),
        PolymarketWalletBlockUM.Content(
            subtitle = stringReference("Prediction markets"),
            balance = stringReference("$1,234.00"),
            isBalanceFlickering = false,
            isBalanceFromCache = false,
            onClick = {},
        ),
        PolymarketWalletBlockUM.Content(
            subtitle = stringReference("Prediction markets"),
            balance = stringReference("$1,234.00"),
            isBalanceFlickering = true,
            isBalanceFromCache = true,
            onClick = {},
        ),
        PolymarketWalletBlockUM.Unavailable(subtitle = stringReference("Unavailable"), onClick = {}),
    ),
)
// endregion