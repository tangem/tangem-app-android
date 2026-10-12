package com.tangem.features.introduction.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM

@Composable
internal fun WalletOptionCard(option: WalletOptionUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (option.isEnabled) 1f else DISABLED_CONTENT_ALPHA),
        onClick = if (option.isEnabled && !option.isLoading) option.onClick else null,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { OptionIcon(icon = option.icon) },
        titleSlot = { TangemRowText(text = option.title, role = TangemRowTextRole.Title) },
        subtitleSlot = {
            TangemRowText(
                text = option.description,
                role = TangemRowTextRole.Subtitle,
                maxLines = 2,
            )
        },
        // The design carries no trailing element; the loader is the exception, since a row that is working
        // has to say so.
        endSlot = if (option.isLoading) {
            { TangemLoader(color = TangemTheme.colors3.icon.secondary) }
        } else {
            null
        },
    )
}

private const val DISABLED_CONTENT_ALPHA = 0.4f

@Composable
private fun OptionIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = TangemTheme.colors3.bg.opaque.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.primary,
        )
    }
}