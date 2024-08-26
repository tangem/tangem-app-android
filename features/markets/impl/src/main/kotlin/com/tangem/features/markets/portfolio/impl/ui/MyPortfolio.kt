package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SmallButtonShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewMyPortfolioUMProvider
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM

@Composable
internal fun MyPortfolio(state: MyPortfolioUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        contentHorizontalPadding = 0.dp,
        title = {
            Text(
                text = stringResource(R.string.markets_common_my_portfolio),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
        action = {
            if (state !is MyPortfolioUM.Tokens) return@InformationBlock

            when (state.buttonState) {
                MyPortfolioUM.Tokens.AddButtonState.Loading -> {
                    SmallButtonShimmer(
                        modifier = Modifier.size(width = 63.dp, height = TangemTheme.dimens.size18),
                        shape = RoundedCornerShape(TangemTheme.dimens.radius3),
                    )
                }
                else -> {
                    SecondarySmallButton(
                        config = SmallButtonConfig(
                            text = resourceReference(R.string.markets_add_token),
                            icon = TangemButtonIconPosition.Start(R.drawable.ic_plus_24),
                            onClick = state.onAddClick,
                            enabled = state.buttonState == MyPortfolioUM.Tokens.AddButtonState.Available,
                        ),
                    )
                }
            }
        },
    ) {
        when (state) {
            is MyPortfolioUM.Tokens -> TokenList(state = state)
            is MyPortfolioUM.AddFirstToken -> AddFirstTokenContent(state = state)
            MyPortfolioUM.Loading -> LoadingPlaceholder()
            MyPortfolioUM.Unavailable -> UnavailableContent()
        }
    }
}

@Composable
private fun TokenList(state: MyPortfolioUM.Tokens, modifier: Modifier = Modifier) {
    Column(modifier) {
        state.tokens.fastForEachIndexed { index, token ->
            PortfolioItem(
                state = token,
                lastInList = index == state.tokens.size - 1,
            )
        }
    }
}

@Composable
private fun UnavailableContent(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            ),
        text = stringResource(R.string.markets_add_to_my_portfolio_unavailable_description),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
private fun AddFirstTokenContent(state: MyPortfolioUM.AddFirstToken, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            ),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = "To start buying, exchanging or receiving this asset, add this token to at least 1 network",
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.markets_add_to_portfolio_button),
            onClick = state.onAddClick,
        )
    }
}

@Composable
private fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            ),
    ) {
        TextShimmer(
            modifier = Modifier.fillMaxWidth(),
            style = TangemTheme.typography.body2,
            textSizeHeight = true,
        )
        TextShimmer(
            modifier = Modifier.fillMaxWidth(fraction = 0.7f),
            style = TangemTheme.typography.body2,
            textSizeHeight = true,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PreviewMyPortfolioUMProvider::class) state: MyPortfolioUM) {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing8),
        ) {
            MyPortfolio(state)
        }
    }
}

@Preview
@Composable
private fun PreviewRtl(@PreviewParameter(PreviewMyPortfolioUMProvider::class) state: MyPortfolioUM) {
    TangemThemePreview(rtl = true) {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing8),
        ) {
            MyPortfolio(state)
        }
    }
}