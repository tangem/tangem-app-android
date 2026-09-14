package com.tangem.features.tangempay.withdraw.bank.success.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayBankWithdrawSuccessScreen(
    amount: String?,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Success,
        title = amount
            ?.let { resourceReference(R.string.tangempay_withdraw_bank_success_title, wrappedList(it)) }
            ?: resourceReference(R.string.common_sending),
        description = resourceReference(R.string.tangempay_withdraw_bank_success_description),
        asset = {
            TangemStatusIcon(
                kind = TangemStatusIcon.Kind.Checkmark,
                modifier = Modifier.size(StatusIconSize),
            )
        },
        footer = {
            TangemButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(FooterPadding),
            ) {
                TangemButton(
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_done),
                    onClick = onDoneClick,
                )
            }
        },
    )
}

private val StatusIconSize = 40.dp
private val FooterPadding = 16.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Preview(showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayBankWithdrawSuccessScreenPreview() {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawSuccessScreen(amount = "$500", onDoneClick = {})
    }
}