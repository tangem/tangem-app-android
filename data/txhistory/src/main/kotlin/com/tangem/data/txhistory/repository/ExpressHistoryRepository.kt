package com.tangem.data.txhistory.repository

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExchangeHistoryDeltaResponse
import com.tangem.datasource.api.express.models.response.ExchangeHistoryResponse
import com.tangem.datasource.api.express.models.response.ExchangeItemResponse
import com.tangem.datasource.api.express.models.response.ExpressPagination
import com.tangem.datasource.api.express.models.response.ExpressPaginationDelta
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryResponse
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse
import com.tangem.datasource.local.converter.toEntity
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.domain.models.wallet.UserWalletId
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

    suspend fun fetchExchangeHistory(
        fromAddress: String,
        userWalletId: UserWalletId,
        limit: Int = DEFAULT_LIMIT,
    ): ExchangeHistoryResponse {
        val state = syncState(ExpressSyncStateEntity.Type.EXCHANGE, fromAddress)

        val response = exchangeApi.getHistory(
            userWalletId = userWalletId.stringValue,
            fromAddress = fromAddress,
            cursor = state?.afterCursor,
            limit = limit,
        ).getOrThrow()

        saveExchanges(ownerAddress = fromAddress, items = response.items)
        persistHistoryState(
            type = ExpressSyncStateEntity.Type.EXCHANGE,
            address = fromAddress,
            previous = state,
            pagination = response.pagination,
        )
        return response
    }

    suspend fun fetchExchangeHistoryDelta(
        fromAddress: String,
        userWalletId: UserWalletId,
        limit: Int = DEFAULT_LIMIT,
    ): ExchangeHistoryDeltaResponse {
        val state = syncState(ExpressSyncStateEntity.Type.EXCHANGE, fromAddress)

        val response = exchangeApi.getHistoryDelta(
            userWalletId = userWalletId.stringValue,
            fromAddress = fromAddress,
            cursor = state?.deltaCursor,
            limit = limit,
        ).getOrThrow()

        saveExchanges(ownerAddress = fromAddress, items = response.items)
        persistDeltaState(
            type = ExpressSyncStateEntity.Type.EXCHANGE,
            address = fromAddress,
            pagination = response.pagination,
        )
        return response
    }

    suspend fun fetchOnrampHistory(
        payoutAddress: String,
        userWalletId: UserWalletId,
        limit: Int = DEFAULT_LIMIT,
    ): OnrampHistoryResponse {
        val state = syncState(ExpressSyncStateEntity.Type.ONRAMP, payoutAddress)

        val response = onrampApi.getHistory(
            userWalletId = userWalletId.stringValue,
            payoutAddress = payoutAddress,
            afterCursor = state?.afterCursor,
            limit = limit,
        ).getOrThrow()

        saveOnramps(ownerAddress = payoutAddress, items = response.items)
        persistHistoryState(
            type = ExpressSyncStateEntity.Type.ONRAMP,
            address = payoutAddress,
            previous = state,
            pagination = response.pagination,
        )
        return response
    }

    suspend fun fetchOnrampHistoryDelta(
        payoutAddress: String,
        userWalletId: UserWalletId,
        limit: Int = DEFAULT_LIMIT,
    ): OnrampHistoryDeltaResponse {
        val state = syncState(ExpressSyncStateEntity.Type.ONRAMP, payoutAddress)

        val response = onrampApi.getHistoryDelta(
            userWalletId = userWalletId.stringValue,
            payoutAddress = payoutAddress,
            cursor = state?.deltaCursor,
            limit = limit,
        ).getOrThrow()

        saveOnramps(ownerAddress = payoutAddress, items = response.items)
        persistDeltaState(
            type = ExpressSyncStateEntity.Type.ONRAMP,
            address = payoutAddress,
            pagination = response.pagination,
        )
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

    private suspend fun persistHistoryState(
        type: ExpressSyncStateEntity.Type,
        address: String,
        previous: ExpressSyncStateEntity?,
        pagination: ExpressPagination,
    ) {
        if (previous == null) {
            expressSyncStateDao.upsert(
                ExpressSyncStateEntity(
                    type = type.name,
                    address = address,
                    isInitialCompleted = !pagination.hasMore,
                    afterCursor = pagination.endCursor,
                    deltaCursor = pagination.startDeltaCursor,
                ),
            )
        } else {
            expressSyncStateDao.updateHistoryCursor(
                type = type.name,
                address = address,
                afterCursor = pagination.endCursor,
                isInitialCompleted = !pagination.hasMore,
            )
        }
    }

    private suspend fun persistDeltaState(
        type: ExpressSyncStateEntity.Type,
        address: String,
        pagination: ExpressPaginationDelta,
    ) {
        val cursor = pagination.startCursor ?: return
        expressSyncStateDao.updateDeltaCursor(type = type.name, address = address, deltaCursor = cursor)
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}