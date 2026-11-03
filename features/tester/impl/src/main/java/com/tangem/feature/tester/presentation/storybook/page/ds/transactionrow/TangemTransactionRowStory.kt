@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.transactionrow

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
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.transactionrow.EntityStackSubtitle
import com.tangem.core.ui.ds2.transactionrow.EntitySubtitle
import com.tangem.core.ui.ds2.transactionrow.Shimmer
import com.tangem.core.ui.ds2.transactionrow.TangemTransactionRow
import com.tangem.core.ui.ds2.transactionrow.TextSubtitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemTransactionRowStory
import kotlinx.collections.immutable.persistentListOf

private const val SAMPLE_ADDRESS = "0x1D1479C185d32EB90533a08b36B3CFa5F84A0E6B"

@Composable
internal fun TangemTransactionRowStory(state: TangemTransactionRowStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(label = "Variant") {
                ChipGrid(
                    items = TangemTransactionRowStory.Variant.entries,
                    label = { it.name },
                    isSelected = { it == state.variant },
                    onSelect = state.onVariantChange,
                )
            }
            Section(label = "Status") {
                ChipGrid(
                    items = TangemTransactionRow.Status.entries,
                    label = { it.name },
                    isSelected = { it == state.status },
                    onSelect = state.onStatusChange,
                )
            }
            Section(label = "Subtitle") {
                ChipGrid(
                    items = TangemTransactionRowStory.Subtitle.entries,
                    label = { it.name },
                    isSelected = { it == state.subtitle },
                    onSelect = state.onSubtitleChange,
                )
            }
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemTransactionRowStory) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary),
    ) {
        when (state.variant) {
            TangemTransactionRowStory.Variant.Content -> ContentRowPreview(state = state)
            TangemTransactionRowStory.Variant.Shimmer -> TangemTransactionRow.Shimmer()
        }
    }
}

@Composable
private fun ContentRowPreview(state: TangemTransactionRowStory) {
    val isFailed = state.status == TangemTransactionRow.Status.Failed
    TangemTransactionRow(
        icon = Icons.ic_arrow_up_20,
        title = stringReference("Send"),
        status = state.status,
        subtitle = state.subtitleSlot(),
        amount = if (state.hasAmount) stringReference(if (isFailed) "$12,000.00" else "-$12,000.00") else null,
        caption = if (state.hasCaption) stringReference("USDT") else null,
        badge = if (state.hasBadge) stringReference("Label") else null,
        statusMessage = if (state.hasStatusMessage) stringReference("KYC verification required by provider") else null,
        isBalanceHidden = state.isBalanceHidden,
        onClick = {},
    )
}

private fun TangemTransactionRowStory.subtitleSlot(): (@Composable () -> Unit)? {
    val description = if (hasDescription) stringReference("Description") else null
    return when (subtitle) {
        TangemTransactionRowStory.Subtitle.None -> null
        TangemTransactionRowStory.Subtitle.Entity -> {
            {
                TangemTransactionRow.EntitySubtitle(
                    template = resourceReference(
                        id = R.string.transaction_history_from_inline_address,
                        formatArgs = wrappedList("Main account"),
                    ),
                    avatar = TangemAvatar.UiState.User(initial = 'M', color = TangemAvatar.FillColor.AzureBlue),
                    description = description,
                )
            }
        }
        TangemTransactionRowStory.Subtitle.Address -> {
            {
                TangemTransactionRow.EntitySubtitle(
                    template = resourceReference(
                        id = R.string.transaction_history_to_inline_address,
                        formatArgs = wrappedList(SAMPLE_ADDRESS),
                    ),
                    avatar = TangemAvatar.UiState.Address(address = SAMPLE_ADDRESS),
                    address = SAMPLE_ADDRESS,
                    description = description,
                )
            }
        }
        TangemTransactionRowStory.Subtitle.Stack -> {
            {
                TangemTransactionRow.EntityStackSubtitle(
                    template = stringReference("from: %image% Main to: %image% Savings"),
                    avatars = persistentListOf(
                        TangemAvatar.UiState.User(initial = 'M', color = TangemAvatar.FillColor.AzureBlue),
                        TangemAvatar.UiState.Address(address = SAMPLE_ADDRESS),
                    ),
                )
            }
        }
        TangemTransactionRowStory.Subtitle.Text -> {
            { TangemTransactionRow.TextSubtitle(label = stringReference("Label"), description = description) }
        }
    }
}

@Composable
private fun Toggles(state: TangemTransactionRowStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "description", checked = state.hasDescription, onToggle = state.onDescriptionToggle)
            ToggleRow(label = "amount", checked = state.hasAmount, onToggle = state.onAmountToggle)
            ToggleRow(label = "caption", checked = state.hasCaption, onToggle = state.onCaptionToggle)
            ToggleRow(label = "badge", checked = state.hasBadge, onToggle = state.onBadgeToggle)
            ToggleRow(
                label = "statusMessage",
                checked = state.hasStatusMessage,
                onToggle = state.onStatusMessageToggle,
            )
            ToggleRow(label = "balanceHidden", checked = state.isBalanceHidden, onToggle = state.onBalanceHiddenToggle)
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
            color = TangemTheme.colors3.text.primary,
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
            .background(TangemTheme.colors3.bg.primary)
            .border(
                width = 1.dp,
                color = TangemTheme.colors3.border.tertiary,
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
                if (selected) TangemTheme.colors3.bg.secondary else TangemTheme.colors3.bg.primary,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors3.text.primary else TangemTheme.colors3.text.secondary,
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
            .background(TangemTheme.colors3.bg.primary)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors3.text.accent.blue else TangemTheme.colors3.text.secondary,
        )
    }
}