package com.tangem.features.feed.ui.market.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.model.market.list.state.SortByMenuUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM

@Composable
internal fun SortByMenu(
    sortMenuUM: SortByMenuUM,
    showDropdownMenu: Boolean,
    onDropdownDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemContextMenu(
        expanded = showDropdownMenu,
        onDismissRequest = onDropdownDismiss,
        offset = DpOffset.Zero,
        modifier = modifier,
    ) {
        SortByTypeUM.entries.fastForEachIndexed { index, sortType ->
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(238.dp)
                        .clickableSingle(
                            onClick = {
                                sortMenuUM.onOptionClicked(sortType)
                                onDropdownDismiss()
                            },
                        )
                        .padding(
                            vertical = TangemTheme.dimens2.x5,
                            horizontal = TangemTheme.dimens2.x4,
                        ),
                ) {
                    Text(
                        text = sortType.text.resolveReference(),
                        style = TangemTheme.typography2.headingSemibold17,
                        color = TangemTheme.colors2.text.neutral.primary,
                        maxLines = 1,
                    )
                    if (sortMenuUM.selectedOption == sortType) {
                        Box(
                            modifier = Modifier
                                .padding(TangemTheme.dimens2.x0_5)
                                .size(TangemTheme.dimens2.x5)
                                .background(
                                    color = TangemTheme.colors2.graphic.neutral.primary,
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                painter = rememberVectorPainter(
                                    ImageVector.vectorResource(R.drawable.ic_check_default_24),
                                ),
                                contentDescription = null,
                                tint = TangemTheme.colors2.graphic.neutral.primaryInverted,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(TangemTheme.dimens2.x0_5)
                                    .size(TangemTheme.dimens2.x4),
                            )
                        }
                    }
                }
                if (index < SortByTypeUM.entries.size - 1) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = TangemTheme.colors2.border.neutral.quaternary,
                    )
                }
            }
        }
    }
}