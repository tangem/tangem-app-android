package com.tangem.domain.common.extensions

import com.tangem.common.services.Result
import com.tangem.datasource.api.tangemTech.CoinsResponse
import com.tangem.datasource.api.tangemTech.TangemTechService

suspend fun TangemTechService.getTokens(
    contractAddress: String,
    networkId: String? = null,
    active: Boolean? = null,
): Result<CoinsResponse> = coins(
            contractAddress = contractAddress,
            networkIds = networkId,
            active = active
        )

suspend fun TangemTechService.getListOfCoins(
    networkIds: List<String>,
    active: Boolean? = null,
    searchText: String? = null,
    offset: Int? = null,
    limit: Int? = null
): Result<CoinsResponse> = coins(
            networkIds = networkIds.joinToString(","),
            active = active,
            searchText = searchText,
            offset = offset,
            limit = limit
        )
