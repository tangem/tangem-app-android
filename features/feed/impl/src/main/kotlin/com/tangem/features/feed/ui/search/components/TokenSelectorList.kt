package com.tangem.features.feed.ui.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.common.ui.account.getResId
import com.tangem.common.ui.account.getUiColor
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.search.state.AccountHeaderData
import com.tangem.features.feed.ui.search.state.TokenSelectorSectionUM
import kotlinx.collections.immutable.ImmutableList

internal fun LazyListScope.tokenSelectorSectionItems(sections: ImmutableList<TokenSelectorSectionUM>) {
    sections.forEachIndexed { index, section ->
        when (section) {
            is TokenSelectorSectionUM.WalletHeader -> {
                item(key = "wallet_${section.walletName}_$index") {
                    WalletHeaderSection(section)
                }
            }
            is TokenSelectorSectionUM.TokenGroup -> {
                if (section.items.isEmpty()) return@forEachIndexed

                if (index > 0 && sections[index - 1] is TokenSelectorSectionUM.TokenGroup) {
                    item(key = "spacer_before_group_$index") {
                        SpacerH(TangemTheme.dimens2.x2)
                    }
                }

                val lastIndex = if (section.accountHeader != null) {
                    section.items.size
                } else {
                    section.items.lastIndex.coerceAtLeast(0)
                }

                section.accountHeader?.let { header ->
                    item(key = "account_header_$index") {
                        TokenGroupAccountHeaderRow(
                            data = header,
                            modifier = Modifier.tokenGroupRowDecoration(
                                currentIndex = 0,
                                lastIndex = lastIndex,
                            ),
                        )
                    }
                }

                val indexOffset = if (section.accountHeader != null) 1 else 0
                section.items.forEachIndexed { itemIndex, single ->
                    item(key = "token_${single.id}_$index") {
                        SingleUserAssetItem(
                            item = single,
                            modifier = Modifier.tokenGroupRowDecoration(
                                currentIndex = indexOffset + itemIndex,
                                lastIndex = lastIndex,
                            ),
                            shouldUsePriceBlock = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.tokenGroupRowDecoration(currentIndex: Int, lastIndex: Int): Modifier =
    this.roundedShapeItemDecoration(
        currentIndex = currentIndex,
        lastIndex = lastIndex,
        addDefaultPadding = false,
        radius = TangemTheme.dimens2.x6,
        backgroundColor = TangemTheme.colors2.surface.level3,
    )

@Composable
private fun WalletHeaderSection(section: TokenSelectorSectionUM.WalletHeader) {
    Row(
        modifier = Modifier
            .padding(top = TangemTheme.dimens2.x4, bottom = TangemTheme.dimens2.x2)
            .padding(horizontal = TangemTheme.dimens2.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
    ) {
        Text(
            text = section.walletName,
            style = TangemTheme.typography2.subheadlineMedium14,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_key_card_20),
            modifier = Modifier.size(TangemTheme.dimens2.x5),
            tint = TangemTheme.colors2.graphic.neutral.tertiary,
            contentDescription = null,
        )
    }
}

@Composable
private fun TokenGroupAccountHeaderRow(data: AccountHeaderData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = TangemTheme.dimens2.x4, bottom = TangemTheme.dimens2.x2)
            .padding(horizontal = TangemTheme.dimens2.x4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x4),
            imageVector = ImageVector.vectorResource(data.cryptoPortfolioIcon.value.getResId()),
            tint = data.cryptoPortfolioIcon.color.getUiColor(),
            contentDescription = null,
        )
        Text(
            text = data.accountName.resolveReference(),
            style = TangemTheme.typography2.captionMedium12,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}