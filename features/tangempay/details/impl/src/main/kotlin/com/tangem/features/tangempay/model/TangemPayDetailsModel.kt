package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig.ShowRefreshState
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.entity.TangemPayDetailsTopBarConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.features.tangempay.model.transformers.TangemPayDetailsRefreshTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayDetailsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
) : Model() {

    val uiState: StateFlow<TangemPayDetailsUM>
        field = MutableStateFlow(getInitialState())

    private val refreshStateJobHolder = JobHolder()

    @Suppress("MagicNumber", "UnusedPrivateMember")
    private fun onRefreshSwipe(refreshState: ShowRefreshState) {
        uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = true))
        modelScope.launch {
            // simulate update logic
            delay(2000)
            uiState.update(TangemPayDetailsRefreshTransformer(isRefreshing = false))
        }.saveIn(refreshStateJobHolder)
    }

    private fun getInitialState(): TangemPayDetailsUM {
        return TangemPayDetailsUM(
            topBarConfig = TangemPayDetailsTopBarConfig(onBackClick = router::pop, items = null),
            pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = ::onRefreshSwipe),
            balanceBlockState = TangemPayDetailsBalanceBlockState.Loading(actionButtons = persistentListOf()),
            cardDetailsUM = TangemPayCardDetailsUM(
                number = "•••• •••• •••• 1245",
                expiry = "••/••",
                cvv = "•••",
                onReveal = {},
            ),
            isBalanceHidden = false,
        )
    }
}