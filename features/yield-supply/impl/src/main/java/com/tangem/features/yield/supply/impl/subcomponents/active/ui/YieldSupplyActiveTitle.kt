package com.tangem.features.yield.supply.impl.subcomponents.active.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.R

@Composable
internal fun YieldSupplyActiveTitle(onCloseClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Text(
                text = stringResourceSafe(R.string.yield_module_earn_sheet_title),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TangemTheme.colors.icon.accent, CircleShape),
                )
                Text(
                    text = stringResourceSafe(R.string.yield_module_status_active),
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier,
                )
            }
        }
        TangemIconButton(
            iconRes = R.drawable.ic_close_24,
            onClick = onCloseClick,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterEnd),
        )
    }
}