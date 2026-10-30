package com.tangem.features.moneyaccount.details

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.account.BankCredentials
import kotlinx.serialization.Serializable

@Serializable
internal sealed class MoneyAccountDetailsRoute : Route {

    @Serializable
    data object Details : MoneyAccountDetailsRoute()

    @Serializable
    data class BankDetails(val bankCredentials: BankCredentials) : MoneyAccountDetailsRoute()
}