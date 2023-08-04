package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.features.tokendetails.impl.R

@Composable
internal fun TokenDetailsBalanceBlock(state: TokenDetailsBalanceBlockState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
    ) {
        Column {
            Text(
                modifier = Modifier
                    .padding(
                        top = TangemTheme.dimens.spacing12,
                        start = TangemTheme.dimens.spacing12,
                        end = TangemTheme.dimens.spacing12,
                    ),
                text = stringResource(id = R.string.onboarding_balance_title),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body2,
                maxLines = 1,
            )
            FiatBalance(
                state = state,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing4)
                    .padding(horizontal = TangemTheme.dimens.spacing12),
            )
            CryptoBalance(
                state = state,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing4)
                    .padding(horizontal = TangemTheme.dimens.spacing12),
            )

            HorizontalActionChips(
                buttons = state.actionButtons,
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
                contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun FiatBalance(state: TokenDetailsBalanceBlockState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenDetailsBalanceBlockState.Loading -> RectangleShimmer(
            modifier = modifier.size(
                width = TangemTheme.dimens.size102,
                height = TangemTheme.dimens.size24,
            ),
        )
        is TokenDetailsBalanceBlockState.Content -> Text(
            modifier = modifier,
            text = state.fiatBalance,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
        is TokenDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Composable
private fun CryptoBalance(state: TokenDetailsBalanceBlockState, modifier: Modifier = Modifier) {
    when (state) {
        is TokenDetailsBalanceBlockState.Loading -> RectangleShimmer(
            modifier = modifier.size(
                width = TangemTheme.dimens.size70,
                height = TangemTheme.dimens.size16,
            ),
        )
        is TokenDetailsBalanceBlockState.Content -> Text(
            modifier = modifier,
            text = state.cryptoBalance,
            style = TangemTheme.typography.caption,
            color = TangemTheme.colors.text.primary1,
        )
        is TokenDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = BigDecimalFormatter.EMPTY_BALANCE_SIGN,
            style = TangemTheme.typography.caption,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsBalanceBlock_LightTheme(
    @PreviewParameter(TokenDetailsBalanceBlockStateProvider::class) state: TokenDetailsBalanceBlockState,
) {
    TangemTheme(isDark = false) {
        TokenDetailsBalanceBlock(state)
    }
}

@Preview
@Composable
private fun Preview_TokenDetailsBalanceBlock_DarkTheme(
    @PreviewParameter(TokenDetailsBalanceBlockStateProvider::class) state: TokenDetailsBalanceBlockState,
) {
    TangemTheme(isDark = true) {
        TokenDetailsBalanceBlock(state)
    }
}

private class TokenDetailsBalanceBlockStateProvider : CollectionPreviewParameterProvider<TokenDetailsBalanceBlockState>(
    collection = listOf(
        TokenDetailsPreviewData.balanceLoading,
        TokenDetailsPreviewData.balanceContent,
        TokenDetailsPreviewData.balanceError,
    ),
)
