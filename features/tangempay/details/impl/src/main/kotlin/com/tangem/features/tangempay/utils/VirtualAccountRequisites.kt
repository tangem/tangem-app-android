package com.tangem.features.tangempay.utils

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.account.BankCredentials
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsBottomSheetComponent.RequisitesRow

/**
 * MVP0 placeholder for the daily deposit limit shown by the reused VA requisites bottom sheet.
 *
 * [REDACTED_TODO_COMMENT]
 */
internal const val VA_DAILY_DEPOSIT_LIMIT_PLACEHOLDER = "$10,000"

/**
 * Maps VA on-ramp [BankCredentials] to the requisites rows consumed by the reused
 * `VirtualAccountAddFundsBottomSheetComponent` (mirrors `VirtualAccountMainModel.buildRequisites`).
 */
internal fun BankCredentials.toRequisitesRows(): List<RequisitesRow> = listOf(
    RequisitesRow(
        title = resourceReference(R.string.virtual_account_requisites_beneficiary_name),
        titleForShare = "Beneficiary name",
        value = beneficiaryName,
    ),
    RequisitesRow(
        title = resourceReference(R.string.virtual_account_requisites_beneficiary_address),
        titleForShare = "Beneficiary address",
        value = beneficiaryAddress,
    ),
    RequisitesRow(
        title = resourceReference(R.string.virtual_account_requisites_bank_name),
        titleForShare = "Bank name",
        value = beneficiaryBankName,
    ),
    RequisitesRow(
        title = resourceReference(R.string.virtual_account_requisites_bank_address),
        titleForShare = "Bank address",
        value = beneficiaryBankAddress,
    ),
    RequisitesRow(
        title = resourceReference(R.string.virtual_account_requisites_account_number),
        titleForShare = "Account number",
        value = accountNumber,
    ),
    RequisitesRow(
        title = resourceReference(R.string.virtual_account_requisites_routing_number),
        titleForShare = "Routing number",
        value = routingNumber,
    ),
)