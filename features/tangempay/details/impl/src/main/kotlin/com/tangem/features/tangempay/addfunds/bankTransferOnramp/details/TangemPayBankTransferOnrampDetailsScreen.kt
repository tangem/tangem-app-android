package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.test.VirtualAccountRequisitesBottomSheetTestTags
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.details.TangemPayBankTransferOnrampDetailsScreenUM.DailyDepositLimitUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.details.TangemPayBankTransferOnrampDetailsScreenUM.RequisiteUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFees
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayRowValueText
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.isEmpty
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val CardShape = RoundedCornerShape(24.dp)

@Composable
internal fun TangemPayBankTransferOnrampDetailsScreen(
    state: TangemPayBankTransferOnrampDetailsScreenUM,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary),
    ) {
        TangemTopNavigation(
            title = resourceReference(R.string.tangempay_va_account_details),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            blurBackground = false,
            onClose = state.onCloseClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            RequisitesCard(requisites = state.requisites)
            if (!state.fees.isEmpty) {
                FeesSection(fees = state.fees)
            }
            state.dailyDepositLimit?.let { limit ->
                LimitationsBlock(limit = limit, modifier = Modifier.padding(top = 16.dp))
            }
        }
        TangemFooter {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(VirtualAccountRequisitesBottomSheetTestTags.SHARE_BUTTON),
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_share),
                onClick = state.onShareClick,
            )
        }
    }
}

@Composable
private fun FeesSection(fees: TangemPayOnrampFeesUM) {
    SectionCaption(
        text = resourceReference(R.string.tangempay_bank_transfer_details_fee_header),
        modifier = Modifier.padding(top = 24.dp),
    )
    TangemPayOnrampFees(fees = fees)
    if (fees !is TangemPayOnrampFeesUM.Error) {
        SectionCaption(text = resourceReference(R.string.tangempay_bank_transfer_swift_warning))
    }
}

@Composable
private fun RequisitesCard(requisites: ImmutableList<RequisiteUM>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        requisites.forEachIndexed { index, row ->
            RequisiteRow(row = row, divider = index != requisites.lastIndex)
        }
    }
}

@Composable
private fun RequisiteRow(row: RequisiteUM, divider: Boolean) {
    TangemRow(
        divider = divider,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            TangemRowText(text = row.title, role = TangemRowTextRole.Subtitle)
        },
        subtitleSlot = {
            TangemRowText(
                modifier = Modifier.testTag(VirtualAccountRequisitesBottomSheetTestTags.value(row.key)),
                text = stringReference(row.value),
                role = TangemRowTextRole.Title,
                maxLines = Int.MAX_VALUE,
            )
        },
        endSlot = {
            TangemButton(
                modifier = Modifier
                    .offset(x = 8.dp)
                    .testTag(VirtualAccountRequisitesBottomSheetTestTags.copyButton(row.key)),
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_copy_24),
                onClick = row.onCopyClick,
                size = TangemButton.Size.X9,
                variant = TangemButton.Variant.Ghost,
                contentDescription = row.title.resolveReference(),
            )
        },
    )
}

@Composable
private fun LimitationsBlock(limit: DailyDepositLimitUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionCaption(text = resourceReference(R.string.tangempay_bank_transfer_details_limitations))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(TangemTheme.colors3.bg.secondary),
        ) {
            TangemRow(
                divider = true,
                contentLead = TangemRowContentLead.Equal,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                titleSlot = {
                    TangemRowText(
                        modifier = Modifier.testTag(VirtualAccountRequisitesBottomSheetTestTags.DEPOSIT_LIMIT_TITLE),
                        text = resourceReference(R.string.tangempay_bank_transfer_details_daily_deposits),
                        role = TangemRowTextRole.Title,
                    )
                },
                valueSlot = { TangemPayRowValueText(text = limit.value.resolveReference()) },
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag(VirtualAccountRequisitesBottomSheetTestTags.DEPOSIT_LIMIT_SUBTITLE),
                text = limit.hint.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun SectionCaption(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        text = text.resolveReference(),
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 1026)
@Preview(showBackground = true, widthDp = 402, heightDp = 1026, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayBankTransferOnrampDetailsScreenPreview(
    @PreviewParameter(BankTransferOnrampDetailsPreviewProvider::class)
    state: TangemPayBankTransferOnrampDetailsScreenUM,
) {
    TangemThemePreviewRedesign {
        TangemPayBankTransferOnrampDetailsScreen(state = state)
    }
}

private class BankTransferOnrampDetailsPreviewProvider :
    CollectionPreviewParameterProvider<TangemPayBankTransferOnrampDetailsScreenUM>(
        collection = listOf(
            previewState(
                fees = TangemPayOnrampFeesUM.Content(
                    rows = persistentListOf(
                        previewFee(type = "FEDWIRE_ONRAMP", title = "FedWire"),
                        previewFee(type = "ACH_ONRAMP", title = "ACH"),
                    ),
                ),
                dailyDepositLimit = DailyDepositLimitUM(
                    value = stringReference("$10,000"),
                    hint = stringReference("If you want to deposit more than $10,000 – contact support"),
                ),
            ),
            previewState(fees = TangemPayOnrampFeesUM.Loading),
            previewState(fees = TangemPayOnrampFeesUM.Error(onRetryClick = {})),
        ),
    )

private fun previewState(fees: TangemPayOnrampFeesUM, dailyDepositLimit: DailyDepositLimitUM? = null) =
    TangemPayBankTransferOnrampDetailsScreenUM(
        requisites = persistentListOf(
            previewRequisite(key = "Account number", value = "0000000000000"),
            previewRequisite(key = "Routing number", value = "000000000"),
            previewRequisite(key = "Beneficiary name", value = "RAIN PAYMENTS INC"),
            previewRequisite(key = "Beneficiary address", value = "123 Finance St, New York, NY 10001, US"),
            previewRequisite(key = "Bank name", value = "SSB Bank"),
            previewRequisite(key = "Bank address", value = "8700 Perry Highway, Pittsburgh, PA 15237, US"),
        ),
        fees = fees,
        dailyDepositLimit = dailyDepositLimit,
        onShareClick = {},
        onCloseClick = {},
    )

private fun previewFee(type: String, title: String) = TangemPayOnrampFeesUM.FeeRow(
    type = type,
    title = stringReference(title),
    value = "$0",
)

private fun previewRequisite(key: String, value: String) = RequisiteUM(
    key = key,
    title = stringReference(key),
    value = value,
    onCopyClick = {},
)