package com.tangem.features.markets.portfolio.impl.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.common.ui.account.AccountTitle
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SmallButtonShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.ui.preview.PreviewMyPortfolioUMProvider
import com.tangem.features.markets.portfolio.impl.ui.state.*
import com.tangem.features.markets.portfolio.impl.ui.state.MyPortfolioUM.Tokens.AddButtonState

@Composable
internal fun MyPortfolio(state: MyPortfolioUM, modifier: Modifier = Modifier) {
    if (state is MyPortfolioUM.Content) {
        val contentModifier = Modifier.padding(
            start = TangemTheme.dimens.spacing16,
            top = TangemTheme.dimens.spacing20,
            end = TangemTheme.dimens.spacing16,
            bottom = TangemTheme.dimens.spacing32,
        )
        PortfolioList(state, contentModifier)
        return
    }
    InformationBlock(
        modifier = modifier,
        contentHorizontalPadding = TangemTheme.dimens.spacing0,
        title = { Title() },
        action = {
            if (state !is MyPortfolioUM.Tokens) return@InformationBlock

            AddButton(state = state.buttonState, onClick = state.onAddClick)
        },
    ) {
        val contentModifier = Modifier.padding(
            start = TangemTheme.dimens.spacing12,
            end = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing12,
        )

        when (state) {
            is MyPortfolioUM.Tokens -> TokenList(state = state)
            is MyPortfolioUM.AddFirstToken -> AddFirstTokenContent(state = state, modifier = contentModifier)
            MyPortfolioUM.Loading -> LoadingPlaceholder(modifier = contentModifier)
            MyPortfolioUM.Unavailable -> UnavailableAsset(modifier = contentModifier)
            MyPortfolioUM.UnavailableForWallet -> UnavailableAssetForWallet(modifier = contentModifier)
            is MyPortfolioUM.Content -> PortfolioList(state = state)
        }
    }

    val bsConfig = state.addToPortfolioBSConfig
    if (bsConfig != null) {
        AddToPortfolioBottomSheet(config = bsConfig)
    }
}

@Composable
private fun Title() {
    Text(
        text = stringResourceSafe(R.string.markets_common_my_portfolio),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
private fun AddButton(state: AddButtonState, onClick: () -> Unit) {
    when (state) {
        AddButtonState.Loading -> {
            Box {
                SmallButtonShimmer(
                    modifier = Modifier.width(width = TangemTheme.dimens.size63),
                    shape = RoundedCornerShape(TangemTheme.dimens.radius3),
                    withIcon = true,
                )

                Box(
                    Modifier
                        .matchParentSize()
                        .background(TangemTheme.colors.background.action),
                )

                RectangleShimmer(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = TangemTheme.dimens.size63, height = TangemTheme.dimens.size18),
                    radius = TangemTheme.dimens.radius3,
                )
            }
        }
        AddButtonState.Available,
        AddButtonState.Unavailable,
        -> {
            SecondarySmallButton(
                config = SmallButtonConfig(
                    text = resourceReference(R.string.markets_add_token),
                    icon = TangemButtonIconPosition.Start(R.drawable.ic_plus_24),
                    onClick = onClick,
                    isEnabled = state == AddButtonState.Available,
                ),
            )
        }
    }
}

@Composable
private fun TokenList(state: MyPortfolioUM.Tokens, modifier: Modifier = Modifier) {
    Column(modifier) {
        state.tokens.fastForEachIndexed { index, token ->
            key(token.tokenItemState.id) {
                PortfolioItem(
                    modifier = Modifier.background(color = TangemTheme.colors.background.action),
                    state = token,
                    lastInList = index == state.tokens.size - 1,
                )
            }
        }
    }
}

@Composable
private fun PortfolioList(state: MyPortfolioUM.Content, modifier: Modifier = Modifier) {
    Column(modifier) {
        key("PortfolioListHeader") {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResourceSafe(R.string.markets_common_my_portfolio),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                )
                AddButton(state = state.buttonState, onClick = state.onAddClick)
            }
        }

        state.items.fastForEachIndexed { index, item ->
            val previousItem = state.items.getOrNull(index.dec())
            val nextItem = state.items.getOrNull(index.inc())
            val itemModifier = Modifier
                .fillMaxWidth()
                .getOffsetModifier(item, previousItem)
                .getBackgroundModifier(item, previousItem, nextItem)

            key(item.id) {
                PortfolioItem(
                    item = item,
                    modifier = itemModifier,
                    lastInList = index == state.items.size - 1,
                )
            }
        }
    }
}

