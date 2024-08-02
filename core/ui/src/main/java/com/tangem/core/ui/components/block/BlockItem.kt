package com.tangem.core.ui.components.block

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
fun BlockItem(model: BlockUM, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        onClick = model.onClick,
    ) {
        Row(
            modifier = Modifier.padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12, Alignment.Start),
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = model.iconRes),
                tint = when (model.accentType) {
                    BlockUM.AccentType.NONE -> TangemTheme.colors.icon.secondary
                    BlockUM.AccentType.ACCENT -> TangemTheme.colors.text.accent
                    BlockUM.AccentType.WARNING -> TangemTheme.colors.text.warning
                },
                contentDescription = null,
            )

            Text(
                text = model.text.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = when (model.accentType) {
                    BlockUM.AccentType.NONE -> TangemTheme.colors.text.primary1
                    BlockUM.AccentType.ACCENT -> TangemTheme.colors.text.accent
                    BlockUM.AccentType.WARNING -> TangemTheme.colors.text.warning
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
