package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.constraintlayout.compose.ConstraintLayout
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.components.buttons.segmentedbutton.SegmentedButtons
import com.tangem.core.ui.components.flicker
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.feature.tokendetails.presentation.tokendetails.TokenDetailsPreviewData
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsActionButton
import com.tangem.features.tokendetails.impl.R
import kotlinx.collections.immutable.toImmutableList

@Suppress("DestructuringDeclarationWithTooManyEntries")
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
        val spacing4 = TangemTheme.dimens.spacing4
        val spacing10 = TangemTheme.dimens.spacing10
        val spacing12 = TangemTheme.dimens.spacing12

        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            val (balanceTitle, toggleButtons, fiatBalance, cryptoBalance, actionChips) = createRefs()

            Text(
                text = stringResourceSafe(id = R.string.common_balance_title),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.subtitle2,
                modifier = Modifier.constrainAs(balanceTitle) {
                    top.linkTo(anchor = parent.top, margin = spacing12)
                    start.linkTo(anchor = parent.start, margin = spacing12)
                },
            )

            BalanceButtons(
                state = state,
                modifier = Modifier.constrainAs(toggleButtons) {
                    top.linkTo(anchor = parent.top)
                    end.linkTo(anchor = parent.end, margin = spacing10)
                },
            )

            FiatBalance(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.constrainAs(fiatBalance) {
                    top.linkTo(anchor = balanceTitle.bottom, margin = spacing4)
                    start.linkTo(anchor = parent.start, margin = spacing12)
                },
            )

            CryptoBalance(
                state = state,
                isBalanceHidden = isBalanceHidden,
                modifier = Modifier.constrainAs(cryptoBalance) {
                    top.linkTo(anchor = fiatBalance.bottom, margin = spacing4)
                    start.linkTo(anchor = parent.start, margin = spacing12)
                },
            )

            HorizontalActionChips(
                buttons = state.actionButtons.map(TokenDetailsActionButton::config).toImmutableList(),
                modifier = Modifier
                    .constrainAs(actionChips) {
                        top.linkTo(anchor = cryptoBalance.bottom, margin = spacing12)
                        start.linkTo(anchor = parent.start)
                        end.linkTo(anchor = parent.end)
                        bottom.linkTo(anchor = parent.bottom, margin = spacing12)
                    },
                containerColor = TangemTheme.colors.background.primary,
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
                height = TangemTheme.dimens.size32,
            ),
        )
        is TokenDetailsBalanceBlockState.Content -> Text(
            modifier = modifier.flicker(state.isBalanceFlickering),
            text = state.displayFiatBalance.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )
        is TokenDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = BigDecimalFormatter.EMPTY_BALANCE_SIGN.orMaskWithStars(isBalanceHidden),
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
            modifier = modifier.flicker(state.isBalanceFlickering),
            text = state.displayCryptoBalance.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
        is TokenDetailsBalanceBlockState.Error -> Text(
            modifier = modifier,
            text = BigDecimalFormatter.EMPTY_BALANCE_SIGN.orMaskWithStars(isBalanceHidden),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun BalanceButtons(state: TokenDetailsBalanceBlockState, modifier: Modifier = Modifier) {
    if (state !is TokenDetailsBalanceBlockState.Content || !state.isBalanceSelectorEnabled) return

    SegmentedButtons(
        config = state.balanceSegmentedButtonConfig,
        onClick = state.onBalanceSelect,
        showIndication = false,
        modifier = modifier
            .padding(top = TangemTheme.dimens.spacing11)
            .width(IntrinsicSize.Min),
    ) { config ->
        val style = if (state.selectedBalanceType == config.type) {
            TangemTheme.typography.caption1
        } else {
            TangemTheme.typography.caption2
        }
        Text(
            text = config.title.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            style = style,
            maxLines = 1,
            modifier = Modifier
                .padding(
                    horizontal = TangemTheme.dimens.spacing6,
                    vertical = TangemTheme.dimens.spacing4,
                )
                .align(Alignment.Center),
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
        TokenDetailsPreviewData.balanceContent.copy(isBalanceFlickering = true),
        TokenDetailsPreviewData.balanceError,
    ),
)