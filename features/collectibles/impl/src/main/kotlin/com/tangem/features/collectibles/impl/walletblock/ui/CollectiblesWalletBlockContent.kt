package com.tangem.features.collectibles.impl.walletblock.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.CollectiblesAccountIcon
import com.tangem.core.ui.components.account.toBoxSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockUM
import com.tangem.utils.StringsSigns.DASH_SIGN

@Composable
internal fun CollectiblesWalletBlockContent(
    state: CollectiblesWalletBlockUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is CollectiblesWalletBlockUM.Hidden -> Unit
        is CollectiblesWalletBlockUM.Content -> {
            TangemRow(
                modifier = modifier.roundedShapeItemDecoration(
                    radius = 24.dp,
                    currentIndex = 0,
                    addDefaultPadding = false,
                    lastIndex = 0,
                    backgroundColor = TangemTheme.colors3.bg.secondary,
                ),
                contentLead = TangemRowContentLead.Start,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                // TODO([REDACTED_TASK_KEY]): move the title to Lokalise once the collectibles keys are created
                titleSlot = {
                    TangemRowText(text = stringReference(value = "Packs account"), role = TangemRowTextRole.Title)
                },
                valueSlot = { BalanceValue(balance = state.balance, isBalanceHidden = isBalanceHidden) },
                startSlot = {
                    TangemIcon(
                        tangemIconUM = collectiblesIconUM(),
                        modifier = Modifier.size(AccountIconSize.Default.toBoxSize()),
                    )
                },
                onClick = state.onClick,
            )
        }
    }
}

@Composable
private fun BalanceValue(balance: CollectiblesWalletBlockUM.Balance, isBalanceHidden: Boolean) {
    when (balance) {
        is CollectiblesWalletBlockUM.Balance.Loading -> TextShimmer(
            style = TangemTheme.typography3.body.medium,
            modifier = Modifier.width(BALANCE_PLACEHOLDER_WIDTH),
            radius = BALANCE_PLACEHOLDER_RADIUS,
        )
        // A dash, not a zero: a zero would claim the vault is empty, which is exactly what is unknown
        is CollectiblesWalletBlockUM.Balance.Unknown -> TangemRowText(
            text = stringReference(DASH_SIGN),
            role = TangemRowTextRole.Value,
        )
        is CollectiblesWalletBlockUM.Balance.Amount -> TangemRowText(
            text = balance.text.orMaskWithStars(isBalanceHidden),
            role = TangemRowTextRole.Value,
        )
    }
}

@Composable
private fun collectiblesIconUM() = TangemIconUM.Currency(
    currencyIconState = CurrencyIconState.CryptoPortfolio.Icon(
        resId = CollectiblesAccountIcon.resId,
        color = CollectiblesAccountIcon.color,
        isGrayscale = false,
        size = AccountIconSize.Default,
    ),
)

private val BALANCE_PLACEHOLDER_WIDTH = 80.dp
private val BALANCE_PLACEHOLDER_RADIUS = 100.dp

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesWalletBlockContent_Preview(
    @PreviewParameter(CollectiblesWalletBlockPreviewProvider::class) state: CollectiblesWalletBlockUM,
) {
    TangemThemePreviewRedesign {
        CollectiblesWalletBlockContent(
            state = state,
            isBalanceHidden = false,
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(all = 12.dp),
        )
    }
}

private class CollectiblesWalletBlockPreviewProvider :
    CollectionPreviewParameterProvider<CollectiblesWalletBlockUM>(
        collection = listOf(
            previewContent(balance = CollectiblesWalletBlockUM.Balance.Unknown),
            previewContent(balance = CollectiblesWalletBlockUM.Balance.Loading),
            previewContent(balance = CollectiblesWalletBlockUM.Balance.Amount(stringReference("$12.00"))),
        ),
    )

private fun previewContent(balance: CollectiblesWalletBlockUM.Balance) = CollectiblesWalletBlockUM.Content(
    balance = balance,
    onClick = {},
)
// endregion