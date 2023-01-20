package com.tangem.feature.swap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.MiddleEllipsisText
import com.tangem.core.ui.components.PrimaryButtonIconRight
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH10
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.SpacerH28
import com.tangem.core.ui.components.SpacerH32
import com.tangem.core.ui.components.atoms.Hand
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.ApprovePermissionButton
import com.tangem.feature.swap.models.CancelPermissionButton
import com.tangem.feature.swap.models.SwapPermissionState
import com.tangem.feature.swap.presentation.R

@Composable
fun SwapPermissionBottomSheetContent(
    data: SwapPermissionState.ReadyForRequest?,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .padding(horizontal = TangemTheme.dimens.spacing16),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Hand()

        SpacerH10()

        Text(
            text = stringResource(id = R.string.swapping_permission_header),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
        )

        SpacerH10()

        Text(
            text = stringResource(
                id = R.string.swapping_permission_subheader,
                data?.currency ?: "",
            ),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing8),
        )

        SpacerH16()

        ApprovalBottomSheetInfo(
            currency = data?.currency ?: "",
            amount = data?.amount ?: "",
            walletAddress = data?.walletAddress ?: "",
            spenderAddress = data?.spenderAddress ?: "",
            fee = data?.fee ?: "",
        )

        SpacerH28()

        PrimaryButtonIconRight(
            text = stringResource(id = R.string.swapping_permission_buttons_approve),
            icon = painterResource(id = R.drawable.ic_tangem_24),
            modifier = Modifier.fillMaxWidth(),
            onClick = { data?.approveButton?.onClick?.invoke() },
        )

        SpacerH12()

        SecondaryButton(
            text = stringResource(id = R.string.common_cancel),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                data?.cancelButton?.onClick?.invoke()
                onCancel()
            },
        )

        SpacerH32()
    }
}

@Composable
private fun ApprovalBottomSheetInfo(
    currency: String,
    amount: String,
    walletAddress: String,
    spenderAddress: String,
    fee: String,
) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.primary)
            .fillMaxWidth()
            .padding(
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing16,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountItem(currency = currency, amount = amount)
        DividerBottomSheet()
        WalletAddressItem(walletAddress = walletAddress)
        DividerBottomSheet()
        SpenderItem(spenderAddress = spenderAddress)
        DividerBottomSheet()
        FeeItem(fee = fee)
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing16),
    ) {
        Text(
            text = subtitle,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
            maxLines = 1,
        )

        SpacerH16()

        MiddleEllipsisText(
            text = value,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
            modifier = Modifier.padding(start = TangemTheme.dimens.spacing16),
        )
    }
}

@Composable
private fun AmountItem(currency: String, amount: String) {
    InformationItem(
        subtitle = stringResource(id = R.string.swapping_permission_rows_amount, currency),
        value = amount,
    )
}

@Composable
private fun WalletAddressItem(walletAddress: String) {
    InformationItem(
        subtitle = stringResource(id = R.string.swapping_permission_rows_your_wallet),
        value = walletAddress,
    )
}

@Composable
private fun SpenderItem(spenderAddress: String) {
    InformationItem(
        subtitle = stringResource(id = R.string.swapping_permission_rows_spender),
        value = spenderAddress,
    )
}

@Composable
private fun FeeItem(fee: String) {
    InformationItem(
        subtitle = stringResource(id = R.string.send_fee_label),
        value = fee,
    )
}

// region preview

@Preview
@Composable
private fun Preview_AgreementBottomSheet_InLightTheme() {
    TangemTheme(isDark = false) {
        SwapPermissionBottomSheetContent(data = previewData) {}
    }
}

@Preview
@Composable
private fun Preview_AgreementBottomSheet_InDarkTheme() {
    TangemTheme(isDark = true) {
        SwapPermissionBottomSheetContent(data = previewData) {}
    }
}

private val previewData = SwapPermissionState.ReadyForRequest(
    currency = "DAI",
    amount = "∞",
    walletAddress = "",
    spenderAddress = "",
    fee = "2,14$",
    approveButton = ApprovePermissionButton(true) {},
    cancelButton = CancelPermissionButton(true) {},
)

//endregion preview
