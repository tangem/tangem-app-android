package com.tangem.features.virtualaccount.main

import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsBottomSheetComponent.RequisitesRow
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface VirtualAccountMainNavigationBottomSheetConfig {
    data class AddFunds(
        val requisites: List<RequisitesRow>,
        val dailyDepositLimit: String,
    ) : VirtualAccountMainNavigationBottomSheetConfig
}