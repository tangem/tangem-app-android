package com.tangem.data.common.txhistory

import com.tangem.datasource.api.express.models.response.ExchangeItemResponse
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse

/**
 * Persists express (exchange/onramp) transactions into the local tx-history database and fetches any missing token
 * metadata for the referenced assets.
 */
interface ExpressHistoryRepository {

    suspend fun storeExchanges(ownerAddress: String, items: List<ExchangeItemResponse>)

    suspend fun storeOnramps(ownerAddress: String, items: List<OnrampItemResponse>)
}