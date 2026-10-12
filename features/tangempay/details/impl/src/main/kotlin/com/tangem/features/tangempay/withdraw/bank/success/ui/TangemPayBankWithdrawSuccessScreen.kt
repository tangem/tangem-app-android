package com.tangem.features.tangempay.withdraw.bank.success.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.success.state.TangemPayBankWithdrawSuccessUM
import com.tangem.features.tangempay.withdraw.bank.success.state.TangemPayBankWithdrawSuccessUM.RowUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayBankWithdrawSuccessScreen(state: TangemPayBankWithdrawSuccessUM, modifier: Modifier = Modifier) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Info,
        title = state.title,
        description = state.subtitle,
        asset = {
            TangemStatusIcon(
                kind = TangemStatusIcon.Kind.Checkmark,
                modifier = Modifier.size(StatusIconSize),
                tint = TangemTheme.colors3.icon.status.info,
            )
        },
        details = if (state.rows.isEmpty()) {
            null
        } else {
            {
                state.rows.forEachIndexed { index, row ->
                    DetailRow(row = row, divider = index < state.rows.lastIndex)
                }
            }
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
                    text = resourceReference(R.string.common_close),
                    onClick = state.onCloseClick,
                )
            }
        },
    )
}

@Composable
private fun DetailRow(row: RowUM, divider: Boolean) {
    TangemRow(
        divider = divider,
        titleSlot = {
            Text(
                text = row.label.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        },
        valueSlot = {
            Text(
                text = row.value,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        },
    )
}

private val StatusIconSize = 40.dp
private val FooterPadding = 16.dp

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Preview(showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayBankWithdrawSuccessScreenPreview(
    @PreviewParameter(SuccessPreviewProvider::class) state: TangemPayBankWithdrawSuccessUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankWithdrawSuccessScreen(state = state)
    }
}

private class SuccessPreviewProvider : PreviewParameterProvider<TangemPayBankWithdrawSuccessUM> {

    private val title = resourceReference(R.string.tangempay_withdraw_bank_success_title, wrappedList("$500"))
    private val subtitle = stringReference("May 2, 2028, 9:41 AM")

    override val values: Sequence<TangemPayBankWithdrawSuccessUM> = sequenceOf(
        TangemPayBankWithdrawSuccessUM(
            title = title,
            subtitle = subtitle,
            rows = persistentListOf(
                RowUM(resourceReference(R.string.send_recipient), "Saburo Arasaka"),
                RowUM(resourceReference(R.string.tangempay_withdraw_bank_field_bank_name), "Arasaka Bank"),
                RowUM(resourceReference(R.string.tangempay_withdraw_bank_field_account_number), "0000000000"),
            ),
            onCloseClick = {},
        ),
        TangemPayBankWithdrawSuccessUM(
            title = title,
            subtitle = subtitle,
            rows = persistentListOf(
                RowUM(resourceReference(R.string.send_recipient), "Saburo Arasaka"),
                RowUM(resourceReference(R.string.tangempay_withdraw_bank_field_account_number), "0000000000"),
            ),
            onCloseClick = {},
        ),
    )
}