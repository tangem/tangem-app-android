@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.accountrow

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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.accountrow.BalanceError
import com.tangem.core.ui.ds2.accountrow.EmptyBody
import com.tangem.core.ui.ds2.accountrow.Section
import com.tangem.core.ui.ds2.accountrow.Shimmer
import com.tangem.core.ui.ds2.accountrow.TangemAccountRow
import com.tangem.core.ui.ds2.accountrow.Unreachable
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.expandable.expandableRowReveal
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRow
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemAccountRowStory

private const val SAMPLE_ICON_URL = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/bitcoin.png"
private const val SHORT_TITLE = "Main account"
private const val LONG_TITLE = "Very long account title that is not going to fit on one line"
private const val MAX_TOKEN_COUNT = 4

@Composable
internal fun TangemAccountRowStory(state: TangemAccountRowStory, modifier: Modifier = Modifier) {
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
            TokenCountSelector(selected = state.tokenCount, onSelect = state.onTokenCountChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemAccountRowStory) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary)
            .padding(16.dp),
    ) {
        AccountRowPreview(state = state)
    }
}

@Suppress("LongMethod")
@Composable
private fun AccountRowPreview(state: TangemAccountRowStory) {
    val avatar = TangemAvatar.UiState.Account(
        iconRes = R.drawable.ic_airplane_mode_24,
        color = TangemAvatar.FillColor.DarkBlue,
    )
    val title = stringReference(if (state.longTitle) LONG_TITLE else SHORT_TITLE)
    val tokensAmount = stringReference("${state.tokenCount} tokens")
    val membersAmount = if (state.hasMembersAmount) stringReference("2 members") else null
    val isEmpty = state.variant == TangemAccountRowStory.Variant.Empty
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        when (state.variant) {
            TangemAccountRowStory.Variant.Default,
            TangemAccountRowStory.Variant.Empty,
            -> TangemAccountRow(
                title = title,
                tokensAmount = if (isEmpty) stringReference("0 tokens") else tokensAmount,
                membersAmount = membersAmount,
                balance = if (isEmpty) stringReference("$0.00") else stringReference("$12,000.00"),
                priceChange = TangemPriceChange.State(
                    value = stringReference(if (isEmpty) "0%" else "2.08%"),
                    direction = if (isEmpty) {
                        TangemPriceChange.Direction.Neutral
                    } else {
                        TangemPriceChange.Direction.Up
                    },
                ),
                showUpdateWarning = state.hasUpdateWarning,
                showCompactBalance = !isEmpty,
                isBalanceHidden = state.isBalanceHidden,
                isBalanceFlickering = state.isBalanceFlickering,
                isExpandable = true,
                isExpanded = state.isExpanded,
                shape = RectangleShape,
                collapseContentDescription = "Collapse",
                onClick = state.onExpandedToggle,
                avatar = avatar,
            )
            TangemAccountRowStory.Variant.Unreachable -> TangemAccountRow.Unreachable(
                title = title,
                badge = stringReference("Unreachable"),
                tokensAmount = tokensAmount,
                membersAmount = membersAmount,
                isExpandable = true,
                isExpanded = state.isExpanded,
                shape = RectangleShape,
                collapseContentDescription = "Collapse",
                onClick = state.onExpandedToggle,
                avatar = avatar,
            )
            TangemAccountRowStory.Variant.BalanceError -> TangemAccountRow.BalanceError(
                title = title,
                tokensAmount = tokensAmount,
                membersAmount = membersAmount,
                isExpandable = true,
                isExpanded = state.isExpanded,
                shape = RectangleShape,
                collapseContentDescription = "Collapse",
                onClick = state.onExpandedToggle,
                avatar = avatar,
            )
            TangemAccountRowStory.Variant.Shimmer -> TangemAccountRow.Shimmer()
        }

        Column(modifier = Modifier.expandableRowReveal(state.isExpanded)) {
            BodyItems(state = state, isEmpty = isEmpty)
        }
    }
}

/**
 * The rows an open account reveals. On the real screen these are sibling lazy items; the storybook
 * stands in for that host with a plain column so the shapes and background still read as one card.
 */
@Composable
private fun BodyItems(state: TangemAccountRowStory, isEmpty: Boolean) {
    val items: List<@Composable () -> Unit> = buildList {
        if (isEmpty) {
            add {
                TangemAccountRow.EmptyBody(
                    message = stringReference(
                        "Add tokens to begin tracking your crypto assets and transactions",
                    ),
                    addTokensLabel = stringReference("Add tokens"),
                    onAddTokensClick = {},
                    shape = RectangleShape,
                )
            }
        } else {
            if (state.hasSection) {
                add { TangemAccountRow.Section(title = stringReference("Section Title")) }
            }
            repeat(state.tokenCount) {
                add { TokenRow(state = state) }
            }
        }
    }
    items.forEach { item -> item() }
}

@Composable
private fun TokenRow(state: TangemAccountRowStory) {
    TangemTokenRow(
        icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = SAMPLE_ICON_URL)),
        title = stringReference("Bitcoin"),
        quote = stringReference("$1.00"),
        priceChange = TangemPriceChange.State(
            value = stringReference("2.08%"),
            direction = TangemPriceChange.Direction.Up,
        ),
        fiatBalance = stringReference("$583.00"),
        cryptoBalance = stringReference("0,000015 BTC"),
        isBalanceHidden = state.isBalanceHidden,
        isBalanceFlickering = state.isBalanceFlickering,
        onClick = {},
    )
}

@Composable
private fun VariantSelector(
    selected: TangemAccountRowStory.Variant,
    onSelect: (TangemAccountRowStory.Variant) -> Unit,
) {
    Section(label = "Variant") {
        ChipGrid(
            items = TangemAccountRowStory.Variant.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun TokenCountSelector(selected: Int, onSelect: (Int) -> Unit) {
    Section(label = "Tokens in the expanded body") {
        ChipGrid(
            items = (1..MAX_TOKEN_COUNT).toList(),
            label = { it.toString() },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemAccountRowStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "expanded", checked = state.isExpanded, onToggle = state.onExpandedToggle)
            ToggleRow(label = "membersAmount", checked = state.hasMembersAmount, onToggle = state.onMembersAmountToggle)
            ToggleRow(label = "updateWarning", checked = state.hasUpdateWarning, onToggle = state.onUpdateWarningToggle)
            ToggleRow(label = "section", checked = state.hasSection, onToggle = state.onSectionToggle)
            ToggleRow(label = "longTitle", checked = state.longTitle, onToggle = state.onLongTitleToggle)
            ToggleRow(label = "balanceHidden", checked = state.isBalanceHidden, onToggle = state.onBalanceHiddenToggle)
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