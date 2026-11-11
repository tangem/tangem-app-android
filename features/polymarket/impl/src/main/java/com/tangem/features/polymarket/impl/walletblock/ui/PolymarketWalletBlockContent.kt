package com.tangem.features.polymarket.impl.walletblock.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.account.PredictionAccountIcon
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.toBoxSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRow
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.row.token.internal.TokenRowTitle
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.polymarket.api.walletblock.PolymarketWalletBlockUM
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardCornerRadius
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.walletblock.PREDICTION_ACCOUNT_ROW_ID
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun PolymarketWalletBlockContent(
    state: PolymarketWalletBlockUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
    isRedesignedTokenRowsEnabled: Boolean = false,
) {
    when (state) {
        is PolymarketWalletBlockUM.Hidden -> Unit
        is PolymarketWalletBlockUM.Content -> if (isRedesignedTokenRowsEnabled) {
            PolymarketAccountRow(state = state, isBalanceHidden = isBalanceHidden, modifier = modifier)
        } else {
            val rowUM = state.toRowUM()

            // The overload the wallet's own account rows use: it pads the head slot the same way and takes the
            // click from the model, so this row cannot drift from them
            TangemTokenRow(
                tokenRowUM = rowUM,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier.roundedShapeItemDecoration(
                    radius = PolymarketCardCornerRadius,
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
private fun PolymarketAccountRow(
    state: PolymarketWalletBlockUM.Content,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier
            .clip(PolymarketCardShape)
            .background(TangemTheme.colors3.bg.secondary),
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = state.onClick,
        startSlot = {
            TangemAvatar(
                state = TangemAvatar.UiState.Account(kind = TangemAvatar.AccountKind.Prediction),
                size = TangemAvatar.Size.X40,
            )
        },
        titleSlot = {
            TangemRowText(
                text = resourceReference(R.string.prediction_account_title),
                role = TangemRowTextRole.Title,
            )
        },
        subtitleSlot = {
            TangemRowText(
                text = resourceReference(R.string.prediction_account_open_detailed_view),
                role = TangemRowTextRole.Subtitle,
            )
        },
        valueSlot = {
            when (val balance = state.balance) {
                is PolymarketWalletBlockUM.Balance.Loading -> TextShimmer(
                    style = TangemTheme.typography3.body.medium,
                    radius = 10.dp,
                    modifier = Modifier.width(64.dp),
                )
                // A dash, not a zero: a zero would claim the account is empty, which is exactly what is unknown
                is PolymarketWalletBlockUM.Balance.Unknown -> PolymarketBalance(
                    text = stringReference(DASH_SIGN),
                    state = state,
                )
                is PolymarketWalletBlockUM.Balance.Amount -> PolymarketBalance(
                    text = balance.text.orMaskWithStars(isBalanceHidden),
                    state = state,
                )
            }
        },
    )
}

@Composable
private fun PolymarketBalance(text: TextReference, state: PolymarketWalletBlockUM.Content) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (state.isBalanceFromCache) {
            Icon(
                painter = painterResource(CoreUiR.drawable.ic_error_sync_24),
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            text = text.resolveAnnotatedReference(),
            style = TangemTheme.typography3.body.medium.applyBladeBrush(
                isEnabled = state.isBalanceFlickering,
                textColor = TangemTheme.colors3.text.primary,
            ),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@RemoveWithToggle("AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED")
@Composable
private fun PolymarketWalletBlockUM.Content.toRowUM(): TangemTokenRowUM = TangemTokenRowUM.Content(
    id = PREDICTION_ACCOUNT_ROW_ID,
    headIconUM = TangemIconUM.Currency(
        currencyIconState = CurrencyIconState.CryptoPortfolio.Icon(
            imageVector = PredictionAccountIcon.imageVector,
            color = PredictionAccountIcon.color,
            isGrayscale = false,
            size = AccountIconSize.Default,
        ),
    ),
    titleUM = TangemTokenRowUM.TitleUM.Content(text = resourceReference(R.string.prediction_account_title)),
    subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
        text = resourceReference(R.string.prediction_account_open_detailed_view),
    ),
    topEndContentUM = toEndContentUM(),
    bottomEndContentUM = TangemTokenRowUM.EndContentUM.Empty,
    onItemClick = onClick,
    onItemLongClick = null,
)

@RemoveWithToggle("AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED")
@Composable
private fun PolymarketWalletBlockUM.Content.toEndContentUM(): TangemTokenRowUM.EndContentUM =
    when (val balance = balance) {
        is PolymarketWalletBlockUM.Balance.Loading -> TangemTokenRowUM.EndContentUM.Loading
        // A dash, not a zero: a zero would claim the account is empty, which is exactly what is unknown
        is PolymarketWalletBlockUM.Balance.Unknown -> endContent(text = stringReference(DASH_SIGN))
        is PolymarketWalletBlockUM.Balance.Amount -> endContent(text = balance.text)
    }

@RemoveWithToggle("AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED")
@Composable
private fun PolymarketWalletBlockUM.Content.endContent(text: TextReference) = TangemTokenRowUM.EndContentUM.Content(
    text = text,
    isFlickering = isBalanceFlickering,
    startIcons = buildList {
        addIf(
            element = TangemIconUM.Icon(
                iconRes = CoreUiR.drawable.ic_error_sync_default_24,
                tintReference = { TangemTheme.colors3.icon.secondary },
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