package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class PredictionFundingRequisites(
    private val refreshInteractor: RefreshPredictionDepositRequisitesInteractor,
    private val userWalletId: UserWalletId,
    private val scope: CoroutineScope,
) {

    val state: StateFlow<RequisitesState>
        field = MutableStateFlow<RequisitesState>(RequisitesState.Loading)

    val target: PolymarketDepositTarget?
        get() = (state.value as? RequisitesState.Ready)?.target

    private val prefetchJobHolder = JobHolder()

    fun prefetchOnce() {
        if (state.value is RequisitesState.Ready || prefetchJobHolder.isActive) return

        scope.launch { refresh() }.saveIn(prefetchJobHolder)
    }

    fun retryIfFailed() {
        if (state.value is RequisitesState.Failed) prefetchOnce()
    }

    suspend fun refresh() {
        state.value = refreshInteractor(userWalletId = userWalletId).fold(
            ifLeft = { RequisitesState.Failed },
            ifRight = { it.default?.let(RequisitesState::Ready) ?: RequisitesState.Failed },
        )
    }
}