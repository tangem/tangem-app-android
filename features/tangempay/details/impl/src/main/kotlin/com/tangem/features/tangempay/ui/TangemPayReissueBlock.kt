package com.tangem.features.tangempay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_clock_20

@Composable
internal fun TangemPayReissueBlock(title: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = TangemTheme.colors3.bg.opaque.primary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.ic_clock_20,
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title.resolveReference(),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
            textAlign = TextAlign.Center,
        )
    }
}