package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.domain.pay.repository.CardDetailsRepository
import com.tangem.features.tangempay.components.TangemPayDetailsComponent
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.transformers.TangemPayDetailsRefreshTransformer
import com.tangem.features.tangempay.transformer.CardDetailsStateTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val cardDetailsRepository: CardDetailsRepository,
) : Model() {

    private val params: TangemPayDetailsComponent.Params = paramsContainer.require()

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(getInitialState())

    private val refreshStateJobHolder = JobHolder()
    private val fetchBalanceJobHolder = JobHolder()

    init {
        fetchBalance()
    }

    private fun fetchBalance(): Job {
        return modelScope.launch {
            val result = cardDetailsRepository.getCardBalance()
            uiState.update(CardDetailsStateTransformer(result))
        }.saveIn(fetchBalanceJobHolder)
    }

    private fun onRefreshSwipe(refreshState: ShowRefreshState) {
        modelScope.launch {
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = refreshState.value))
            fetchBalance().join()
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    private fun getInitialState(): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = router::pop, items = null),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = ::onRefreshSwipe),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(actionButtons = persistentListOf()),
            cardDetailsUM = getHiddenCardDetails(),
            isBalanceHidden = false,
        )
    }

    private fun getHiddenCardDetails() = TangemPayCardDetailsUM(
        number = "•••• •••• •••• ${params.cardNumberEnd}",
        expiry = "••/••",
        cvv = "•••",
        onReveal = ::revealCardDetails,
    )

    private fun revealCardDetails() {
        // TODO "[REDACTED_TASK_KEY] add reveal card details"
    }
}