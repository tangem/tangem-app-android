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

    override fun onStopEarning() {
        router.push(YieldSupplyActiveRoute.Exit)
    }

    override fun onBackClick() {
        router.pop()
    }

    override fun onTransactionSent() {
        params.onDismiss()
    }

    override fun onApprove() {
        router.push(YieldSupplyActiveRoute.Approve)
    }

    override fun onReadMoreClick() {
        urlOpener.openUrl(TangemBlogUrlBuilder.YIELD_SUPPLY_HOW_IT_WORKS_URL)
    }
}