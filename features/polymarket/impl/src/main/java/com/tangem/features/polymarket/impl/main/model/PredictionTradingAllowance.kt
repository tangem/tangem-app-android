package com.tangem.features.polymarket.impl.main.model

import arrow.core.getOrElse
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

internal class PredictionTradingAllowance(
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    val isAllowed: StateFlow<Boolean>
        field = MutableStateFlow(value = false)

    suspend fun refresh() {
        val isBlocked = withContext(dispatchers.default) {
            checkPolymarketGeoblockUseCase().getOrElse { error ->
                TangemLogger.e("Predictions: region check failed, funding is left available: $error")
                false
            }
        }

        isAllowed.value = !isBlocked
    }
}