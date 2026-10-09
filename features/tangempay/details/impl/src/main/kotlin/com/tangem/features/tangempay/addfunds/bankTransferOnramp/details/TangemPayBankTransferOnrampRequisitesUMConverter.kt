package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import androidx.annotation.StringRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.BankCredentials
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.details.TangemPayBankTransferOnrampDetailsScreenUM.RequisiteUM
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class TangemPayBankTransferOnrampRequisitesUMConverter(
    private val onCopyClick: (field: String, value: String) -> Unit,
) : Converter<BankCredentials, ImmutableList<RequisiteUM>> {

    override fun convert(value: BankCredentials): ImmutableList<RequisiteUM> = persistentListOf(
        requisite(
            field = "Account number",
            titleId = R.string.virtual_account_requisites_account_number,
            value = value.accountNumber,
        ),
        requisite(
            field = "Routing number",
            titleId = R.string.virtual_account_requisites_routing_number,
            value = value.routingNumber,
        ),
        requisite(
            field = "Beneficiary name",
            titleId = R.string.virtual_account_requisites_beneficiary_name,
            value = value.beneficiaryName,
        ),
        requisite(
            field = "Beneficiary address",
            titleId = R.string.virtual_account_requisites_beneficiary_address,
            value = value.beneficiaryAddress,
        ),
        requisite(
            field = "Bank name",
            titleId = R.string.virtual_account_requisites_bank_name,
            value = value.beneficiaryBankName,
        ),
        requisite(
            field = "Bank address",
            titleId = R.string.virtual_account_requisites_bank_address,
            value = value.beneficiaryBankAddress,
        ),
    )

    private fun requisite(field: String, @StringRes titleId: Int, value: String) = RequisiteUM(
        key = field,
        title = resourceReference(titleId),
        value = value,
        onCopyClick = { onCopyClick(field, value) },
    )
}