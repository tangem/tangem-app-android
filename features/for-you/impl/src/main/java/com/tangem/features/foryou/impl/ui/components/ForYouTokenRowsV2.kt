package com.tangem.features.foryou.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.common.ui.tokens.toTokenIconState
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.flicker
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeUM
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.expandable.ExpandableRowRevealItem
import com.tangem.core.ui.ds2.expandablerow.TangemExpandableRow
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.DonutSegmentColor
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouWalletGroupUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ForYouPortfolioTokenListV2(items: ImmutableList<ForYouTokenListItemUM>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        items.fastForEach { item ->
            key(item.tokenRowUM.id) {
                ForYouListItemV2(item = item)
            }
        }
    }
}

@Composable
internal fun ForYouEarnOpportunitiesTokenListV2(
    groups: ImmutableList<ForYouWalletGroupUM>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        groups.fastForEach { group ->
            group.header?.let { header ->
                key(header.id) {
                    WalletNameRow(header = header)
                }
            }
            group.items.fastForEach { item ->
                key(item.tokenRowUM.id) {
                    ForYouListItemV2(item = item)
                }
            }
        }
    }
}

@Composable
private fun ForYouListItemV2(item: ForYouTokenListItemUM) {
    val accountRow = item.accountRow
    when {
        accountRow != null -> ExpandableGroup(item = item) {
            TangemAccountRow(
                avatar = accountRow.avatar,
                state = accountRow.state,
                isExpanded = item.isExpanded,
                isExpandable = true,
                collapseContentDescription = stringResourceSafe(id = R.string.common_hide),
                shape = RectangleShape,
            )
        }
        item.isExpandable -> ExpandableGroup(item = item) {
            TangemExpandableRow(
                avatar = item.tokenRowUM.toAvatar(),
                state = item.tokenRowUM.toExpandableState(),
                isExpanded = item.isExpanded,
                isExpandable = true,
                collapseContentDescription = stringResourceSafe(id = R.string.common_hide),
                shape = RectangleShape,
            )
        }
        else -> ForYouTokenRow(
            row = item.tokenRowUM,
            segmentColor = item.segmentColor,
            modifier = Modifier.roundedShapeItemDecoration(
                currentIndex = 0,
                lastIndex = 0,
                radius = 24.dp,
                addDefaultPadding = false,
                backgroundColor = TangemTheme.colors3.bg.secondary,
            ),
        )
    }
    SpacerH8()
}

@Composable
private fun ExpandableGroup(item: ForYouTokenListItemUM, header: @Composable () -> Unit) {
    val count = item.tokenList.size
    Column(
        modifier = Modifier.roundedShapeItemDecoration(
            currentIndex = 0,
            lastIndex = 0,
            radius = 24.dp,
            addDefaultPadding = false,
            backgroundColor = TangemTheme.colors3.bg.secondary,
        ),
    ) {
        header()
        item.tokenList.fastForEachIndexed { index, row ->
            key(row.id) {
                ExpandableRowRevealItem(isExpanded = item.isExpanded, index = index, count = count) {
                    ForYouTokenRow(row = row, segmentColor = null)
                }
            }
        }
    }
}

@Composable
private fun ForYouTokenRow(row: TangemTokenRowUM, segmentColor: DonutSegmentColor?, modifier: Modifier = Modifier) {
    if (row !is TangemTokenRowUM.Content) {
        TangemTokenRow(state = TangemTokenRow.State.Shimmer(id = row.id), modifier = modifier)
        return
    }
    val title = row.titleUM as? TangemTokenRowUM.TitleUM.Content
    val subtitle = row.subtitleUM as? TangemTokenRowUM.SubtitleUM.Content
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = row.onItemClick,
        startSlot = { ForYouRowIcon(icon = row.headIconUM, segmentColor = segmentColor) },
        titleSlot = {
            if (title != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TangemRowText(
                        text = title.text.resolveAnnotatedReference(),
                        role = TangemRowTextRole.Title,
                        modifier = Modifier.weight(weight = 1f, fill = false),
                    )
                    title.badge?.let { badge -> SentimentBadge(badge = badge) }
                }
            }
        },
        subtitleSlot = subtitle?.let {
            {
                TangemRowText(
                    text = subtitle.text.resolveAnnotatedReference(),
                    role = TangemRowTextRole.Subtitle,
                    modifier = Modifier.flicker(isFlickering = subtitle.isFlickering),
                )
            }
        },
        valueSlot = row.topEndContentUM.slot(role = TangemRowTextRole.Value),
        subvalueSlot = row.bottomEndContentUM.slot(role = TangemRowTextRole.Subvalue),
    )
}

