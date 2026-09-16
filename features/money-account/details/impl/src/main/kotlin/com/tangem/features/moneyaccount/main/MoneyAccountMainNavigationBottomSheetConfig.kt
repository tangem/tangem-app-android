package com.tangem.features.moneyaccount.main

import com.tangem.features.moneyaccount.details.component.VirtualAccountAddFundsBottomSheetComponent.RequisitesRow
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface MoneyAccountMainNavigationBottomSheetConfig {
    data class AddFunds(
        val requisites: List<RequisitesRow>,
        val dailyDepositLimit: String,
    ) : MoneyAccountMainNavigationBottomSheetConfig
}