package com.tangem.datasource.api.ethpool.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response for POST /api/v1/staking/pool/{network}/vaults/{vaultAddress}/accounts/list
 *
 * Each item is keyed by delegatorAddress and carries either a non-null [account]
 * or a per-address [error] (e.g. code 127108 — invalid delegator address).
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolAccountsListResponse(
    @Json(name = "list")
    val list: List<P2PEthPoolAccountListItem>,
)

@JsonClass(generateAdapter = true)
data class P2PEthPoolAccountListItem(
    @Json(name = "delegatorAddress")
    val delegatorAddress: String,
    @Json(name = "account")
    val account: P2PEthPoolAccountResponse?,
    @Json(name = "error")
    val error: P2PEthPoolErrorDetailsDTO?,
)