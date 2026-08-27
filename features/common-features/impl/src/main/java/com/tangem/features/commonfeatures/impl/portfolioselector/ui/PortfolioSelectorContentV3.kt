package com.tangem.features.commonfeatures.impl.portfolioselector.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.userwallet.CardImage
import com.tangem.common.ui.userwallet.getBalanceValueAndFlickerState
import com.tangem.common.ui.userwallet.getInformationValue
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalCanScrollBackward
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.commonfeatures.impl.portfolioselector.converter.PortfolioSelectorGroupPositionsConverter
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorItemUM
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorUM
import com.tangem.utils.StringsSigns.DOT
import kotlinx.collections.immutable.toImmutableList

private const val DISABLED_WALLET_ALPHA = 0.5f

@Composable
internal fun PortfolioSelectorContentV3(
    state: PortfolioSelectorUM,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp),
) {
    CompositionLocalProvider(
        LocalCanScrollBackward provides
            lazyListState.canScrollBackward,
    ) {
        val items = state.items

        LazyColumn(
            modifier = modifier,
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id },
            ) { index, item ->
                val pos = item.groupPosition
                val decoration = Modifier
                    .conditional(pos.isGroupStart && index != 0) {
                        padding(top = 8.dp)
                    }
                    .roundedShapeItemDecoration(
                        currentIndex = pos.indexInGroup,
                        lastIndex = pos.lastIndexInGroup,
                        backgroundColor = TangemTheme.colors3.bg.tertiary,
                        radius = 24.dp,
                        addDefaultPadding = false,
                    )

                when (item) {
                    is PortfolioSelectorItemUM.Portfolio -> PortfolioSelectorItem(
                        walletModel = item.item,
                        isSelected = item.isSelected,
                        isMultiChoiceEnabled = state.isMultiChoiceEnabled,
                        modifier = decoration
                            .clickable(enabled = item.item.isEnabled, onClick = item.item.onClick)
                            .conditional(!item.item.isEnabled) { alpha(DISABLED_WALLET_ALPHA) },
                    )
                    is PortfolioSelectorItemUM.GroupTitle -> WalletNameRow(
                        model = item,
                        isMultiChoiceEnabled = state.isMultiChoiceEnabled,
                        modifier = decoration,
                    )
                }
            }
        }
    }
}

@Composable
private fun PortfolioSelectorItem(
    walletModel: UserWalletItemUM,
    isSelected: Boolean,
    isMultiChoiceEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = {
            Box(
                modifier = Modifier.clip(CircleShape),
            ) {
                CardImage(
                    modifier = Modifier.size(40.dp),
                    imageState = walletModel.imageState,
                )
            }
        },
        titleSlot = { TangemRowText(text = walletModel.name.resolveReference(), role = TangemRowTextRole.Title) },
        subtitleSlot = { InfoRow(walletModel.information, walletModel.balance) },
        endSlot = {
            if (isMultiChoiceEnabled) {
                TangemCheckmark(
                    checked = isSelected,
                    isEnabled = walletModel.isEnabled,
                    onCheckedChange = {
                        walletModel.onClick()
                    },
                )
            }
        },
    )
}

@Composable
private fun RowScope.InfoRow(informationModel: UserWalletItemUM.Information, balance: UserWalletItemUM.Balance) {
    val information = getInformationValue(informationModel)

    if (information == null) {
        TangemShimmer(style = TangemTheme.typography3.caption.medium)
    } else {
        TangemRowText(text = information, role = TangemRowTextRole.Subtitle)
    }

    TangemRowText(text = " $DOT ", role = TangemRowTextRole.Subtitle)

    val (balanceValue, isFlickering) = getBalanceValueAndFlickerState(balance)

    if (balanceValue == null) {
        TangemShimmer(style = TangemTheme.typography3.caption.medium)
    } else {
        Text(
            text = balanceValue,
            style = TangemTheme.typography3.caption.medium.applyBladeBrush(
                isEnabled = isFlickering,
                textColor = TangemTheme.colors3.text.secondary,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun WalletNameRow(
    model: PortfolioSelectorItemUM.GroupTitle,
    isMultiChoiceEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val onClick = model.onClick?.takeIf { isMultiChoiceEnabled }

    TangemRow(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        contentLead = TangemRowContentLead.Start,
        titleSlot = {
            TangemRowText(model.name.resolveReference(), role = TangemRowTextRole.Title)
            TangemDeviceIcon(
                state = model.deviceIcon,
                modifier = Modifier.size(20.dp),
            )
        },
        endSlot = {
            if (onClick != null) {
                TangemCheckmark(
                    checked = model.isSelected,
                    onCheckedChange = { onClick() },
                )
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview(@PreviewParameter(PortfolioSelectorPreviewStateProvider::class) params: PortfolioSelectorUM) {
    val state = remember(params) {
        params.copy(items = PortfolioSelectorGroupPositionsConverter().convert(params.items).toImmutableList())
    }
    TangemThemePreviewRedesign {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            PortfolioSelectorContentV3(
                state = state,
                modifier = Modifier
                    .background(color = TangemTheme.colors3.bg.primary)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}