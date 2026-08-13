package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `GET /v1/wallets/{walletId}/joint-accounts`.
 *
 * Archived accounts are included — the full set is required to pick a free derivation index; filtering for
 * display is the client's job. An empty list (not a 404) means the wallet participates in none.
 */
@JsonClass(generateAdapter = true)
data class GetJointAccountsResponse(
    @Json(name = "jointAccounts") val jointAccounts: List<JointAccountDto>,
)