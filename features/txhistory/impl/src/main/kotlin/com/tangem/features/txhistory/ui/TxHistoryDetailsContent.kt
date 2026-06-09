package com.tangem.features.txhistory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM

@Composable
internal fun TxHistoryDetailsContent(state: TxHistoryDetailsUM, modifier: Modifier = Modifier) {
    // Placeholder:card showing only the operation title, to verify tap -> sheet navigation
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TangemTheme.colors2.surface.level2)
            .heightIn(min = 240.dp)
            .padding(TangemTheme.dimens2.x6),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = state.title,
            color = TangemTheme.colors2.text.neutral.primary,
            style = TangemTheme.typography2.headingSemibold28,
            textAlign = TextAlign.Center,
        )
    }
}