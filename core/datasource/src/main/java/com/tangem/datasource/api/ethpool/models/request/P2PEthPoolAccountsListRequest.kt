package com.tangem.datasource.api.ethpool.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request body for POST /api/v1/staking/pool/{network}/vaults/{vaultAddress}/accounts/list
 *
 * Batch fetch of staking balances for multiple delegator addresses within a single vault.
 * Limit: up to 255 addresses per request. Addresses are deduplicated server-side.
 */
@JsonClass(generateAdapter = true)
data class P2PEthPoolAccountsListRequest(
    @Json(name = "delegatorAddresses")
    val delegatorAddresses: List<String>,
)