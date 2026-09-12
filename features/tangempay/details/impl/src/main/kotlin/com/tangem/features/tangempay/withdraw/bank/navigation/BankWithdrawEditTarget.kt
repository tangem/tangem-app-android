package com.tangem.features.tangempay.withdraw.bank.navigation

import kotlinx.serialization.Serializable

/** What the confirm screen can send the user back to correct. */
@Serializable
internal enum class BankWithdrawEditTarget { Amount, RecipientDetails, BankDetails }