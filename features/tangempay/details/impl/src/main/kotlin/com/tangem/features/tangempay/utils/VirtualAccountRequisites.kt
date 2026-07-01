package com.tangem.features.tangempay.utils

import com.tangem.domain.models.account.BankCredentials
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
        title = "Beneficiary name and address",
        titleForShare = "Beneficiary name and address",
        value = "$beneficiaryName\n$beneficiaryAddress",
    ),
    RequisitesRow(
        title = "Bank name and address",
        titleForShare = "Bank name and address",
        value = "$beneficiaryBankName\n$beneficiaryBankAddress",
    ),
    RequisitesRow(
        title = "Account number",
        titleForShare = "Account number",
        value = accountNumber,
    ),
    RequisitesRow(
        title = "Routing number",
        titleForShare = "Routing number",
        value = routingNumber,
    ),
)