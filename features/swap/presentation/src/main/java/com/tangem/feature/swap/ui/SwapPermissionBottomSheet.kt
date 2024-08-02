package com.tangem.feature.swap.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.swap.models.ApprovePermissionButton
import com.tangem.feature.swap.models.ApproveType
import com.tangem.feature.swap.models.CancelPermissionButton
import com.tangem.feature.swap.models.SwapPermissionState
import com.tangem.feature.swap.models.states.GivePermissionBottomSheetConfig
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.ImmutableList

@Composable
fun SwapPermissionBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { content: GivePermissionBottomSheetConfig ->
        SwapPermissionBottomSheetContent(content = content)
    }
}

@Composable
private fun SwapPermissionBottomSheetContent(content: GivePermissionBottomSheetConfig) {
    var isPermissionAlertShow by remember { mutableStateOf(false) }
    val data = content.data
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(id = R.string.swapping_permission_header),
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
            )
            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { isPermissionAlertShow = true },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_question_24),
                    contentDescription = null,
                )
            }
        }

        SpacerH10()

        Text(
            text = stringResource(
                id = R.string.swapping_permission_subheader,
                data.providerName,
                data.currency,
            ),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing8),
        )

        SpacerH16()

        ApprovalBottomSheetInfo(data)

        SpacerH28()

        PrimaryButtonIconEnd(
            text = stringResource(id = R.string.swapping_permission_buttons_approve),
            iconResId = R.drawable.ic_tangem_24,
            modifier = Modifier.fillMaxWidth(),
            onClick = data.approveButton.onClick,
        )

        SpacerH12()

        SecondaryButton(
            text = stringResource(id = R.string.common_cancel),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                content.onCancel()
            },
        )

        SpacerH16()

        // region dialog
        if (isPermissionAlertShow) {
            BasicDialog(
                message = stringResource(id = R.string.swapping_approve_information_text),
                title = stringResource(id = R.string.swapping_approve_information_title),
                confirmButton = DialogButton { isPermissionAlertShow = false },
                onDismissDialog = {},
            )
        }
    }
}

@Composable
private fun ApprovalBottomSheetInfo(data: SwapPermissionState.ReadyForRequest) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountItem(
            currency = data.currency,
            approveType = data.approveType,
            onChangeApproveType = data.onChangeApproveType,
            approveItems = data.approveItems,
        )
        SubtitleItem(
            subtitle = stringResource(id = R.string.swapping_permission_policy_type_footer),
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH24()
        DividerBottomSheet()
        FeeItem(fee = data.fee.resolveReference())
        SubtitleItem(
            subtitle = stringResource(id = R.string.swapping_permission_fee_footer),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DividerBottomSheet() {
    Divider(
        color = TangemTheme.colors.stroke.primary,
        thickness = TangemTheme.dimens.size0_5,
    )
}

@Composable
private fun InformationItem(subtitle: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = subtitle,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
            maxLines = 1,
        )

        MiddleEllipsisText(
            text = value,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun AmountItem(
    currency: String,
    approveType: ApproveType,
    approveItems: ImmutableList<ApproveType>,
    onChangeApproveType: (ApproveType) -> Unit,
) {
    var isExpandSelector by remember {
        mutableStateOf(false)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = TangemTheme.dimens.spacing16),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.swapping_permission_rows_amount, currency),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
            maxLines = 1,
        )
        Box {
            SelectorItem(
                getTitleForApproveType(approveType = approveType),
            ) {
                isExpandSelector = true
            }
            DropdownSelector(
                isExpanded = isExpandSelector,
                onDismiss = { isExpandSelector = false },
                onItemClick = { approveType ->
                    isExpandSelector = false
                    onChangeApproveType.invoke(approveType)
                },
                items = approveItems,
            )
        }
    }
}

@Composable
private fun SelectorItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = title,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.body1,
            maxLines = 1,
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_24),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
    }
}

@Composable
private fun DropdownSelector(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onItemClick: (ApproveType) -> Unit,
    items: ImmutableList<ApproveType>,
) {
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .wrapContentSize()
            .background(TangemTheme.colors.background.secondary),
    ) {
        items.forEach { item ->
            DropdownMenuItem(
                onClick = {
                    onItemClick.invoke(item)
                },
            ) {
                Text(
                    text = getTitleForApproveType(approveType = item),
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.body1,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun FeeItem(fee: String) {
    InformationItem(
        subtitle = stringResource(id = R.string.common_network_fee_title),
        value = fee,
    )
}

@Composable
private fun SubtitleItem(subtitle: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = subtitle,
        color = TangemTheme.colors.text.secondary,
        style = TangemTheme.typography.body2,
    )
}

@Composable
private fun getTitleForApproveType(approveType: ApproveType): String = when (approveType) {
    ApproveType.LIMITED -> stringResource(id = R.string.swapping_permission_current_transaction)
    ApproveType.UNLIMITED -> stringResource(id = R.string.swapping_permission_unlimited)
}

// region preview

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AgreementBottomSheet() {
    TangemThemePreview {
        SwapPermissionBottomSheetContent(content = previewData)
    }
}

private val previewData = GivePermissionBottomSheetConfig(
    data = SwapPermissionState.ReadyForRequest(
        providerName = "1inch",
        currency = "DAI",
        amount = "∞",
        walletAddress = "",
        spenderAddress = "",
        fee = TextReference.Str("2,14$"),
        approveType = ApproveType.UNLIMITED,
        approveButton = ApprovePermissionButton(true) {},
        cancelButton = CancelPermissionButton(true),
        onChangeApproveType = { ApproveType.UNLIMITED },
    ),
    onCancel = {},
)
