package com.tangem.features.polymarket.impl.walletblock.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.account.PredictionAccountIcon
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.toBoxSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRow
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.row.token.internal.TokenRowTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.walletblock.PREDICTION_ACCOUNT_ROW_ID
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun PolymarketWalletBlockContent(
    state: PolymarketWalletBlockUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is PolymarketWalletBlockUM.Hidden -> Unit
        is PolymarketWalletBlockUM.Content -> {
            val rowUM = state.toRowUM()

            // The overload the wallet's own account rows use: it pads the head slot the same way and takes the
            // click from the model, so this row cannot drift from them
            TangemTokenRow(
                tokenRowUM = rowUM,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.roundedShapeItemDecoration(
                    radius = 20.dp,
                    currentIndex = 0,
                    addDefaultPadding = false,
                    lastIndex = 0,
                    backgroundColor = TangemTheme.colors3.bg.secondary,
                ),
                headComponent = { headModifier ->
                    TangemIcon(
                        tangemIconUM = rowUM.headIconUM,
                        modifier = headModifier.size(AccountIconSize.Default.toBoxSize()),
                    )
                },
                titleComponent = { titleModifier ->
                    TokenRowTitle(titleUM = rowUM.titleUM, modifier = titleModifier)
                },
            )
        }
    }
}

@Composable
private fun PolymarketWalletBlockUM.Content.toRowUM(): TangemTokenRowUM = TangemTokenRowUM.Content(
    id = PREDICTION_ACCOUNT_ROW_ID,
    headIconUM = TangemIconUM.Currency(
        currencyIconState = CurrencyIconState.CryptoPortfolio.Icon(
            resId = PredictionAccountIcon.resId,
            color = PredictionAccountIcon.color,
            isGrayscale = false,
            size = AccountIconSize.Default,
        ),
    ),
    titleUM = TangemTokenRowUM.TitleUM.Content(text = resourceReference(R.string.prediction_account_title)),
    // Empty until the positions contract lands: the mock shows a count of open predictions, which
    // nothing can supply yet
    subtitleUM = TangemTokenRowUM.SubtitleUM.Empty,
    topEndContentUM = toEndContentUM(),
    bottomEndContentUM = TangemTokenRowUM.EndContentUM.Empty,
    onItemClick = onClick,
    onItemLongClick = null,
)

@Composable
private fun PolymarketWalletBlockUM.Content.toEndContentUM(): TangemTokenRowUM.EndContentUM =
    when (val balance = balance) {
        is PolymarketWalletBlockUM.Balance.Loading -> TangemTokenRowUM.EndContentUM.Loading
        // A dash, not a zero: a zero would claim the account is empty, which is exactly what is unknown
        is PolymarketWalletBlockUM.Balance.Unknown -> endContent(text = stringReference(DASH_SIGN))
        is PolymarketWalletBlockUM.Balance.Amount -> endContent(text = balance.text)
    }

@Composable
private fun PolymarketWalletBlockUM.Content.endContent(text: TextReference) = TangemTokenRowUM.EndContentUM.Content(
    text = text,
    isFlickering = isBalanceFlickering,
    startIcons = buildList {
        addIf(
            element = TangemIconUM.Icon(
                iconRes = CoreUiR.drawable.ic_error_sync_default_24,
                tintReference = { TangemTheme.colors2.graphic.neutral.tertiary },
            ),
            condition = isBalanceFromCache,
        )
    }.toImmutableList(),
)

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
                .padding(all = 12.dp),
        )
    }
}

private class PolymarketWalletBlockPreviewProvider :
    CollectionPreviewParameterProvider<PolymarketWalletBlockUM>(
        collection = listOf(
            previewContent(balance = PolymarketWalletBlockUM.Balance.Amount(stringReference("$1,234.00"))),
            previewContent(balance = PolymarketWalletBlockUM.Balance.Loading),
            previewContent(balance = PolymarketWalletBlockUM.Balance.Unknown),
            previewContent(
                balance = PolymarketWalletBlockUM.Balance.Amount(stringReference("$1,234.00")),
                isBalanceFromCache = true,
            ),
        ),
    )

private fun previewContent(balance: PolymarketWalletBlockUM.Balance, isBalanceFromCache: Boolean = false) =
    PolymarketWalletBlockUM.Content(
        balance = balance,
        isBalanceFlickering = false,
        isBalanceFromCache = isBalanceFromCache,
        onClick = {},
    )
// endregion