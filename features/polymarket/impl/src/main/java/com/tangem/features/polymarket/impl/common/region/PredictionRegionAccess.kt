package com.tangem.features.polymarket.impl.common.region

import arrow.core.getOrElse
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PredictionRegionAccess @Inject constructor(
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    val status: StateFlow<PredictionRegionStatus>
        field = MutableStateFlow(value = PredictionRegionStatus.UNKNOWN)

    val isRestricted: Boolean
        get() = status.value == PredictionRegionStatus.RESTRICTED

    private val latestRequest = AtomicInteger()

    suspend fun refresh() {
        val request = latestRequest.incrementAndGet()
        val isBlocked = withContext(dispatchers.default) {
            checkPolymarketGeoblockUseCase().getOrElse { error ->
                TangemLogger.e("Predictions: region check failed, trading and funding are left available: $error")
                false
            }
        }

        if (request != latestRequest.get()) return

        status.value = if (isBlocked) PredictionRegionStatus.RESTRICTED else PredictionRegionStatus.ALLOWED
    }
}

internal enum class PredictionRegionStatus {
    UNKNOWN,
    ALLOWED,
    RESTRICTED,
}