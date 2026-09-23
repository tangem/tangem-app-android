package com.tangem.features.polymarket.impl.common.funding

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.model.PolymarketDepositTarget
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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

    private val jobHolder = JobHolder()

    fun prefetchOnce() {
        if (state.value is RequisitesState.Ready || jobHolder.isActive) return

        launchRefresh()
    }

    fun retryIfFailed() {
        if (state.value is RequisitesState.Failed) prefetchOnce()
    }

    suspend fun refresh() {
        launchRefresh().join()
    }

    private fun launchRefresh(): Job = scope.launch {
        refreshInteractor(userWalletId = userWalletId).fold(
            ifLeft = { state.update { current -> current as? RequisitesState.Ready ?: RequisitesState.Failed } },
            ifRight = { requisites ->
                state.value = requisites.default?.let(RequisitesState::Ready) ?: RequisitesState.Failed
            },
        )
    }.saveIn(jobHolder)
}