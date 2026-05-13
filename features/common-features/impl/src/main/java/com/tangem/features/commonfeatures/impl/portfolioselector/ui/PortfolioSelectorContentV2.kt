package com.tangem.features.commonfeatures.impl.portfolioselector.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.userwallet.CardImage
import com.tangem.common.ui.userwallet.getBalanceValueAndFlickerState
import com.tangem.common.ui.userwallet.getInformationValue
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalCanScrollBackward
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorItemUM
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorUM
import com.tangem.utils.StringsSigns.DOT

private const val DISABLED_WALLET_ALPHA = 0.5f

@Composable
internal fun PortfolioSelectorContentV2(
    state: PortfolioSelectorUM,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val lazyListState = rememberLazyListState()

    CompositionLocalProvider(
        LocalCanScrollBackward provides
            lazyListState.canScrollBackward,
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = modifier,
            contentPadding = contentPadding,
        ) {
            val items = state.items
            itemsIndexed(
                items = items,
                key = { _, item -> item.id },
            ) { index, item ->
                when (item) {
                    is PortfolioSelectorItemUM.Portfolio ->
                        PortfolioSelectorItem(
                            state = item.item,
                            modifier = Modifier
                                .roundedShapeItemDecoration(
                                    currentIndex = index,
                                    lastIndex = state.items.lastIndex,
                                    backgroundColor = TangemTheme.colors2.surface.level3,
                                    radius = TangemTheme.dimens2.x5,
                                    addDefaultPadding = false,
                                )
                                .clickable(enabled = item.item.isEnabled, onClick = item.item.onClick)
                                .conditional(!item.item.isEnabled) { alpha(DISABLED_WALLET_ALPHA) },
                        )
                    is PortfolioSelectorItemUM.GroupTitle -> WalletNameRow(
                        model = item,
                        modifier = Modifier
                            .fillMaxWidth()
                            .roundedShapeItemDecoration(
                                currentIndex = index,
                                lastIndex = state.items.lastIndex,
                                backgroundColor = TangemTheme.colors2.surface.level3,
                                radius = TangemTheme.dimens2.x5,
                                addDefaultPadding = false,
                            )
                            .padding(horizontal = TangemTheme.dimens.spacing16),
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioSelectorItem(state: UserWalletItemUM, modifier: Modifier = Modifier) {
    TangemRowContainer(modifier = modifier) {
        Box(
            modifier = Modifier
                .layoutId(TangemRowLayoutId.HEAD)
                .padding(end = TangemTheme.dimens2.x3),
            contentAlignment = Alignment.Center,
        ) {
            CardImage(
                modifier = Modifier.size(40.dp),
                imageState = state.imageState,
            )
        }

        Text(
            modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_TOP),
            text = state.name.resolveReference(),
            style = TangemTheme.typography2.bodyMedium16,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        InfoRow(
            modifier = Modifier.layoutId(layoutId = TangemRowLayoutId.START_BOTTOM),
            information = state.information,
            balance = state.balance,
        )
    }
}

@Composable
private fun InfoRow(
    information: UserWalletItemUM.Information,
    balance: UserWalletItemUM.Balance,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = information,
            label = "Information content",
        ) { information ->
            val informationValue = getInformationValue(information)

            if (informationValue == null) {
                TextShimmer(
                    style = TangemTheme.typography2.captionMedium12,
                    text = "aaaaa",
                )
            } else {
                Text(
                    text = informationValue,
                    style = TangemTheme.typography2.captionMedium12,
                    color = TangemTheme.colors2.text.neutral.secondary,
                    maxLines = 1,
                )
            }
        }

        Row {
            Text(
                text = " $DOT ",
                style = TangemTheme.typography2.captionMedium12,
                color = TangemTheme.colors2.text.neutral.secondary,
                maxLines = 1,
            )

            val (balanceValue, isFlickering) = getBalanceValueAndFlickerState(balance)

            if (balanceValue == null) {
                TextShimmer(
                    style = TangemTheme.typography2.captionMedium12,
                    text = "aaaaa",
                )
            } else {
                Text(
                    text = balanceValue,
                    style = TangemTheme.typography2.captionMedium12.applyBladeBrush(
                        isEnabled = isFlickering,
                        textColor = TangemTheme.colors2.text.neutral.secondary,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WalletNameRow(model: PortfolioSelectorItemUM.GroupTitle, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = TangemTheme.dimens2.x4, bottom = TangemTheme.dimens2.x2),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterVertically),
            text = model.name.resolveReference(),
            style = TangemTheme.typography2.subheadlineMedium14,
            color = TangemTheme.colors2.text.neutral.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        TangemDeviceIcon(
            state = model.deviceIcon,
            modifier = Modifier
                .align(Alignment.Bottom)
                .size(TangemTheme.dimens2.x5),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PortfolioSelectorContentV2(
                    state = params,
                    modifier = Modifier.background(color = TangemTheme.colors.background.tertiary),
                )
            }
        }
    }
}