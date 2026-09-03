package com.tangem.data.collectibles

import com.tangem.data.collectibles.converter.MachineCategoriesConverter
import com.tangem.data.collectibles.converter.MachineConverter
import com.tangem.data.collectibles.converter.TurboModeConverter
import com.tangem.data.collectibles.pagination.CollectibleGachaMachinesBatchFetcher
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.domain.collectibles.model.CollectibleGachaMachinesBatchFlow
import com.tangem.domain.collectibles.model.CollectibleGachaMachinesBatchingContext
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
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesRepository {

    override suspend fun getCategories(): CollectibleGachaMachineCategories = withContext(dispatchers.io) {
        MachineCategoriesConverter.convert(collectiblesApi.getCategories().getOrThrow())
    }

    override suspend fun getMachine(externalId: String) = withContext(dispatchers.io) {
        MachineConverter.convert(collectiblesApi.getMachine(externalId).getOrThrow())
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

        return CollectibleGachaMachinesPage(
            collectibleGachaMachines = response.machines.map(MachineConverter::convert),
            cursor = response.cursor,
            hasNext = response.hasNext,
        )
    }
}