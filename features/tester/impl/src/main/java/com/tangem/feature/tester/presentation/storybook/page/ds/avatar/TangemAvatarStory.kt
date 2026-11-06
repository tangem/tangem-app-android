@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.avatar

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.assets.il_account_airplane
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_box_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_cards_star_20
import com.tangem.core.ui.res.generated.icons.ic_grid_plus_20
import com.tangem.core.ui.res.generated.icons.ic_percent_20
import com.tangem.core.ui.res.generated.icons.ic_stack_20
import com.tangem.feature.tester.presentation.storybook.entity.TangemAvatarStory

private const val SAMPLE_INITIAL = 'A'
private const val SAMPLE_ASSET_URL = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/bitcoin.png"
private const val CHIPS_PER_ROW = 4
private const val ACCOUNT_VARIANTS_PER_ROW = 3

@Composable
internal fun TangemAvatarStory(state: TangemAvatarStory, modifier: Modifier = Modifier) {
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
            Selector(
                label = "Variant",
                items = TangemAvatarStory.Variant.entries,
                selected = state.variant,
                onSelect = state.onVariantChange,
            )
            VariantControls(state = state)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemAvatarStory) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary)
            .padding(16.dp),
    ) {
        TangemAvatar.Size.entries.forEach { size ->
            TangemAvatar(state = state.toUiState(), size = size)
        }
    }
}

@Composable
private fun VariantControls(state: TangemAvatarStory) {
    when (state.variant) {
        TangemAvatarStory.Variant.Account -> {
            Selector(
                label = "Account variant",
                items = TangemAvatarStory.AccountVariant.entries,
                selected = state.accountVariant,
                onSelect = state.onAccountVariantChange,
                perRow = ACCOUNT_VARIANTS_PER_ROW,
            )
            if (state.accountVariant.hasFillColor) FillColorSelector(state)
        }
        TangemAvatarStory.Variant.User -> FillColorSelector(state)
        TangemAvatarStory.Variant.Address -> Selector(
            label = "Address",
            items = TangemAvatarStory.AddressSample.entries,
            selected = state.address,
            onSelect = state.onAddressChange,
        )
        TangemAvatarStory.Variant.Icon -> Selector(
            label = "Appearance",
            items = TangemAvatar.Appearance.entries,
            selected = state.appearance,
            onSelect = state.onAppearanceChange,
        )
        TangemAvatarStory.Variant.Transaction -> {
            Selector(
                label = "Transaction type",
                items = TangemAvatarStory.TransactionType.entries,
                selected = state.transactionType,
                onSelect = state.onTransactionTypeChange,
            )
            Selector(
                label = "Status",
                items = TangemAvatar.Status.entries,
                selected = state.status,
                onSelect = state.onStatusChange,
            )
        }
        TangemAvatarStory.Variant.Asset,
        TangemAvatarStory.Variant.Shimmer,
        -> Unit
    }
}

@Composable
private fun FillColorSelector(state: TangemAvatarStory) {
    Selector(
        label = "Fill color",
        items = TangemAvatar.FillColor.entries,
        selected = state.fillColor,
        onSelect = state.onFillColorChange,
    )
}

private val TangemAvatarStory.AccountVariant.hasFillColor: Boolean
    get() = this == TangemAvatarStory.AccountVariant.Crypto || this == TangemAvatarStory.AccountVariant.Letter

private fun TangemAvatarStory.toUiState(): TangemAvatar.UiState = when (variant) {
    TangemAvatarStory.Variant.Account -> toAccountUiState()
    TangemAvatarStory.Variant.User -> TangemAvatar.UiState.User(
        initial = SAMPLE_INITIAL.takeIf { hasLetter },
        color = fillColor,
        shape = shape(),
    )
    TangemAvatarStory.Variant.Address -> TangemAvatar.UiState.Address(address = address.value, shape = shape())
    TangemAvatarStory.Variant.Icon -> TangemAvatar.UiState.Icon(
        imageVector = Icons.ic_grid_plus_20,
        appearance = appearance,
        shape = shape(),
    )
    TangemAvatarStory.Variant.Transaction -> TangemAvatar.UiState.Transaction(
        imageVector = transactionType.icon,
        status = status,
        shape = shape(),
    )
    TangemAvatarStory.Variant.Asset -> if (isRemoteAsset) {
        TangemAvatar.UiState.Asset(url = SAMPLE_ASSET_URL, shape = shape())
    } else {
        TangemAvatar.UiState.Asset(iconRes = R.drawable.img_collectibles_welcome_hero, shape = shape())
    }
    TangemAvatarStory.Variant.Shimmer -> TangemAvatar.UiState.Shimmer(shape = shape())
}

