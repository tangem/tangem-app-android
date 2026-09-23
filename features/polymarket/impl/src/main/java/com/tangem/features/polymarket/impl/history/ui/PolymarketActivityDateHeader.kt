package com.tangem.features.polymarket.impl.history.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun PolymarketActivityDateHeader(title: TextReference, modifier: Modifier = Modifier) {
    Text(
        text = title.resolveReference(),
        color = TangemTheme.colors3.text.primary,
        style = TangemTheme.typography3.subheading.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
            .semantics { heading() },
    )
}