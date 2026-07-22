package com.tangem.features.foryou.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletTabUM

/**
 * Horizontal wallet pill-tab strip for the For You screen.
 *
 * Faithful replica of the `WalletTabItem` / `walletListItem` reference in
 * `features/common-features/impl/.../choosetoken/ui/ChooseTokenScreen.kt` (which is `private`),
 * adapted to a standalone composable (For You renders inside a plain scrollable Column, not a LazyListScope).
 */
@Composable
internal fun WalletTabsBlock(walletList: WalletListUM, modifier: Modifier = Modifier) {
    if (walletList.items.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(walletList.items) { um ->
            WalletTabItem(um)
        }
    }
}

@Composable
private fun WalletTabItem(state: WalletTabUM, modifier: Modifier = Modifier) {
    val isSelected = state.isSelected
    val backgroundColor = if (isSelected) TangemTheme.colors.button.primary else TangemTheme.colors.button.secondary
    val buttonTextColor = if (isSelected) TangemTheme.colors.text.primary2 else TangemTheme.colors.text.primary1
    val countTextColor = if (isSelected) TangemTheme.colors.text.primary2 else TangemTheme.colors.text.secondary
    val countBackground = if (isSelected) {
        TangemTheme.colors.button.secondary.copy(alpha = 0.2f)
    } else {
        TangemTheme.colors.button.primary.copy(alpha = 0.1f)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(backgroundColor)
            .clickable(onClick = state.onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.text.resolveReference(),
            color = buttonTextColor,
            style = TangemTheme.typography2.bodySemibold16,
        )

        val count = state.count
        if (count != null) {
            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(countBackground, shape = CircleShape)
                    .defaultMinSize(minWidth = 20.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.resolveReference(),
                    color = countTextColor,
                    style = TangemTheme.typography.caption1,
                )
            }
        }
    }
}