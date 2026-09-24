package com.tangem.features.polymarket.impl.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape

@Composable
internal fun PolymarketPositionsEmptyState(modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier.fillMaxWidth().heightIn(min = 136.dp),
        color = TangemTheme.colors3.bg.secondary,
        shape = PolymarketCardShape,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(TangemTheme.colors3.bg.tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.ic_info_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.secondary,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResourceSafe(R.string.prediction_main_empty_positions_title),
                color = TangemTheme.colors3.text.primary,
                style = TangemTheme.typography3.caption.medium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResourceSafe(R.string.prediction_main_empty_positions_subtitle),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.caption.medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}