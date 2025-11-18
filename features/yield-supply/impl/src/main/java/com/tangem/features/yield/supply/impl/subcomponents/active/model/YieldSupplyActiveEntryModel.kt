package com.tangem.features.yield.supply.impl.subcomponents.active.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveEntryComponent
import com.tangem.features.yield.supply.impl.subcomponents.approve.YieldSupplyApproveComponent
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.utils.TangemBlogUrlBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyActiveEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
) : Model(), YieldSupplyActiveComponent.ModelCallback,
    YieldSupplyStopEarningComponent.ModelCallback,
    YieldSupplyApproveComponent.ModelCallback {

    private val params = paramsContainer.require<YieldSupplyActiveEntryComponent.Params>()

    val isTransactionInProgressFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    override fun onStopEarning() {
        router.push(YieldSupplyActiveRoute.Exit)
    }

    override fun onBackClick() {
        if (!isTransactionInProgressFlow.value) {
            router.pop()
        }
    }

    override fun onTransactionProgress(inProgress: Boolean) {
        isTransactionInProgressFlow.update { inProgress }
    }

    override fun onTransactionSent() {
        isTransactionInProgressFlow.update { false }
        params.onDismiss()
    }

    override fun onApprove() {
        router.push(YieldSupplyActiveRoute.Approve)
    }

    override fun onReadMoreClick() {
        urlOpener.openUrl(TangemBlogUrlBuilder.YIELD_SUPPLY_HOW_IT_WORKS_URL)
    }
}