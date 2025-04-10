package com.tangem.common.ui.bottomsheet.permission

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.PopupProperties
import com.tangem.common.ui.R
import com.tangem.common.ui.bottomsheet.permission.state.*
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.containers.FooterContainer
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList

@Composable
fun GiveTxPermissionBottomSheet(config: TangemBottomSheetConfig) {
    var isPermissionAlertShow by remember { mutableStateOf(false) }

    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.secondary,
        titleText = resourceReference(R.string.give_permission_title),
        titleAction = TopAppBarButtonUM(
            iconRes = R.drawable.ic_information_24,
            onIconClicked = { isPermissionAlertShow = true },
        ),
        content = { content: GiveTxPermissionBottomSheetConfig ->
            GiveTxPermissionBottomSheetContent(content = content)

            if (isPermissionAlertShow) {
                BasicDialog(
                    message = content.data.dialogText.resolveReference(),
                    title = stringResourceSafe(id = R.string.common_approve),
                    confirmButton = DialogButtonUM { isPermissionAlertShow = false },
                    onDismissDialog = {},
                )
            }
        },
    )
}

@Composable
private fun GiveTxPermissionBottomSheetContent(content: GiveTxPermissionBottomSheetConfig) {
    val data = content.data
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.secondary)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = content.data.subtitle.resolveReference(),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing24),
        )

        SpacerH16()

        ApprovalBottomSheetInfo(data)

        SpacerH(height = TangemTheme.dimens.spacing20)

        PrimaryButtonIconEnd(
            text = stringResourceSafe(id = R.string.common_approve),
            iconResId = R.drawable.ic_tangem_24,
            showProgress = data.approveButton.loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing16),
            onClick = data.approveButton.onClick,
            enabled = data.approveButton.enabled,
        )

        SpacerH12()

        SecondaryButton(
            text = stringResourceSafe(id = R.string.common_cancel),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens.spacing16),
            onClick = content.onCancel,
            enabled = data.cancelButton.enabled,
        )

        SpacerH16()
    }
}

@Composable
private fun ApprovalBottomSheetInfo(data: GiveTxPermissionState.ReadyForRequest) {
    FooterContainer(
        footer = resourceReference(R.string.give_permission_policy_type_footer),
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        AmountItem(
            currency = data.currency,
            approveType = data.approveType,
            onChangeApproveType = data.onChangeApproveType,
            approveItems = data.approveItems,
        )
    }
    SpacerH16()
    FooterContainer(
        footer = data.footerText,
        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing16),
    ) {
        FeeItem(fee = data.fee)
    }
}

@Composable
private fun FeeItem(fee: TextReference) {
    InputRowDefault(
        title = resourceReference(R.string.common_network_fee_title),
        text = fee,
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action),
    )
}

@Composable
private fun AmountItem(
    currency: String,
    approveType: ApproveType,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: ((ApproveType) -> Unit)?,
) {
    var isExpandSelector by remember { mutableStateOf(false) }
    var amountSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(
                enabled = onChangeApproveType != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = { isExpandSelector = true },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { amountSize = it }
                .padding(
                    top = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing16,
                    start = TangemTheme.dimens.spacing12,
                    end = TangemTheme.dimens.spacing12,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResourceSafe(id = R.string.give_permission_rows_amount, currency),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
                maxLines = 1,
            )
            SpacerWMax()
            Text(
                text = approveType.text.resolveReference(),
                color = TangemTheme.colors.text.tertiary,
                style = TangemTheme.typography.body1,
                maxLines = 1,
            )
            if (onChangeApproveType != null) {
                Icon(
                    painter = rememberVectorPainter(ImageVector.vectorResource(id = R.drawable.ic_chevron_24)),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.informative,
                    modifier = Modifier.padding(start = TangemTheme.dimens.spacing2),
                )
            }
        }
        if (onChangeApproveType != null) {
            DropdownSelector(
                isExpanded = isExpandSelector,
                onDismiss = { isExpandSelector = false },
                onItemClick = { approveType ->
                    onChangeApproveType.let {
                        isExpandSelector = false
                        onChangeApproveType.invoke(approveType)
                    }
                },
                items = approveItems,
                selectedType = approveType,
                amountSize = amountSize,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun DropdownSelector(
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

    // Workaround to set color and shape of dropdown menu
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
                            Text(
                                text = when (item) {
                                    ApproveType.LIMITED -> stringResourceSafe(
                                        id = R.string.give_permission_current_transaction,
                                    )
                                    ApproveType.UNLIMITED -> stringResourceSafe(id = R.string.give_permission_unlimited)
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

// region preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, locale = "ru")
private fun Preview_GiveTxPermissionBottomSheet() {
    TangemThemePreview {
        GiveTxPermissionBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = previewData,
            ),
        )
    }
}

private val previewData = GiveTxPermissionBottomSheetConfig(
    data = GiveTxPermissionState.ReadyForRequest(
        currency = "DAI",
        amount = "1",
        walletAddress = "",
        spenderAddress = "",
        fee = TextReference.Str("0.1233 BTC (2,14$)"),
        approveType = ApproveType.LIMITED,
        approveButton = ApprovePermissionButton(true) {},
        cancelButton = CancelPermissionButton(true),
        onChangeApproveType = { ApproveType.LIMITED },
        subtitle = resourceReference(R.string.give_permission_staking_subtitle, wrappedList("1")),
        dialogText = resourceReference(R.string.give_permission_staking_footer),
        footerText = resourceReference(R.string.swap_give_permission_fee_footer),
    ),
    onCancel = {},
)