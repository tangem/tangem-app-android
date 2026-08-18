package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /v1/wallets/{walletId}/joint-accounts`.
 *
 * Archived accounts are excluded — they come from `/accounts/archived`, and the free derivation index comes
 * from `totalJointAccounts` in `GET /accounts`, not from this list. An empty list (not a 404) means the wallet
 * participates in none.
 */
@JsonClass(generateAdapter = true)
data class GetJointAccountsResponse(
    @Json(name = "jointAccounts") val jointAccounts: List<JointAccountDto>,
)