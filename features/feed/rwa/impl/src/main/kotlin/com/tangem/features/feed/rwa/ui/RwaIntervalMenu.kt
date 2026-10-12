package com.tangem.features.feed.rwa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.ui.state.RwaIntervalUM

/**
 * Trend-interval options dropped under the interval chip, mirroring the Crypto tab's menu.
 *
 * [TangemContextMenu] is a slot API — it supplies neither a row nor a selection affordance — so both are
 * drawn here. Picking an option dismisses the menu itself; the model only ever sees the new interval.
 */
@Composable
internal fun RwaIntervalMenu(
    um: RwaIntervalUM,
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemContextMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        RwaInterval.entries.fastForEachIndexed { index, interval ->
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(200.dp)
                        .clickableSingle(
                            onClick = {
                                um.onOptionClick(interval)
                                onDismiss()
                            },
                        )
                        .padding(vertical = 10.dp, horizontal = 16.dp),
                ) {
                    Text(
                        text = interval.title.resolveReference(),
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.primary,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (um.selected == interval) {
                            SelectedCheck()
                        }
                    }
                }
                if (index < RwaInterval.entries.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = TangemTheme.colors3.border.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedCheck(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .size(20.dp)
            .background(color = TangemTheme.colors3.icon.primary, shape = CircleShape),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_check_default_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.inverse,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(2.dp)
                .size(16.dp),
        )
    }
}