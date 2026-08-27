package com.tangem.features.introduction.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun WalletOptionCard(option: WalletOptionUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.opaque.primary),
        onClick = option.onClick,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { OptionIcon(iconRes = option.iconRes) },
        titleSlot = { TangemRowText(text = option.title, role = TangemRowTextRole.Title) },
        subtitleSlot = {
            TangemRowText(
                text = option.description,
                role = TangemRowTextRole.Subtitle,
                maxLines = 2,
            )
        },
        endSlot = {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = CoreUiR.drawable.ic_chevron_right_24),
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        },
    )
}

@Composable
private fun OptionIcon(iconRes: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = TangemTheme.colors3.bg.opaque.secondary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
    }
}