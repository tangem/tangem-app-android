package com.tangem.features.send.v2.subcomponents.amount.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.amountScreen.AmountScreenContent
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.amount.model.SendAmountClickIntents
import com.tangem.features.send.v2.subcomponents.amount.ui.preview.SendAmountClickIntentsStub

@Composable
fun SendAmountContent(
    amountState: AmountState,
    isBalanceHidden: Boolean,
    clickIntents: SendAmountClickIntents,
    isSendWithSwapEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(TangemTheme.colors.background.tertiary)) {
        AmountScreenContent(
            amountState = amountState,
            isBalanceHidden = isBalanceHidden,
            clickIntents = clickIntents,
        )
        if (isSendWithSwapEnabled) {
            SendConvertTokenButton(
                onConvertToAnother = clickIntents::onConvertToAnotherToken,
            )
        }
    }
}

@Composable
private fun SendConvertTokenButton(onConvertToAnother: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = null,
                onClick = onConvertToAnother,
            ),
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_convert_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
                modifier = Modifier
                    .size(20.dp)
                    .background(TangemTheme.colors.control.unchecked, CircleShape)
                    .padding(2.dp),
            )
            Text(
                text = stringResourceSafe(com.tangem.common.ui.R.string.send_amount_convert_to_another_token),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SendAmountContent_Preview(@PreviewParameter(SendAmountContentPreviewProvider::class) params: AmountState) {
    TangemThemePreview {
        SendAmountContent(
            amountState = params,
            isBalanceHidden = true,
            clickIntents = SendAmountClickIntentsStub,
            isSendWithSwapEnabled = true,
        )
    }
}

private class SendAmountContentPreviewProvider : PreviewParameterProvider<AmountState> {
    override val values: Sequence<AmountState>
        get() = sequenceOf(
            AmountStatePreviewData.amountStateV2,
        )
}
// endregion