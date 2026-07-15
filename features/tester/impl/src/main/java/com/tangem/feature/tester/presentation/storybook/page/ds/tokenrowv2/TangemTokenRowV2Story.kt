@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.tokenrowv2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.NoAddress
import com.tangem.core.ui.ds2.tokenrow.Organize
import com.tangem.core.ui.ds2.tokenrow.Shimmer
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.ds2.tokenrow.Unreachable
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chart_bar_vertical_16
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowV2Story

private const val SAMPLE_ICON_URL = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/bitcoin.png"

@Composable
internal fun TangemTokenRowV2Story(state: TangemTokenRowV2Story, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VariantSelector(selected = state.variant, onSelect = state.onVariantChange)
            DirectionSelector(selected = state.direction, onSelect = state.onDirectionChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemTokenRowV2Story) {
    val icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = SAMPLE_ICON_URL))
    val priceChange = if (state.hasPriceChange) {
        TangemPriceChange.State(value = stringReference("2.08%"), direction = state.direction)
    } else {
        null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary)
            .padding(vertical = 16.dp),
    ) {
        when (state.variant) {
            TangemTokenRowV2Story.Variant.Default -> DefaultRowPreview(
                state = state,
                icon = icon,
                priceChange = priceChange,
            )
            TangemTokenRowV2Story.Variant.Organize -> TangemTokenRow.Organize(
                icon = icon,
                title = stringReference("Bitcoin"),
                ticker = stringReference("BTC"),
                fiatBalance = stringReference("$583.00"),
                isBalanceHidden = state.isBalanceHidden,
            )
            TangemTokenRowV2Story.Variant.Unreachable -> TangemTokenRow.Unreachable(
                icon = icon,
                title = stringReference("Bitcoin"),
                badge = stringReference("Unreachable"),
                quote = if (state.hasQuote) stringReference("$1.00") else null,
                priceChange = priceChange,
                onClick = {},
            )
            TangemTokenRowV2Story.Variant.NoAddress -> TangemTokenRow.NoAddress(
                icon = icon,
                title = stringReference("Bitcoin"),
                message = stringReference("No address"),
                quote = if (state.hasQuote) stringReference("$1.00") else null,
                priceChange = priceChange,
                onClick = {},
            )
            TangemTokenRowV2Story.Variant.Shimmer -> TangemTokenRow.Shimmer()
        }
    }
}

@Composable
private fun DefaultRowPreview(
    state: TangemTokenRowV2Story,
    icon: TangemTokenIcon.UiState,
    priceChange: TangemPriceChange.State?,
) {
    TangemTokenRow(
        icon = icon,
        title = stringReference("Bitcoin"),
        badge = if (state.hasBadge) {
            TangemTokenRow.Badge(
                text = stringReference("APY 5.47%"),
                variant = if (state.isBadgeFilled) TangemBadge.Variant.Solid else TangemBadge.Variant.Tinted,
                status = if (state.isBadgeFilled) TangemBadge.Status.Success else TangemBadge.Status.Neutral,
            )
        } else {
            null
        },
        hasPending = state.hasPending,
        quote = if (state.hasQuote) stringReference("$1.00") else null,
        priceChange = priceChange,
        fiatBalance = stringReference("$583.00"),
        cryptoBalance = if (state.hasCryptoBalance) stringReference("0,000015 BTC") else null,
        showContractWarning = state.hasContractWarning,
        showUpdateWarning = state.hasUpdateWarning,
        isBalanceHidden = state.isBalanceHidden,
        isQuoteFlickering = state.isQuoteFlickering,
        isBalanceFlickering = state.isBalanceFlickering,
        messageBubble = if (state.hasMessageBubble) {
            {
                TangemMessageBubble(
                    text = stringReference("Enable 5.47% APY on your balance"),
                    variant = TangemMessageBubble.Variant.Success,
                    icon = Icons.ic_chart_bar_vertical_16,
                    onClick = {},
                    onClose = {},
                    closeContentDescription = "Dismiss",
                )
            }
        } else {
            null
        },
        onClick = {},
    )
}

@Composable
private fun VariantSelector(
    selected: TangemTokenRowV2Story.Variant,
    onSelect: (TangemTokenRowV2Story.Variant) -> Unit,
) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemTokenRowV2Story.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun DirectionSelector(selected: TangemPriceChange.Direction, onSelect: (TangemPriceChange.Direction) -> Unit) {
    Section(label = "Price change") {
        ChipGrid(
            items = TangemPriceChange.Direction.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemTokenRowV2Story) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "badge", checked = state.hasBadge, onToggle = state.onBadgeToggle)
            ToggleRow(label = "badgeFilled", checked = state.isBadgeFilled, onToggle = state.onBadgeFilledToggle)
            ToggleRow(label = "pending", checked = state.hasPending, onToggle = state.onPendingToggle)
            ToggleRow(label = "quote", checked = state.hasQuote, onToggle = state.onQuoteToggle)
            ToggleRow(label = "priceChange", checked = state.hasPriceChange, onToggle = state.onPriceChangeToggle)
            ToggleRow(label = "cryptoBalance", checked = state.hasCryptoBalance, onToggle = state.onCryptoBalanceToggle)
            ToggleRow(
                label = "contractWarning",
                checked = state.hasContractWarning,
                onToggle = state.onContractWarningToggle,
            )
            ToggleRow(label = "updateWarning", checked = state.hasUpdateWarning, onToggle = state.onUpdateWarningToggle)
            ToggleRow(label = "messageBubble", checked = state.hasMessageBubble, onToggle = state.onMessageBubbleToggle)
            ToggleRow(label = "balanceHidden", checked = state.isBalanceHidden, onToggle = state.onBalanceHiddenToggle)
            ToggleRow(
                label = "quoteFlickering",
                checked = state.isQuoteFlickering,
                onToggle = state.onQuoteFlickeringToggle,
            )
            ToggleRow(
                label = "balanceFlickering",
                checked = state.isBalanceFlickering,
                onToggle = state.onBalanceFlickeringToggle,
            )
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        content()
    }
}

@Composable
private fun <T> ChipGrid(items: List<T>, label: (T) -> String, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.secondary,
                shape = shape,
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            Chip(
                label = label(item),
                selected = isSelected(item),
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors2.surface.level2)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
        )
    }
}