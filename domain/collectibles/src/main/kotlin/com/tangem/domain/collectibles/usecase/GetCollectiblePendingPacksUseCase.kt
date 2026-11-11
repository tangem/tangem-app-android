package com.tangem.domain.collectibles.usecase

import com.tangem.domain.collectibles.converter.CollectiblePendingPackConverter
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsFetcher
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsSupplier
import com.tangem.domain.collectibles.models.CollectiblePendingPack
import com.tangem.domain.collectibles.repository.CollectiblesDismissedTxRepository
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * The user's pending packs in the backend's order, without the ones the user has hidden.
 *
 * Shows the last read at once and reads again on every collection; while a payment is in progress, it reads every
 * 5 seconds, as only a payment changes on its own. A pack opened meanwhile is re-read by the opening itself.
 */
class GetCollectiblePendingPacksUseCase @Inject constructor(
    private val activeTxsSupplier: CollectiblesActiveTxsSupplier,
    private val activeTxsFetcher: CollectiblesActiveTxsFetcher,
    private val dismissedTxRepository: CollectiblesDismissedTxRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<CollectiblePendingPack>> = channelFlow {
        launch { fetchWhilePaying(userWalletId) }

        combine(
            flow = activeTxsSupplier(userWalletId),
            flow2 = dismissedTxRepository.getDismissedTxIds(),
        ) { activeTxs, dismissedTxIds ->
            activeTxs
                .mapNotNull(CollectiblePendingPackConverter::convert)
                .filterNot { pack -> pack.isDismissible && pack.txId in dismissedTxIds }
        }
            .collect(::send)
    }.distinctUntilChanged()

    private suspend fun fetchWhilePaying(userWalletId: UserWalletId) {
        val params = CollectiblesActiveTxsFetcher.Params(userWalletId)
        activeTxsFetcher(params)

        activeTxsSupplier(userWalletId).collectLatest { activeTxs ->
            while (activeTxs.any { tx -> tx.status.isPaymentInProgress }) {
                delay(POLL_INTERVAL)
                activeTxsFetcher(params)
            }
        }
    }

    private companion object {
        val POLL_INTERVAL = 5.seconds
    }
}