@Composable
private fun ForYouRowIcon(icon: TangemIconUM, segmentColor: DonutSegmentColor?) {
    val currencyIconState = (icon as? TangemIconUM.Currency)?.currencyIconState
    if (currencyIconState == null || currencyIconState is CurrencyIconState.Empty) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_empty_64),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.tertiary,
            modifier = Modifier.size(40.dp),
        )
        return
    }
    val state = when (val iconState = currencyIconState.toTokenIconState()) {
        is TangemTokenIcon.UiState.Token -> if (segmentColor != null) {
            iconState.copy(
                tokenState = iconState.tokenState.copy(
                    indicator = TangemTokenIcon.State.Indicator { segmentColor.getColor() },
                ),
            )
        } else {
            iconState
        }
        else -> iconState
    }
    TangemTokenIcon(state = state, size = TangemTokenIcon.Size.X40)
}

private fun TangemTokenRowUM.EndContentUM.slot(role: TangemRowTextRole): (@Composable RowScope.() -> Unit)? {
    val content = this as? TangemTokenRowUM.EndContentUM.Content ?: return null
    return {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content.startIcons.fastForEach { TangemIcon(tangemIconUM = it, modifier = Modifier.size(16.dp)) }
            TangemRowText(
                text = content.text.resolveAnnotatedReference(),
                role = role,
                modifier = Modifier.flicker(isFlickering = content.isFlickering),
            )
            content.endIcons.fastForEach { TangemIcon(tangemIconUM = it, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun SentimentBadge(badge: TangemBadgeUM) {
    TangemBadge(
        text = badge.text,
        variant = TangemBadge.Variant.Tinted,
        status = when (badge.color) {
            TangemBadgeColor.Green, TangemBadgeColor.GreenAlt -> TangemBadge.Status.Success
            TangemBadgeColor.Red -> TangemBadge.Status.Error
            TangemBadgeColor.Blue -> TangemBadge.Status.Info
            TangemBadgeColor.Gray -> TangemBadge.Status.Neutral
        },
        size = TangemBadge.Size.X4,
    )
}

private fun TangemTokenRowUM.toExpandableState(): TangemExpandableRow.State {
    if (this !is TangemTokenRowUM.Content) return TangemExpandableRow.State.Shimmer(id = id)
    val title = titleUM as? TangemTokenRowUM.TitleUM.Content
    return TangemExpandableRow.State.Content(
        id = id,
        title = title?.text ?: TextReference.EMPTY,
        badge = title?.badge?.text,
        subtitle = (subtitleUM as? TangemTokenRowUM.SubtitleUM.Content)?.text,
        value = (topEndContentUM as? TangemTokenRowUM.EndContentUM.Content)?.text,
        caption = (bottomEndContentUM as? TangemTokenRowUM.EndContentUM.Content)?.text,
        onClick = onItemClick,
    )
}

private fun TangemTokenRowUM.toAvatar(): TangemAvatar.UiState {
    return when (val iconState = (headIconUM as? TangemIconUM.Currency)?.currencyIconState) {
        CurrencyIconState.Loading -> TangemAvatar.UiState.Shimmer()
        is CurrencyIconState.CoinIcon -> iconState.url?.let { TangemAvatar.UiState.Asset(url = it) }
            ?: TangemAvatar.UiState.Asset(iconRes = iconState.fallbackResId)
        is CurrencyIconState.TokenIcon -> iconState.url?.let { TangemAvatar.UiState.Asset(url = it) }
            ?: TangemAvatar.UiState.Asset(iconRes = R.drawable.ic_empty_64)
        else -> TangemAvatar.UiState.Asset(iconRes = R.drawable.ic_empty_64)
    }
}