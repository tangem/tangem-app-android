package com.tangem.data.collectibles

import com.tangem.data.collectibles.converter.MachineCategoriesConverter
import com.tangem.data.collectibles.converter.MachineConverter
import com.tangem.data.collectibles.converter.MachineNftConverter
import com.tangem.data.collectibles.converter.TurboModeConverter
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
import com.tangem.grow.datasource.collectibles.models.request.GenerateTxRequestBody
import com.tangem.grow.datasource.collectibles.models.request.SubmitTxRequestBody
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultCollectiblesRepository @Inject constructor(
    private val collectiblesApi: CollectiblesApi,
    private val machineStore: CollectiblesMachineStore,
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

    override suspend fun generateTx(userWalletId: UserWalletId, params: GenerateTxParams): CollectibleTransaction =
        withContext(dispatchers.io) {
            val response = collectiblesApi.generateTx(
                userWalletId = userWalletId.stringValue,
                body = GenerateTxRequestBody(
                    fromAddress = params.fromAddress,
                    collectibleType = params.collectibleType,
                    toAddress = params.toAddress,
                    turboMode = params.collectibleTurboMode?.let(TurboModeConverter::convertBack),
                ),
            ).getOrThrow()

            CollectibleTransaction(id = response.id, txData = response.txData)
        }

    override suspend fun submitTx(userWalletId: UserWalletId, signedTxData: String): SubmittedCollectibleTransaction =
        withContext(dispatchers.io) {
            val response = collectiblesApi.submitTx(
                userWalletId = userWalletId.stringValue,
                body = SubmitTxRequestBody(signedTxData = signedTxData),
            ).getOrThrow()

            SubmittedCollectibleTransaction(id = response.id)
        }

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