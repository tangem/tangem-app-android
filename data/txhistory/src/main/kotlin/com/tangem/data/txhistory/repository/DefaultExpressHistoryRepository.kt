package com.tangem.data.txhistory.repository

import androidx.room.withTransaction
import com.tangem.data.common.converter.ExpressProviderConverter
import com.tangem.data.common.txhistory.ExpressHistoryRepository
import com.tangem.data.txhistory.repository.converter.ExpressStatusMapper
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntities
import com.tangem.data.txhistory.repository.converter.toHistoryIndexEntity
import com.tangem.data.txhistory.repository.factory.toAssetId
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.local.converter.toEntity
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
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
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
) : ExpressHistoryRepository {

    private val expressProviderConverter = ExpressProviderConverter()

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

        val newlyRefunded = findNewlyRefunded(items = response.items)

        storeExchanges(items = response.items)
        newlyRefunded.forEach { item -> addRefundCurrencyIfNeeded(userWalletId = userWalletId, response = item) }

        persistDeltaState(
            type = HistorySyncStateEntity.Type.EXCHANGE,
            userWalletId = userWalletId,
            pagination = response.pagination,
        )
        return response
    }

    /**
     * The deals of [items] that are turning refunded right now — the stored row exists and is not refunded yet.
     *
     * Must be read before the write, since the write is what makes them indistinguishable from a deal that was
     * already refunded. A deal with no stored row is deliberately not counted: that is the initial history walk and
     * a reinstall, where every past refund would otherwise land in the portfolio at once.
     */
    private suspend fun findNewlyRefunded(items: List<ExchangeItemResponse>): List<ExchangeItemResponse> {
        val refunded = items.filter { it.status == ExpressStatusMapper.refundedExchangeStatus }
        if (refunded.isEmpty()) return emptyList()

        return refunded.filter { item ->
            val stored = expressHistoryDao.observeExchangeById(item.txId).first()
            stored != null && stored.status != ExpressStatusMapper.refundedExchangeStatus
        }
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
     * The express operations of [currency] that are still in flight, re-emitted whenever the local database
     * changes, so a caller polling their statuses learns about a new deal and about a finished one on its own.
     *
     * The operations are looked up by the asset ([ExpressAsset.ID]) plus the wallet's addresses on its network:
     * a swap counts when the currency is either of its legs, an onramp when it is the payout side. Only
     * non-terminal statuses are returned — a finished deal expects no further update.
     */
    internal fun observeActiveOperationIds(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Flow<Set<ExpressOperationId>> = flow {
        val addresses = getAddresses(userWalletId, currency.network)
        if (addresses.isEmpty()) {
            emit(emptySet())
            return@flow
        }
        val assetId = ExpressAsset.ID(currency)

        val exchangeIds = expressHistoryDao.observeActiveExchangeIds(
            addresses = addresses,
            network = assetId.networkId,
            contract = assetId.contractAddress,
            activeStatuses = ExpressStatusMapper.activeExchangeStatuses,
        ).map {
            it.mapTo(mutableSetOf()) { id -> ExpressOperationId(id, ExpressOperationType.Swap) }
        }
        val onrampIds = expressHistoryDao.observeActiveOnrampIds(
            addresses = addresses,
            network = assetId.networkId,
            contract = assetId.contractAddress,
            activeStatuses = ExpressStatusMapper.activeOnrampStatuses,
        ).map {
            it.mapTo(mutableSetOf()) { id -> ExpressOperationId(id, ExpressOperationType.Onramp) }
        }
        emitAll(combine(exchangeIds, onrampIds) { a, b -> a.plus(b) })
    }

    /**
     * Re-reads [activeId] from the backend and writes the answer back into the local database, so an operation
     * whose status moved on while the history sync was idle stops the screen from showing a stale one.
     *
     * Best-effort: a failed request is logged and dropped, since the caller polls and will ask again.
     */
    internal suspend fun refreshActiveStatuses(userWalletId: UserWalletId, activeId: ExpressOperationId) {
        val userWallet = getUserWalletUseCase(userWalletId).getOrNull() ?: return
        val walletIdValue = userWalletId.stringValue
        val refCode = expressUtils.getRefCode(userWallet = userWallet)

        when (activeId.type) {
            ExpressOperationType.Swap -> {
                val response = refreshExchangeStatus(
                    userWalletId = walletIdValue,
                    refCode = refCode,
                    txId = activeId.id,
                ) ?: return

                addRefundCurrencyIfNeeded(userWalletId = userWalletId, response = response)
            }
            ExpressOperationType.Onramp -> refreshOnrampStatus(
                userWalletId = walletIdValue,
                refCode = refCode,
                txId = activeId.id,
            )
        }
    }

    /**
     * Puts the token a DEX-bridge swap was refunded in into the portfolio, so the refund is visible without the
     * user opening the deal first.
     *
     * Only a DEX-bridge deal is refunded in an intermediate token the wallet does not hold — every other provider
     * refunds the token the user already has, and leaves the refund fields empty. The add is idempotent, and the
     * account is the one the deal was paid from.
     *
     * `NonCancellable` because `refunded` is a terminal status: the write that precedes this drops the operation out
     * of the polled set, which cancels the very job this runs in before the token would reach the portfolio.
     */
    private suspend fun addRefundCurrencyIfNeeded(userWalletId: UserWalletId, response: ExchangeItemResponse) {
        val refundNetwork = response.refundNetwork ?: return
        val refundContractAddress = response.refundContractAddress ?: return

        withContext(NonCancellable) {
            if (!isDexBridge(providerId = response.providerId)) return@withContext

            val accountId = findPayInAccountId(userWalletId = userWalletId, response = response)

            if (accountId == null) {
                TangemLogger.e("Unable to resolve the pay-in account of ${response.txId} for its refund token")
                return@withContext
            }

            manageCryptoCurrenciesUseCase.add(
                accountId = accountId,
                networkId = refundNetwork,
                contractAddress = refundContractAddress,
            ).onLeft { TangemLogger.e("Failed to add the refund token, txId=${response.txId}", it) }
        }
    }

    /**
     * The account the swap was paid from: the one holding a currency of the pay-in network at the pay-in address.
     * The refund token's own network is no help here — the wallet holds nothing on it yet, which is the whole point.
     */
    private suspend fun findPayInAccountId(userWalletId: UserWalletId, response: ExchangeItemResponse): AccountId? {
        val fromAddress = response.fromAddress ?: return null
        val accountStatuses = singleAccountStatusListSupplier.getSyncOrNull(userWalletId)?.accountStatuses
            ?: return null

        return accountStatuses
            .filterCryptoPortfolio()
            .firstOrNull { accountStatus ->
                accountStatus.flattenCurrencies().any { status ->
                    status.currency.network.rawId == response.fromNetwork &&
                        status.value.networkAddress.holdsAddress(fromAddress)
                }
            }
            ?.accountId
    }

    private suspend fun isDexBridge(providerId: String): Boolean {
        val provider = expressHistoryDao.getProvidersById().first()[providerId] ?: return false

        return expressProviderConverter.convert(provider).type == ExpressProviderType.DEX_BRIDGE
    }

    private suspend fun refreshExchangeStatus(
        userWalletId: String,
        refCode: String?,
        txId: String,
    ): ExchangeItemResponse? = runSuspendCatching {
        val response = exchangeApi.getExchangeStatus(
            userWalletId = userWalletId,
            refCode = refCode,
            txId = txId,
        ).getOrThrow()

        storeExchanges(items = listOf(response))
        response
    }
        .onFailure { TangemLogger.e("Failed to refresh exchange status, txId=$txId", it) }
        .getOrNull()

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

/**
 * Every address of the network, not just the default one — a chain with several address formats reports whichever
 * one the deal used. Compared case-insensitively: EVM addresses are derived checksummed, the backend is not.
 */
private fun NetworkAddress?.holdsAddress(address: String): Boolean =
    this?.availableAddresses.orEmpty().any { it.value.equals(address, ignoreCase = true) }