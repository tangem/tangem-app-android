package com.tangem.features.approval.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.Text as M3Text
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Reusable row that shows "Amount for {currency}" on the left and the currently selected
 * [ApproveType] on the right, with a dropdown to switch between the available types.
 *
 * Used by both [GiveApprovalContent] (full approval flow) and [SelectApprovalTypeContent]
 * (selection-only flow).
 */
@Composable
internal fun ApprovalTypeSelectorRow(
    currency: String,
    approveType: ApproveType,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: (ApproveType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpandSelector by remember { mutableStateOf(false) }
    var amountSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { isExpandSelector = true },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { amountSize = it }
                .padding(vertical = 12.dp, horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            M3Text(
                text = stringResourceSafe(id = R.string.give_permission_rows_amount, currency),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
                maxLines = 1,
            )
            SpacerWMax()
            M3Text(
                text = approveType.text.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body1,
                maxLines = 1,
            )
            Icon(
                painter = rememberVectorPainter(ImageVector.vectorResource(id = R.drawable.ic_chevron_24)),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing2),
            )
        }
        ApprovalTypeDropdown(
            isExpanded = isExpandSelector,
            onDismiss = { isExpandSelector = false },
            onItemClick = { type ->
                isExpandSelector = false
                onChangeApproveType(type)
            },
            items = approveItems,
            selectedType = approveType,
            amountSize = amountSize,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ApprovalTypeDropdown(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onItemClick: (ApproveType) -> Unit,
    items: ImmutableList<ApproveType>,
    selectedType: ApproveType,
    amountSize: IntSize,
) {
    var dropDownWidth by remember { mutableStateOf(IntSize.Zero) }
    val offsetY = amountSize.height.times(-1)
    val offsetX = amountSize.width - dropDownWidth.width

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(surface = TangemTheme.colors.background.action),
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(TangemTheme.dimens.radius16)),
    ) {
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = onDismiss,
            properties = PopupProperties(clippingEnabled = false),
            offset = with(LocalDensity.current) {
                DpOffset(x = offsetX.toDp(), y = offsetY.toDp())
            },
            modifier = Modifier
                .wrapContentSize()
                .background(TangemTheme.colors.background.action)
                .onSizeChanged { dropDownWidth = it },
        ) {
            items.forEach { item ->
                val color = if (item == selectedType) TangemTheme.colors.icon.accent else Color.Transparent

                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Row {
                            M3Text(
                                text = when (item) {
                                    ApproveType.LIMITED -> stringResourceSafe(
                                        id = R.string.give_permission_current_transaction,
                                    )
                                    ApproveType.UNLIMITED -> stringResourceSafe(
                                        id = R.string.give_permission_unlimited,
                                    )
                                },
                                color = TangemTheme.colors.text.primary1,
                                style = TangemTheme.typography.body1,
                                maxLines = 1,
                            )
                            SpacerWMax()
                            Icon(
                                painter = rememberVectorPainter(
                                    image = ImageVector.vectorResource(id = R.drawable.ic_check_24),
                                ),
                                tint = color,
                                contentDescription = null,
                                modifier = Modifier.padding(start = TangemTheme.dimens.size20),
                            )
                        }
                    },
                    onClick = {
                        onItemClick.invoke(item)
                    },
                )
            }
        }
    }
}