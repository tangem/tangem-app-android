package com.tangem.data.txhistory.repository

import com.tangem.data.common.txhistory.ExpressHistoryRepository
import com.tangem.data.txhistory.repository.factory.TokenInfoRepository
import com.tangem.data.txhistory.repository.factory.toAssetId
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.*
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.datasource.api.onramp.models.response.OnrampHistoryResponse
import com.tangem.datasource.api.onramp.models.response.OnrampItemResponse
import com.tangem.datasource.local.converter.toEntity
import com.tangem.datasource.local.txhistory.db.dao.ExpressHistoryDao
import com.tangem.datasource.local.txhistory.db.dao.ExpressSyncStateDao
import com.tangem.datasource.local.txhistory.db.entity.express.ExpressSyncStateEntity
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fetches express (exchange & onramp) transaction history from the API, persists it into the local database, and
 * fetches any missing token metadata for the referenced assets.
 */
internal class DefaultExpressHistoryRepository @Inject constructor(
    private val exchangeApi: TangemExpressApi,
    private val onrampApi: OnrampApi,
    private val expressHistoryDao: ExpressHistoryDao,
    private val expressSyncStateDao: ExpressSyncStateDao,
    private val tokenInfoRepository: TokenInfoRepository,
    private val appScope: AppCoroutineScope,
) : ExpressHistoryRepository {

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

        storeExchanges(ownerAddress = fromAddress, items = response.items)
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

        storeExchanges(ownerAddress = fromAddress, items = response.items)
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

        storeOnramps(ownerAddress = payoutAddress, items = response.items)
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

        storeOnramps(ownerAddress = payoutAddress, items = response.items)
        persistDeltaState(
            type = ExpressSyncStateEntity.Type.ONRAMP,
            address = payoutAddress,
            pagination = response.pagination,
        )
        return response
    }

    override suspend fun storeExchanges(ownerAddress: String, items: List<ExchangeItemResponse>) {
        if (items.isEmpty()) return
        val entities = items.map { it.toEntity(ownerAddress) }
        expressHistoryDao.upsertExchanges(entities)
        fetchMissingTokenInfo(
            buildSet {
                entities.forEach { entity ->
                    add(entity.from.toAssetId())
                    add(entity.to.toAssetId())
                }
            },
        )
    }

    override suspend fun storeOnramps(ownerAddress: String, items: List<OnrampItemResponse>) {
        if (items.isEmpty()) return
        val entities = items.map { it.toEntity(ownerAddress) }
        expressHistoryDao.upsertOnramps(entities)
        fetchMissingTokenInfo(entities.mapTo(mutableSetOf()) { it.to.toAssetId() })
    }

    suspend fun syncState(type: ExpressSyncStateEntity.Type, address: String): ExpressSyncStateEntity? {
        return expressSyncStateDao.observe(type = type.name, address = address).first()
    }

    private fun fetchMissingTokenInfo(assetIds: Set<ExpressAsset.ID>) {
        appScope.launch { tokenInfoRepository.fetchMissing(assetIds) }
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