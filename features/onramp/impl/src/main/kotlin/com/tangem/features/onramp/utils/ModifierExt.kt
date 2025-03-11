package com.tangem.features.onramp.utils

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun Modifier.selectedBorder() = border(
    width = 2.5.dp,
    color = TangemTheme.colors.text.accent.copy(alpha = 0.1f),
    shape = RoundedCornerShape(18.dp),
)
    .padding(2.5.dp)
    .border(
        width = 1.dp,
        color = TangemTheme.colors.text.accent,
        shape = RoundedCornerShape(16.dp),
    )
    .clip(RoundedCornerShape(16.dp))