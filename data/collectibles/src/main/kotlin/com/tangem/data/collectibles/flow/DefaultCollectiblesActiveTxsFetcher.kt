package com.tangem.data.collectibles.flow

import arrow.core.Either
import com.tangem.data.collectibles.converter.CollectibleActiveTxConverter
import com.tangem.data.collectibles.store.CollectiblesActiveTxsStore
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsFetcher
import com.tangem.domain.core.utils.catchOn
import com.tangem.grow.datasource.collectibles.CollectiblesApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

internal class DefaultCollectiblesActiveTxsFetcher @Inject constructor(
    private val collectiblesApi: CollectiblesApi,
    private val activeTxsStore: CollectiblesActiveTxsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesActiveTxsFetcher {

    override suspend fun invoke(params: CollectiblesActiveTxsFetcher.Params): Either<Throwable, Unit> {
        return Either.catchOn(dispatchers.io) {
            val activeTxs = collectiblesApi.getActiveTxs(
                userWalletId = params.userWalletId.stringValue,
                includeMachine = true,
            )
                .getOrThrow()
                .activeTxs
                .map(CollectibleActiveTxConverter::convert)

            activeTxsStore.store(userWalletId = params.userWalletId, activeTxs = activeTxs)
        }
            .onLeft { TangemLogger.e("Unable to read the active collectible purchases", it) }
    }
}