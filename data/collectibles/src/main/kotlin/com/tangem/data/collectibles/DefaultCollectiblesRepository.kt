package com.tangem.data.collectibles

import com.tangem.data.collectibles.converter.MachineCategoriesConverter
import com.tangem.data.collectibles.converter.MachineConverter
import com.tangem.data.collectibles.converter.MachineNftConverter
import com.tangem.data.collectibles.converter.CollectibleTxStatusConverter
import com.tangem.data.collectibles.converter.OpenedPackConverter
import com.tangem.data.collectibles.converter.TurboModeConverter
import com.tangem.data.collectibles.error.CollectiblesErrorResolver
import com.tangem.data.collectibles.pagination.CollectibleGachaMachineNftsBatchFetcher
import com.tangem.data.collectibles.pagination.CollectibleGachaMachinesBatchFetcher
import com.tangem.data.collectibles.store.CollectiblesMachineStore
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachineNftsBatchingContext
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.CollectibleGachaMachinesBatchingContext
import com.tangem.domain.collectibles.models.*
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.grow.datasource.collectibles.CollectiblesApi
import com.tangem.grow.datasource.collectibles.models.request.CollectibleTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.GeneratePurchaseTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.PurchaseTxSentRequestBody
import com.tangem.grow.datasource.collectibles.models.response.CollectibleTxResponse
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultCollectiblesRepository @Inject constructor(
    private val collectiblesApi: CollectiblesApi,
    private val machineStore: CollectiblesMachineStore,
    private val errorResolver: CollectiblesErrorResolver,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesRepository {

    override suspend fun getCategories(): CollectibleGachaMachineCategories = withContext(dispatchers.io) {
        MachineCategoriesConverter.convert(collectiblesApi.getCategories().getOrThrow())
    }

    override suspend fun getMachine(externalId: String, skipCache: Boolean) = withContext(dispatchers.io) {
        val cached = if (skipCache) null else machineStore.getSyncOrNull(externalId)

        cached ?: MachineConverter.convert(collectiblesApi.getMachine(externalId).getOrThrow())
            .also { machineStore.store(machines = listOf(it)) }
    }

    override fun getMachinesBatchFlow(
        context: CollectibleGachaMachinesBatchingContext,
        batchSize: Int,
    ): CollectibleGachaMachinesBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = CollectibleGachaMachinesBatchFetcher(batchSize = batchSize, fetchPage = ::fetchMachinesPage),
        ).toBatchFlow()
    }

    override fun getMachineNftsBatchFlow(
        context: CollectibleGachaMachineNftsBatchingContext,
        batchSize: Int,
        userWalletId: UserWalletId,
    ): CollectibleGachaMachineNftsBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = CollectibleGachaMachineNftsBatchFetcher(
                batchSize = batchSize,
                fetchPage = { config, page, limit ->
                    fetchMachineNftsPage(
                        config = config,
                        page = page,
                        limit = limit,
                        userWalletId = userWalletId,
                    )
                },
            ),
        ).toBatchFlow()
    }

    override suspend fun generatePurchaseTx(
        userWalletId: UserWalletId,
        params: GeneratePurchaseTxParams,
    ): CollectiblePurchaseTx = withContext(dispatchers.io) {
        val response = errorResolver.getOrThrow(
            collectiblesApi.generatePurchaseTx(
                userWalletId = userWalletId.stringValue,
                body = GeneratePurchaseTxRequestBody(
                    fromAddress = params.fromAddress,
                    machineId = params.machineId,
                    toAddress = params.toAddress,
                    turboMode = params.collectibleTurboMode?.let(TurboModeConverter::convertBack),
                ),
            ),
        )

        CollectiblePurchaseTx(
            txId = response.txId,
            memo = response.txData.memo,
            amount = response.txData.fromAmount.toLong(),
            feeAmount = response.txData.feeAmount.toLong(),
        )
    }

    override suspend fun reportPurchaseTxSent(
        userWalletId: UserWalletId,
        txId: String,
        txHash: String,
    ): CollectibleTxStatus = withContext(dispatchers.io) {
        val response = collectiblesApi.purchaseTxSent(
            userWalletId = userWalletId.stringValue,
            body = PurchaseTxSentRequestBody(txId = txId, txHash = txHash),
        ).getOrThrow()

        CollectibleTxStatusConverter.convert(response.status)
    }

    override suspend fun getTxState(
        userWalletId: UserWalletId,
        txId: String,
        includeNftWon: Boolean,
    ): CollectibleTxState = withContext(dispatchers.io) {
        collectiblesApi.getTxStatus(
            userWalletId = userWalletId.stringValue,
            txId = txId,
            includeNftWon = includeNftWon,
        )
            .getOrThrow()
            .toState()
    }

    override suspend fun openPack(userWalletId: UserWalletId, txId: String): CollectibleTxState =
        withContext(dispatchers.io) {
            collectiblesApi.openPack(
                userWalletId = userWalletId.stringValue,
                body = CollectibleTxRequestBody(txId = txId),
                includeNftWon = true,
            ).getOrThrow().toState()
        }

    private fun CollectibleTxResponse.toState() = CollectibleTxState(
        status = CollectibleTxStatusConverter.convert(status),
        pack = nftWon?.let { nft -> OpenedPackConverter.convert(OpenedPackConverter.Input(txId = txId, nft = nft)) },
    )

    private suspend fun fetchMachinesPage(
        config: CollectibleGachaMachinesListConfig,
        cursor: String?,
        limit: Int,
    ): CollectibleGachaMachinesPage {
        val response = collectiblesApi.getMachines(
            categoryId = config.categoryId,
            search = config.search,
            limit = limit,
            cursor = cursor,
        ).getOrThrow()

        val machines = response.machines.map(MachineConverter::convert)
        machineStore.store(machines = machines)

        return CollectibleGachaMachinesPage(
            collectibleGachaMachines = machines,
            cursor = response.cursor,
            hasNext = response.hasNext,
        )
    }

    private suspend fun fetchMachineNftsPage(
        config: CollectibleGachaMachineNftsListConfig,
        page: Int,
        limit: Int,
        userWalletId: UserWalletId,
    ): CollectibleGachaMachineNftsPage {
        val response = collectiblesApi.getMachineNfts(
            userWalletId = userWalletId.stringValue,
            externalId = config.externalId,
            page = page,
            limit = limit,
        ).getOrThrow()

        return CollectibleGachaMachineNftsPage(
            nfts = response.nfts.map(MachineNftConverter::convert),
            page = response.page,
            hasMore = response.hasMore,
        )
    }
}