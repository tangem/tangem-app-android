package com.tangem.data.txhistory.repository

import androidx.room.withTransaction
import com.tangem.data.common.txhistory.ExpressHistoryRepository
import com.tangem.data.txhistory.repository.converter.ExpressStatusMapper
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntities
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntity
import com.tangem.data.txhistory.repository.factory.toAssetId
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.local.converter.toEntity
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.repository.TokenInfoRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.grow.datasource.express.ExpressUtils
import com.tangem.grow.datasource.express.TangemExpressApi
import com.tangem.grow.datasource.express.models.response.*
import com.tangem.grow.datasource.onramp.OnrampApi
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryDeltaResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampHistoryResponse
import com.tangem.grow.datasource.onramp.models.response.OnrampItemResponse
import com.tangem.store.datasource.txhistory.db.TxHistoryDatabase
import com.tangem.store.datasource.txhistory.db.dao.ExpressHistoryDao
import com.tangem.store.datasource.txhistory.db.dao.HistoryIndexDao
import com.tangem.store.datasource.txhistory.db.dao.HistorySyncStateDao
import com.tangem.store.datasource.txhistory.db.entity.sync.HistorySyncStateEntity
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Fetches express (exchange & onramp) transaction history from the API, persists it into the local database, and
 * fetches any missing token metadata for the referenced assets.
 */