@Composable
private fun Modifier.getBackgroundModifier(
    item: PortfolioListItem,
    previousItem: PortfolioListItem?,
    nextItem: PortfolioListItem?,
): Modifier {
    val color = TangemTheme.colors.background.action
    val radius = 14.dp
    val topRound = RoundedCornerShape(topStart = radius, topEnd = radius)
    val bottomRound = RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
    val allRound = RoundedCornerShape(size = radius)
    val backgroundModifier = when (item) {
        is WalletHeader -> this
        is PortfolioHeader -> this
            .clip(topRound)
            .background(color = color)
        is PortfolioTokenUM -> when {
            previousItem is PortfolioHeader && nextItem !is PortfolioTokenUM -> this
                .clip(bottomRound)
                .background(color = color)
            previousItem is WalletHeader && nextItem !is PortfolioTokenUM -> this
                .clip(allRound)
                .background(color = color)
            previousItem is PortfolioTokenUM && nextItem !is PortfolioTokenUM -> this
                .clip(bottomRound)
                .background(color = color)
            else -> this.background(color = color)
        }
    }
    return backgroundModifier
}

private fun Modifier.getOffsetModifier(item: PortfolioListItem, previousItem: PortfolioListItem?): Modifier = when {
    item is WalletHeader -> this.padding(top = 20.dp, start = 4.dp, end = 4.dp)
    item is PortfolioHeader && previousItem is PortfolioTokenUM -> this.padding(top = 12.dp)
    item is PortfolioHeader && previousItem == null -> this.padding(top = 20.dp)
    previousItem is WalletHeader -> this.padding(top = 12.dp)
    previousItem is PortfolioTokenUM -> this
    else -> this
}

@Composable
private fun PortfolioItem(item: PortfolioListItem, lastInList: Boolean, modifier: Modifier = Modifier) {
    when (item) {
        is PortfolioHeader -> AccountTitle(
            modifier = modifier.padding(
                start = 12.dp,
                top = 12.dp,
                bottom = 8.dp,
            ),
            accountTitleUM = item.state,
            textStyle = TangemTheme.typography.caption1,
            textColor = TangemTheme.colors.text.primary1,
        )
        is PortfolioTokenUM -> PortfolioItem(
            state = item,
            modifier = modifier,
            lastInList = lastInList,
        )
        is WalletHeader -> Text(
            modifier = modifier,
            text = item.name.resolveReference(),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Composable
fun UnavailableAsset(modifier: Modifier = Modifier) {
    UnavailableContent(
        textId = R.string.markets_add_to_my_portfolio_unavailable_description,
        modifier = modifier,
    )
}

@Composable
fun UnavailableAssetForWallet(modifier: Modifier = Modifier) {
    UnavailableContent(
        textId = R.string.markets_add_to_my_portfolio_unavailable_for_wallet_description,
        modifier = modifier,
    )
}

@Composable
private fun UnavailableContent(@StringRes textId: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResourceSafe(textId),
        style = TangemTheme.typography.body2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Composable
private fun AddFirstTokenContent(state: MyPortfolioUM.AddFirstToken, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResourceSafe(R.string.markets_add_to_my_portfolio_description),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_add_to_portfolio),
            onClick = state.onAddClick,
        )
    }
}

@Composable
private fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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
                .background(TangemTheme.colors.background.tertiary),
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