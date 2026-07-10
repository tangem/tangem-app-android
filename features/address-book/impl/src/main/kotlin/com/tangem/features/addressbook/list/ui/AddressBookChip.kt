package com.tangem.features.addressbook.list.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.addressbook.list.ui.state.AddressBookChipUM

@Composable
internal fun AddressBookChip(state: AddressBookChipUM, modifier: Modifier = Modifier) {
    val backgroundColor = if (state.isSelected) {
        TangemTheme.colors2.tabs.backgroundPrimary
    } else {
        TangemTheme.colors2.tabs.backgroundSecondary
    }
    val textColor = if (state.isSelected) {
        TangemTheme.colors2.tabs.textPrimary
    } else {
        TangemTheme.colors2.tabs.textSecondary
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape = CircleShape)
            .background(color = backgroundColor)
            .selectable(
                selected = state.isSelected,
                onClick = state.onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = state.text.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = textColor,
            maxLines = 1,
        )
        if (state.iconRes != null) {
            Icon(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
                painter = painterResource(id = state.iconRes),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.tertiaryConstant,
            )
        }
    }
}