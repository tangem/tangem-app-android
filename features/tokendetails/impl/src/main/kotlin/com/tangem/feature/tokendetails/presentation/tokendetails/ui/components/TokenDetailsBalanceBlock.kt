package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.common.Strings.STARS
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun TokenDetailsBalanceBlock(
    state: TokenDetailsBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = TangemTheme.shapes.roundedCornersXMedium,
        color = TangemTheme.colors.background.primary,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing12)
                    .padding(horizontal = TangemTheme.dimens.spacing12)
                    .fillMaxWidth()
                    .heightIn(min = TangemTheme.dimens.spacing24),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(id = R.string.common_balance_title),
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.subtitle2,
                    maxLines = 1,
                )
            }
            FiatBalance(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing4)
                    .padding(horizontal = TangemTheme.dimens.spacing12),
            )
            CryptoBalance(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier
                    .padding(top = TangemTheme.dimens.spacing4)
                    .padding(horizontal = TangemTheme.dimens.spacing12),
            )

            HorizontalActionChips(
                buttons = state.actionButtons.map(TokenDetailsActionButton::config).toImmutableList(),
                modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
                contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun FiatBalance(
    state: TokenDetailsBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TokenDetailsBalanceBlockState.Loading -> RectangleShimmer(
            modifier = modifier.size(
                width = TangemTheme.dimens.size102,
                height = TangemTheme.dimens.size24,
            ),
        )
        is TokenDetailsBalanceBlockState.Content -> Text(
            modifier = modifier,
            text = if (isBalanceHidden) STARS else state.fiatBalance,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
        is TokenDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = if (isBalanceHidden) STARS else BigDecimalFormatter.EMPTY_BALANCE_SIGN,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Composable
private fun CryptoBalance(
    state: TokenDetailsBalanceBlockState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TokenDetailsBalanceBlockState.Loading -> RectangleShimmer(
            modifier = modifier.size(
                width = TangemTheme.dimens.size70,
                height = TangemTheme.dimens.size16,
            ),
        )
        is TokenDetailsBalanceBlockState.Content -> Text(
            modifier = modifier,
            text = if (isBalanceHidden) STARS else state.cryptoBalance,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
        is TokenDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = if (isBalanceHidden) STARS else BigDecimalFormatter.EMPTY_BALANCE_SIGN,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Preview(widthDp = 328, heightDp = 152)
@Preview(widthDp = 328, heightDp = 152, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TokenDetailsBalanceBlock(
    @PreviewParameter(TokenDetailsBalanceBlockStateProvider::class) state: TokenDetailsBalanceBlockState,
) {
    TangemThemePreview {
        TokenDetailsBalanceBlock(state = state, isBalanceHidden = false)
    }
}

private class TokenDetailsBalanceBlockStateProvider : CollectionPreviewParameterProvider<TokenDetailsBalanceBlockState>(
    collection = listOf(
        TokenDetailsPreviewData.balanceLoading,
        TokenDetailsPreviewData.balanceContent,
        TokenDetailsPreviewData.balanceError,
    ),
)