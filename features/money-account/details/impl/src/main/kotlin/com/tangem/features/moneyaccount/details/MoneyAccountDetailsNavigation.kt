package com.tangem.features.moneyaccount.details

import com.tangem.domain.models.TokenReceiveConfig
import kotlinx.serialization.Serializable

@Serializable
internal sealed class MoneyAccountDetailsNavigation {

    @Serializable
    data class AddFundsOptions(val depositAddress: String?) : MoneyAccountDetailsNavigation()

    @Serializable
    data class Receive(val config: TokenReceiveConfig) : MoneyAccountDetailsNavigation()

    @Serializable
    data object BankDetailsError : MoneyAccountDetailsNavigation()
}