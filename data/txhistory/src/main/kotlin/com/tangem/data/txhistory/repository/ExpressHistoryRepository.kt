package com.tangem.data.txhistory.repository

import com.tangem.data.txhistory.repository.converter.toEntity
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.datasource.api.express.models.response.ExchangeHistoryResponse
import com.tangem.datasource.api.express.models.response.ExchangeItemResponse
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryResponse
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Fetches express (exchange & onramp) transaction history from the API and persists it into the local database.
 *
 */
internal class ExpressHistoryRepository @Inject constructor(
    private val exchangeApi: TangemExpressApi,
    private val onrampApi: OnrampApi,
    private val expressHistoryDao: ExpressHistoryDao,
    private val expressSyncStateDao: ExpressSyncStateDao,
) {

    suspend fun fetchExchangeHistory(fromAddress: String, limit: Int = DEFAULT_LIMIT): ExchangeHistoryResponse {
        val state = syncState(ExpressSyncStateEntity.Type.EXCHANGE, fromAddress)

        val response = exchangeApi.getHistory(
            fromAddress = fromAddress,
            cursor = state?.afterCursor,
            limit = limit,
        ).getOrThrow()

        saveExchanges(ownerAddress = fromAddress, items = response.items)
        return response
    }

    suspend fun fetchExchangeHistoryDelta(
        fromAddress: String,
        limit: Int = DEFAULT_LIMIT,
    ): ExchangeHistoryDeltaResponse {
        val state = syncState(ExpressSyncStateEntity.Type.EXCHANGE, fromAddress)

        val response = exchangeApi.getHistoryDelta(
            fromAddress = fromAddress,
            cursor = state?.deltaCursor,
            limit = limit,
        ).getOrThrow()

        saveExchanges(ownerAddress = fromAddress, items = response.items)
        return response
    }

    suspend fun fetchOnrampHistory(payoutAddress: String, limit: Int = DEFAULT_LIMIT): OnrampHistoryResponse {
        val state = syncState(ExpressSyncStateEntity.Type.ONRAMP, payoutAddress)

        val response = onrampApi.getHistory(
            payoutAddress = payoutAddress,
            afterCursor = state?.afterCursor,
            limit = limit,
        ).getOrThrow()

        saveOnramps(ownerAddress = payoutAddress, items = response.items)
        return response
    }

    suspend fun fetchOnrampHistoryDelta(
        payoutAddress: String,
        limit: Int = DEFAULT_LIMIT,
    ): OnrampHistoryDeltaResponse {
        val state = syncState(ExpressSyncStateEntity.Type.ONRAMP, payoutAddress)

        val response = onrampApi.getHistoryDelta(
            payoutAddress = payoutAddress,
            cursor = state?.deltaCursor,
            limit = limit,
        ).getOrThrow()

        saveOnramps(ownerAddress = payoutAddress, items = response.items)
        return response
    }

    suspend fun syncState(type: ExpressSyncStateEntity.Type, address: String): ExpressSyncStateEntity? {
        return expressSyncStateDao.observe(type = type.name, address = address).first()
    }

    private suspend fun saveExchanges(ownerAddress: String, items: List<ExchangeItemResponse>) {
        expressHistoryDao.upsertExchanges(items.map { it.toEntity(ownerAddress) })
    }

    private suspend fun saveOnramps(ownerAddress: String, items: List<OnrampItemResponse>) {
        expressHistoryDao.upsertOnramps(items.map { it.toEntity(ownerAddress) })
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}