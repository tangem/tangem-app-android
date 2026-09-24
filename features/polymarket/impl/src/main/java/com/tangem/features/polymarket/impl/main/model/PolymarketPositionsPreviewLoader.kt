package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.flow.PredictionPositionsUpdateListener
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketPositionsPreviewUM
import com.tangem.features.polymarket.impl.positions.model.PolymarketPositionUMConverter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

internal class PolymarketPositionsPreviewLoader @Inject constructor(
    private val repository: PolymarketPositionsRepository,
    private val deriveAddresses: DerivePolymarketAddressesUseCase,
    private val updateListener: PredictionPositionsUpdateListener,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    val state: StateFlow<PolymarketPositionsPreviewUM>
        field = MutableStateFlow<PolymarketPositionsPreviewUM>(PolymarketPositionsPreviewUM.Loading)

    private val refreshJob = JobHolder()
    private val invalidationJob = JobHolder()
    private var isResumed = false
    private var appliedRevision = 0L
    private var indexingRetry = INDEXING_RETRY_INTERVALS.size

    fun resume(
        scope: CoroutineScope,
        userWalletId: UserWalletId,
        onPositionClick: ((PolymarketPosition.Open) -> Unit)? = null,
    ) {
        isResumed = true
        val converter = PolymarketPositionUMConverter(onPositionClick)
        val onRetry = { if (isResumed) resume(scope, userWalletId, onPositionClick) }
        applyRevision(updateListener.currentRevision(userWalletId))
        startPolling(scope = scope, userWalletId = userWalletId, onRetry = onRetry, converter = converter)
        scope.launch {
            updateListener.revisionFlow(userWalletId).collect { revision ->
                if (revision == appliedRevision) return@collect
                applyRevision(revision)
                startPolling(scope = scope, userWalletId = userWalletId, onRetry = onRetry, converter = converter)
            }
        }.saveIn(invalidationJob)
    }

    suspend fun refresh(
        scope: CoroutineScope,
        userWalletId: UserWalletId,
        onPositionClick: ((PolymarketPosition.Open) -> Unit)? = null,
    ) {
        if (!isResumed) return
        val onRetry = { if (isResumed) resume(scope, userWalletId, onPositionClick) }
        startPolling(
            scope = scope,
            userWalletId = userWalletId,
            onRetry = onRetry,
            converter = PolymarketPositionUMConverter(onPositionClick),
        ).await()
    }

    private fun applyRevision(revision: Long) {
        if (revision == appliedRevision) return
        appliedRevision = revision
        indexingRetry = 0
    }

    private fun startPolling(
        scope: CoroutineScope,
        userWalletId: UserWalletId,
        onRetry: () -> Unit,
        converter: PolymarketPositionUMConverter,
    ): Deferred<Unit> {
        val firstRead = CompletableDeferred<Unit>()
        scope.launch {
            if (state.value !is PolymarketPositionsPreviewUM.Content) {
                state.value = PolymarketPositionsPreviewUM.Loading
            }
            while (true) {
                refresh(userWalletId, onRetry, converter)
                firstRead.complete(Unit)
                val indexingDelay = INDEXING_RETRY_INTERVALS.getOrNull(indexingRetry)
                if (indexingDelay != null) indexingRetry++
                delay(indexingDelay ?: POLL_INTERVAL)
            }
        }.saveIn(refreshJob).invokeOnCompletion { firstRead.cancel() }
        return firstRead
    }

    fun pause() {
        isResumed = false
        refreshJob.cancel()
        invalidationJob.cancel()
    }

    private suspend fun refresh(
        userWalletId: UserWalletId,
        onRetry: () -> Unit,
        converter: PolymarketPositionUMConverter,
    ) {
        val result = withContext(dispatchers.default) {
            val addresses = deriveAddresses.stored(userWalletId) ?: return@withContext null
            repository.getOpenPositions(addresses = addresses, limit = PREVIEW_SIZE, offset = 0)
        }
        currentCoroutineContext().ensureActive()
        if (result == null) {
            showError(reason = "Stored addresses unavailable", onRetry = onRetry)
            return
        }
        result.fold(
            ifLeft = { showError(reason = it.toString(), onRetry = onRetry) },
            ifRight = { page ->
                state.value = PolymarketPositionsPreviewUM.Content(
                    positions = page.positions.take(PREVIEW_SIZE).map(converter::convert).toImmutableList(),
                )
            },
        )
    }

    private fun showError(reason: String, onRetry: () -> Unit) {
        TangemLogger.e("Failed to refresh Polymarket positions preview: $reason")
        val previous = state.value
        state.value = when (previous) {
            is PolymarketPositionsPreviewUM.Content -> previous.copy(onRetry = onRetry)
            else -> PolymarketPositionsPreviewUM.Error(onRetry)
        }
    }

    private companion object {
        const val PREVIEW_SIZE = 3
        val POLL_INTERVAL = 20.seconds
        // Successful reads can still return the pre-trade list while the backend indexes a fill.
        val INDEXING_RETRY_INTERVALS = listOf(2.seconds, 4.seconds, 8.seconds, 16.seconds)
    }
}