private fun TangemAvatarStory.toAccountUiState(): TangemAvatar.UiState.Account {
    val shape = shape()
    return when (accountVariant) {
        TangemAvatarStory.AccountVariant.Crypto -> TangemAvatar.UiState.Account(
            imageVector = Icons.il_account_airplane,
            color = fillColor,
            hasIndicator = hasAccountIndicator,
            shape = shape,
        )
        TangemAvatarStory.AccountVariant.Letter -> TangemAvatar.UiState.Account(
            letter = SAMPLE_INITIAL,
            color = fillColor,
            hasIndicator = hasAccountIndicator,
            shape = shape,
        )
        TangemAvatarStory.AccountVariant.Prediction -> accountKind(TangemAvatar.AccountKind.Prediction, shape)
        TangemAvatarStory.AccountVariant.Gacha -> accountKind(TangemAvatar.AccountKind.Gacha, shape)
        TangemAvatarStory.AccountVariant.Visa -> accountKind(TangemAvatar.AccountKind.Visa, shape)
        TangemAvatarStory.AccountVariant.Money -> accountKind(TangemAvatar.AccountKind.Money, shape)
    }
}

private fun TangemAvatarStory.accountKind(kind: TangemAvatar.AccountKind, shape: TangemAvatar.Shape) =
    TangemAvatar.UiState.Account(kind = kind, hasIndicator = hasAccountIndicator, shape = shape)

private val TangemAvatarStory.TransactionType.icon: ImageVector
    get() = when (this) {
        TangemAvatarStory.TransactionType.Send -> Icons.ic_arrow_up_20
        TangemAvatarStory.TransactionType.Receive -> Icons.ic_arrow_down_20
        TangemAvatarStory.TransactionType.Swap -> Icons.ic_arrow_swap_horizontal_20
        TangemAvatarStory.TransactionType.TopUp -> Icons.ic_card_20
        TangemAvatarStory.TransactionType.Staking -> Icons.ic_stack_20
        TangemAvatarStory.TransactionType.Delivery -> Icons.ic_box_20
        TangemAvatarStory.TransactionType.Pack -> Icons.ic_cards_star_20
        TangemAvatarStory.TransactionType.Fee -> Icons.ic_percent_20
    }

private val TangemAvatarStory.AddressSample.value: String
    get() = when (this) {
        TangemAvatarStory.AddressSample.First -> "0x1f9840a85d5af5bf1d1762f925bdaddc4201f984"
        TangemAvatarStory.AddressSample.Second -> "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045"
        TangemAvatarStory.AddressSample.Third -> "0x95222290DD7278Aa3Ddd389Cc1E1d165CC4BAfe5"
    }

private fun TangemAvatarStory.shape(): TangemAvatar.Shape =
    if (isRoundedSquare) TangemAvatar.Shape.RoundedSquare else TangemAvatar.Shape.Circle

@Composable
private fun <T : Enum<T>> Selector(
    label: String,
    items: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    perRow: Int = CHIPS_PER_ROW,
) {
    Section(label = label) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.chunked(perRow).forEach { row ->
                ChipGrid(
                    items = row,
                    label = { it.name },
                    isSelected = { it == selected },
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
private fun Toggles(state: TangemAvatarStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.variant) {
                TangemAvatarStory.Variant.Account -> ToggleRow(
                    label = "accountIndicator",
                    checked = state.hasAccountIndicator,
                    onToggle = state.onAccountIndicatorToggle,
                )
                TangemAvatarStory.Variant.User -> ToggleRow(
                    label = "letter",
                    checked = state.hasLetter,
                    onToggle = state.onLetterToggle,
                )
                TangemAvatarStory.Variant.Asset -> ToggleRow(
                    label = "remoteAsset",
                    checked = state.isRemoteAsset,
                    onToggle = state.onRemoteAssetToggle,
                )
                TangemAvatarStory.Variant.Address,
                TangemAvatarStory.Variant.Icon,
                TangemAvatarStory.Variant.Transaction,
                TangemAvatarStory.Variant.Shimmer,
                -> Unit
            }
            ToggleRow(
                label = "roundedSquare",
                checked = state.isRoundedSquare,
                onToggle = state.onRoundedSquareToggle,
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