@Suppress("LongParameterList")
internal class DefaultExpressHistoryRepository @Inject constructor(
    private val exchangeApi: TangemExpressApi,
    private val onrampApi: OnrampApi,
    private val expressHistoryDao: ExpressHistoryDao,
    private val historyIndexDao: HistoryIndexDao,
    private val historySyncStateDao: HistorySyncStateDao,
    private val tokenInfoRepository: TokenInfoRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val expressUtils: ExpressUtils,
    private val database: TxHistoryDatabase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
) : ExpressHistoryRepository {

    suspend fun fetchExchangeHistory(userWalletId: UserWalletId, limit: Int = DEFAULT_LIMIT): ExchangeHistoryResponse {
        val state = syncState(HistorySyncStateEntity.Type.EXCHANGE, userWalletId)

        val response = exchangeApi.getHistory(
            userWalletId = userWalletId.stringValue,
            cursor = state?.afterCursor,
            limit = limit,
        ).getOrThrow()

        storeExchanges(items = response.items)
        persistHistoryState(
            type = HistorySyncStateEntity.Type.EXCHANGE,
            userWalletId = userWalletId,
            previous = state,
            pagination = response.pagination,
        )
        return response
    }

    suspend fun fetchExchangeHistoryDelta(
        userWalletId: UserWalletId,
        limit: Int = DEFAULT_LIMIT,
    ): ExchangeHistoryDeltaResponse {
        val state = syncState(HistorySyncStateEntity.Type.EXCHANGE, userWalletId)

        val response = exchangeApi.getHistoryDelta(
            userWalletId = userWalletId.stringValue,
            cursor = state?.deltaCursor,
            limit = limit,
        ).getOrThrow()

        storeExchanges(items = response.items)
        persistDeltaState(
            type = HistorySyncStateEntity.Type.EXCHANGE,
            userWalletId = userWalletId,
            pagination = response.pagination,
        )
        return response
    }

    suspend fun fetchOnrampHistory(userWalletId: UserWalletId, limit: Int = DEFAULT_LIMIT): OnrampHistoryResponse {
        val state = syncState(HistorySyncStateEntity.Type.ONRAMP, userWalletId)

        val response = onrampApi.getHistory(
            userWalletId = userWalletId.stringValue,
            afterCursor = state?.afterCursor,
            limit = limit,
        ).getOrThrow()

        storeOnramps(items = response.items)
        persistHistoryState(
            type = HistorySyncStateEntity.Type.ONRAMP,
            userWalletId = userWalletId,
            previous = state,
            pagination = response.pagination,
        )
        return response
    }

    suspend fun fetchOnrampHistoryDelta(
        userWalletId: UserWalletId,
        limit: Int = DEFAULT_LIMIT,
    ): OnrampHistoryDeltaResponse {
        val state = syncState(HistorySyncStateEntity.Type.ONRAMP, userWalletId)

        val response = onrampApi.getHistoryDelta(
            userWalletId = userWalletId.stringValue,
            cursor = state?.deltaCursor,
            limit = limit,
        ).getOrThrow()

        storeOnramps(items = response.items)
        persistDeltaState(
            type = HistorySyncStateEntity.Type.ONRAMP,
            userWalletId = userWalletId,
            pagination = response.pagination,
        )
        return response
    }

    /**
     * Re-reads from the backend every express operation of [currency] that is still in flight and writes the
     * answer back into the local database, so an operation whose status moved on while the history sync was
     * idle stops the screen from showing a stale one.
     *
     * The operations are looked up by the asset ([ExpressAsset.ID]) plus the wallet's addresses on its network:
     * a swap counts when the currency is either of its legs, an onramp when it is the payout side. Only
     * non-terminal statuses are refreshed — a finished deal expects no further update.
     *
     * The requests run in parallel and a failed one is logged and dropped: refreshing is best-effort, and one
     * provider that does not answer must not cost the rest their update. Returns once all of them have settled.
     */
    suspend fun refreshActiveStatuses(userWalletId: UserWalletId, currency: CryptoCurrency): Boolean {
        val addresses = getAddresses(userWalletId, currency.network)
        if (addresses.isEmpty()) return false

        val assetId = ExpressAsset.ID(currency)
        val exchangeIds = expressHistoryDao.getActiveExchangeIds(
            addresses = addresses,
            network = assetId.networkId,
            contract = assetId.contractAddress,
            activeStatuses = ExpressStatusMapper.activeExchangeStatuses,
        )
        val onrampIds = expressHistoryDao.getActiveOnrampIds(
            addresses = addresses,
            network = assetId.networkId,
            contract = assetId.contractAddress,
            activeStatuses = ExpressStatusMapper.activeOnrampStatuses,
        )
        if (exchangeIds.isEmpty() && onrampIds.isEmpty()) return false

        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return false
        val userWalletId = userWallet.walletId.stringValue
        val refCode = expressUtils.getRefCode(userWallet = userWallet)

        coroutineScope {
            val requests = exchangeIds.map { txId ->
                async { refreshExchangeStatus(userWalletId = userWalletId, refCode = refCode, txId = txId) }
            } + onrampIds.map { txId ->
                async { refreshOnrampStatus(userWalletId = userWalletId, refCode = refCode, txId = txId) }
            }
            requests.awaitAll()
        }
        return true
    }

    private suspend fun refreshExchangeStatus(userWalletId: String, refCode: String?, txId: String) {
        runSuspendCatching {
            val response = exchangeApi.getExchangeStatus(
                userWalletId = userWalletId,
                refCode = refCode,
                txId = txId,
            ).getOrThrow()

            storeExchanges(items = listOf(response))
        }.onFailure { TangemLogger.e("Failed to refresh exchange status, txId=$txId", it) }
    }

    private suspend fun refreshOnrampStatus(userWalletId: String, refCode: String?, txId: String) {
        runSuspendCatching {
            val response = onrampApi.getStatus(
                userWalletId = userWalletId,
                refCode = refCode,
                txId = txId,
            ).getOrThrow()

            storeOnramps(items = listOf(response))
        }.onFailure { TangemLogger.e("Failed to refresh onramp status, txId=$txId", it) }
    }

    private suspend fun getAddresses(userWalletId: UserWalletId, network: Network): List<String> {
        val walletAddresses = walletManagersFacade.getAddresses(userWalletId, network).map { it.value }
        val dynamicAddresses = walletManagersFacade.usedDynamicAddresses(userWalletId, network).orEmpty()
        return (walletAddresses + dynamicAddresses).distinct()
    }

    override suspend fun storeExchanges(items: List<ExchangeItemResponse>) {
        val entities = items.mapNotNull { it.toEntity() }
        if (entities.isEmpty()) return
        database.withTransaction {
            expressHistoryDao.upsertExchanges(entities)
            historyIndexDao.upsert(entities.flatMap { it.toHistoryIndexEntities() })
        }
        tokenInfoRepository.fetchMissing(
            buildSet {
                entities.forEach { entity ->
                    add(entity.from.toAssetId())
                    add(entity.to.toAssetId())
                }
            },
        )
    }

    override suspend fun storeOnramps(items: List<OnrampItemResponse>) {
        if (items.isEmpty()) return
        val entities = items.map { it.toEntity() }
        database.withTransaction {
            expressHistoryDao.upsertOnramps(entities)
            historyIndexDao.upsert(entities.map { it.toHistoryIndexEntity() })
        }
        tokenInfoRepository.fetchMissing(entities.mapTo(mutableSetOf()) { it.to.toAssetId() })
    }

    suspend fun syncState(type: HistorySyncStateEntity.Type, userWalletId: UserWalletId): HistorySyncStateEntity? {
        return historySyncStateDao.observe(type = type.name, userWalletId = userWalletId.stringValue).first()
    }

    private suspend fun persistHistoryState(
        type: HistorySyncStateEntity.Type,
        userWalletId: UserWalletId,
        previous: HistorySyncStateEntity?,
        pagination: ExpressPagination,
    ) {
        if (previous == null) {
            historySyncStateDao.upsert(
                HistorySyncStateEntity(
                    type = type.name,
                    userWalletId = userWalletId.stringValue,
                    isInitialCompleted = !pagination.hasMore,
                    afterCursor = pagination.endCursor,
                    deltaCursor = pagination.startDeltaCursor,
                ),
            )
        } else {
            historySyncStateDao.updateHistoryCursor(
                type = type.name,
                userWalletId = userWalletId.stringValue,
                afterCursor = pagination.endCursor,
                isInitialCompleted = !pagination.hasMore,
            )
        }
    }

    private suspend fun persistDeltaState(
        type: HistorySyncStateEntity.Type,
        userWalletId: UserWalletId,
        pagination: ExpressPaginationDelta,
    ) {
        val cursor = pagination.startCursor ?: return
        historySyncStateDao.updateDeltaCursor(
            type = type.name,
            userWalletId = userWalletId.stringValue,
            deltaCursor = cursor,
        )
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}