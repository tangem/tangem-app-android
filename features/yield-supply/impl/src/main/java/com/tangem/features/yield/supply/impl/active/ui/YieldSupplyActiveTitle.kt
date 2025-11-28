package com.tangem.features.yield.supply.impl.active.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.yield.supply.impl.R

@Composable
internal fun YieldSupplyActiveTitle(onCloseClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_close_24),
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp)
                .clickable(
                    indication = ripple(false),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onCloseClick,
                ),
        )
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
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
                Text(
                    text = stringResourceSafe(R.string.yield_module_status_active),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(TangemTheme.colors.icon.accent, CircleShape),
                )
            }
        }
    }
}