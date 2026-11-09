package com.tangem.domain.collectibles.usecase

import com.tangem.domain.collectibles.models.CollectibleActiveTx
import com.tangem.domain.collectibles.models.CollectiblePendingPack
import com.tangem.domain.collectibles.models.CollectibleTxStatus
import com.tangem.domain.collectibles.repository.CollectiblesDismissedTxRepository
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * The user's pending packs in the backend's order, without the ones the user has hidden.
 *
 * Reads them once, and keeps re-reading while a payment is in progress, as only a payment changes on its own; a
 * pack waiting to be opened changes when the user opens it, and the screen asks again on return. A failed read
 * keeps the last answer.
 */
class GetCollectiblePendingPacksUseCase @Inject constructor(
    private val collectiblesRepository: CollectiblesRepository,
    private val dismissedTxRepository: CollectiblesDismissedTxRepository,
) {

    operator fun invoke(userWalletId: UserWalletId): Flow<List<CollectiblePendingPack>> = combine(
        flow = pollActiveTxs(userWalletId),
        flow2 = dismissedTxRepository.getDismissedTxIds(),
    ) { activeTxs, dismissedTxIds ->
        activeTxs
            .mapNotNull(::toPendingPack)
            .filterNot { pack -> pack.isDismissible && pack.txId in dismissedTxIds }
    }

    private fun pollActiveTxs(userWalletId: UserWalletId): Flow<List<CollectibleActiveTx>> = flow {
        var activeTxs = emptyList<CollectibleActiveTx>()

        while (true) {
            runSuspendCatching { collectiblesRepository.getActiveTxs(userWalletId) }
                .onSuccess { txs ->
                    activeTxs = txs
                    emit(txs)
                }
                .onFailure { TangemLogger.e("Unable to read the pending collectible packs", it) }

            if (activeTxs.none { tx -> tx.status.isPaymentInProgress }) break

            delay(POLL_INTERVAL)
        }
    }.distinctUntilChanged()

    private fun toPendingPack(tx: CollectibleActiveTx): CollectiblePendingPack? = when {
        tx.status.isPaymentInProgress -> CollectiblePendingPack.PaymentInProgress(txId = tx.txId)
        tx.status == CollectibleTxStatus.PACK_PURCHASED -> {
            tx.machineId?.let { machineId ->
                CollectiblePendingPack.ReadyToOpen(
                    txId = tx.txId,
                    machineId = machineId,
                    isTurboModeEnabled = tx.isTurboModeEnabled,
                )
            }
        }
        tx.status == CollectibleTxStatus.PACK_OPENING_ERROR -> CollectiblePendingPack.OpenFailed(txId = tx.txId)
        tx.status == CollectibleTxStatus.REFUNDING -> CollectiblePendingPack.Refunding(txId = tx.txId)
        else -> null
    }

    private companion object {
        val POLL_INTERVAL = 5.seconds
    }
}