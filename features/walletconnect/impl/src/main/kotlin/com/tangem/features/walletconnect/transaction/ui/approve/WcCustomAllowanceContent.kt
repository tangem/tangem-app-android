package com.tangem.features.walletconnect.transaction.ui.approve

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import coil.compose.AsyncImage
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.approve.WcCustomAllowanceUM

@Composable
internal fun WcCustomAllowanceContent(
    allowance: WcCustomAllowanceUM,
    onAmountChange: (String) -> Unit,
    onToggleChange: (Boolean) -> Unit,
    onClickDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.tertiary)
            .fillMaxWidth()
            .padding(TangemTheme.dimens.size16),
    ) {
        AmountTextField(
            allowance = allowance,
            onAmountChange = onAmountChange,
        )

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing16))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
                .background(color = TangemTheme.colors.background.action)
                .padding(TangemTheme.dimens.spacing16)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResourceSafe(R.string.wc_unlimited_amount),
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.weight(1f),
            )
            TangemSwitch(
                checked = allowance.isUnlimited,
                onCheckedChange = onToggleChange,
            )
        }

        Spacer(modifier = Modifier.height(TangemTheme.dimens.spacing60))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResourceSafe(R.string.common_done),
            onClick = onClickDone,
        )
    }
}

@Composable
private fun AmountTextField(
    allowance: WcCustomAllowanceUM,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(TangemTheme.dimens.radius14))
            .background(color = TangemTheme.colors.background.action)
            .padding(
                end = TangemTheme.dimens.size8,
                top = TangemTheme.dimens.size4,
                bottom = TangemTheme.dimens.size4,
            )
            .fillMaxWidth(),
    ) {
        TextField(
            value = allowance.amountText,
            onValueChange = onAmountChange,
            label = {
                Text(
                    text = stringResourceSafe(R.string.send_amount_label),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                )
            },
            textStyle = TangemTheme.typography.body1,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
            ),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedTextColor = TangemTheme.colors.text.primary1,
                unfocusedTextColor = TangemTheme.colors.text.primary1,
                focusedPlaceholderColor = TangemTheme.colors.text.disabled,
                unfocusedPlaceholderColor = TangemTheme.colors.text.disabled,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = TangemTheme.colors.icon.primary1,
            ),
        )

        TokenWithNetworkIcon(
            tokenIconUrl = allowance.tokenIconUrl,
            networkIconRes = allowance.networkIconRes,
        )
    }
}

@Composable
private fun TokenWithNetworkIcon(
    tokenIconUrl: String,
    @DrawableRes networkIconRes: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size48),
    ) {
        AsyncImage(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(CircleShape)
                .align(Alignment.Center)
                .padding(TangemTheme.dimens.spacing8),
            model = tokenIconUrl,
            contentDescription = null,
        )
        Image(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(TangemTheme.dimens.size18)
                .border(TangemTheme.dimens.size2, TangemTheme.colors.background.action, CircleShape)
                .padding(TangemTheme.dimens.spacing2),
            painter = painterResource(id = networkIconRes),
            contentDescription = null,
        )
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun PreviewWcCustomAllowanceContent(
    @PreviewParameter(WcCustomAllowanceStateProvider::class) state: WcCustomAllowanceUM,
) {
    TangemThemePreview {
        TangemModalBottomSheet<WcCustomAllowanceUM>(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = state,
            ),
            containerColor = TangemTheme.colors.background.tertiary,
            title = {
                TangemModalBottomSheetTitle(
                    title = resourceReference(R.string.wc_custom_allowance_title),
                    endIconRes = null,
                    onEndClick = {},
                    startIconRes = R.drawable.ic_back_24,
                    onStartClick = {},
                )
            },
            content = {
                WcCustomAllowanceContent(
                    allowance = state,
                    onAmountChange = {},
                    onToggleChange = {},
                    onClickDone = {},
                )
            },
        )
    }
}

private class WcCustomAllowanceStateProvider : CollectionPreviewParameterProvider<WcCustomAllowanceUM>(
    listOf(
        WcCustomAllowanceUM(
            networkIconRes = R.drawable.img_eth_22,
            tokenIconUrl = "https://tangem.com",
            amountText = "100",
            isUnlimited = false,
        ),
    ),
)