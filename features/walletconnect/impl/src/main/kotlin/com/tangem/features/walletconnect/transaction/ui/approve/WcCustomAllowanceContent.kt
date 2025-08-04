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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetWithFooter
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import java.math.BigDecimal

@Composable
internal fun WcCustomAllowanceContent(
    state: WcSpendAllowanceUM,
    onClickDone: (BigDecimal, Boolean) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf(state.amountValue.toPlainString()) }
    var isUnlimited by remember { mutableStateOf(state.isUnlimited) }

    val amount: BigDecimal by remember(amountText) {
        mutableStateOf(amountText.parseBigDecimalOrNull() ?: state.amountValue)
    }

    TangemModalBottomSheetWithFooter<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors.background.tertiary,
        onBack = onBack,
        title = {
            TangemModalBottomSheetTitle(
                title = resourceReference(R.string.wc_custom_allowance_title),
                startIconRes = R.drawable.ic_back_24,
                onStartClick = onBack,
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .background(TangemTheme.colors.background.tertiary)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                AmountTextField(
                    amountText = amountText,
                    onAmountChange = { amountText = it },
                    isEnabled = !isUnlimited,
                    tokenImageUrl = state.tokenImageUrl,
                    networkIconRes = state.networkIconRes,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TangemTheme.colors.background.action)
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = stringResourceSafe(R.string.wc_unlimited_amount),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.primary1,
                        modifier = Modifier.weight(1f),
                    )
                    TangemSwitch(
                        checked = isUnlimited,
                        onCheckedChange = { isUnlimited = it },
                    )
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        },
        footer = {
            PrimaryButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.common_done),
                onClick = { onClickDone(amount, isUnlimited) },
            )
        },
    )
}

// TODO: [REDACTED_JIRA] refactor to use AmountField from core, use transformers to parse
@Composable
private fun AmountTextField(
    amountText: String,
    onAmountChange: (String) -> Unit,
    isEnabled: Boolean,
    tokenImageUrl: String?,
    @DrawableRes networkIconRes: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(TangemTheme.colors.background.action)
            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
    ) {
        TextField(
            value = amountText,
            onValueChange = onAmountChange,
            enabled = isEnabled,
            label = {
                Text(
                    text = stringResourceSafe(R.string.send_amount_label),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.tertiary,
                )
            },
            textStyle = TangemTheme.typography.body1,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                focusedTextColor = TangemTheme.colors.text.primary1,
                unfocusedTextColor = TangemTheme.colors.text.primary1,
                focusedPlaceholderColor = TangemTheme.colors.text.disabled,
                unfocusedPlaceholderColor = TangemTheme.colors.text.disabled,
                disabledContainerColor = Color.Transparent,
                disabledTextColor = TangemTheme.colors.text.disabled,
                disabledIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = TangemTheme.colors.icon.primary1,
            ),
        )

        TokenWithNetworkIcon(
            tokenIconUrl = tokenImageUrl,
            networkIconRes = networkIconRes,
        )
    }
}

@Composable
private fun TokenWithNetworkIcon(
    tokenIconUrl: String?,
    @DrawableRes networkIconRes: Int?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(48.dp),
    ) {
        AsyncImage(
            model = tokenIconUrl,
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(CircleShape)
                .align(Alignment.Center)
                .padding(8.dp),
            placeholder = painterResource(R.drawable.ic_nft_placeholder_20),
            error = painterResource(R.drawable.ic_nft_placeholder_20),
        )

        if (networkIconRes != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .border(2.dp, TangemTheme.colors.background.action, CircleShape)
                    .padding(2.dp),
                painter = painterResource(networkIconRes),
                contentDescription = null,
            )
        }
    }
}

@Composable
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun PreviewWcCustomAllowanceContent(
    @PreviewParameter(WcCustomAllowanceStateProvider::class) state: WcSpendAllowanceUM,
) {
    TangemThemePreview {
        TangemModalBottomSheet<WcSpendAllowanceUM>(
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
                    state = state,
                    onClickDone = { _, _ -> },
                    onBack = {},
                    onDismiss = {},
                )
            },
        )
    }
}

private class WcCustomAllowanceStateProvider : CollectionPreviewParameterProvider<WcSpendAllowanceUM>(
    listOf(
        WcSpendAllowanceUM(
            networkIconRes = R.drawable.img_eth_22,
            tokenImageUrl = "https://tangem.com",
            amountText = TextReference.Str("100"),
            amountValue = BigDecimal("100"),
            tokenSymbol = "ETH",
            isUnlimited = false,
        ),
    ),
)