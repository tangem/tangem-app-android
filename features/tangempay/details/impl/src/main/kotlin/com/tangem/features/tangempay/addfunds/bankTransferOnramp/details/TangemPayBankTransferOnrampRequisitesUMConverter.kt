package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import androidx.annotation.StringRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.BankCredentials
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.details.TangemPayBankTransferOnrampDetailsScreenUM.RequisiteUM
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayBankTransferOnrampRequisitesUMConverter(
    private val onCopyClick: (field: RequisiteField, value: String) -> Unit,
) : Converter<BankCredentials, ImmutableList<RequisiteUM>> {

    override fun convert(value: BankCredentials): ImmutableList<RequisiteUM> = listOfNotNull(
        requisite(
            field = RequisiteField.ACCOUNT_NUMBER,
            titleId = R.string.virtual_account_requisites_account_number,
            value = value.accountNumber,
        ),
        requisite(
            field = RequisiteField.ROUTING_NUMBER,
            titleId = R.string.virtual_account_requisites_routing_number,
            value = value.routingNumber,
        ),
        requisite(
            field = RequisiteField.BENEFICIARY_NAME,
            titleId = R.string.virtual_account_requisites_beneficiary_name,
            value = value.beneficiaryName,
        ),
        requisite(
            field = RequisiteField.BENEFICIARY_ADDRESS,
            titleId = R.string.virtual_account_requisites_beneficiary_address,
            value = value.beneficiaryAddress,
        ),
        requisite(
            field = RequisiteField.BANK_NAME,
            titleId = R.string.virtual_account_requisites_bank_name,
            value = value.beneficiaryBankName,
        ),
        requisite(
            field = RequisiteField.BANK_ADDRESS,
            titleId = R.string.virtual_account_requisites_bank_address,
            value = value.beneficiaryBankAddress,
        ),
    ).toImmutableList()

    private fun requisite(field: RequisiteField, @StringRes titleId: Int, value: String): RequisiteUM? {
        if (value.isBlank()) return null
        return RequisiteUM(
            key = field.label,
            title = resourceReference(titleId),
            value = value,
            onCopyClick = { onCopyClick(field, value) },
        )
    }
}

internal val RequisiteField.label: String
    get() = when (this) {
        RequisiteField.ACCOUNT_NUMBER -> "Account number"
        RequisiteField.ROUTING_NUMBER -> "Routing number"
        RequisiteField.BENEFICIARY_NAME -> "Beneficiary name"
        RequisiteField.BENEFICIARY_ADDRESS -> "Beneficiary address"
        RequisiteField.BANK_NAME -> "Bank name"
        RequisiteField.BANK_ADDRESS -> "Bank address"
    }