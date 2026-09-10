package com.tangem.features.tangempay.withdraw.bank

import kotlinx.serialization.Serializable

/** Where the bank withdraw wizard starts and which recipient it is pre-filled with. */
@Serializable
internal sealed class BankWithdrawEntry {

    @Serializable
    data object NewPersonal : BankWithdrawEntry()

    @Serializable
    data object NewBusiness : BankWithdrawEntry()

    @Serializable
    data class Saved(val bankAccountId: String) : BankWithdrawEntry()

    @Serializable
    data object SeeAll : BankWithdrawEntry()
}