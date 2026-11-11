package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_16

@Composable
internal fun FeedPlusIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size = 24.dp)
            .clip(shape = CircleShape)
            .background(color = TangemTheme.colors3.bg.accent.blue),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.ic_sign_plus_16,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticDark,
            modifier = Modifier.size(size = 20.dp),
        )
    }
}