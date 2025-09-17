package com.tangem.features.yield.supply.impl.subcomponents.active.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveComponent
import com.tangem.features.yield.supply.impl.subcomponents.active.YieldSupplyActiveEntryComponent
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyActiveEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
) : Model(), YieldSupplyActiveComponent.ModelCallback, YieldSupplyStopEarningComponent.ModelCallback {

    private val params = paramsContainer.require<YieldSupplyActiveEntryComponent.Params>()

    override fun onStopEarning() {
        router.push(YieldSupplyActiveRoute.Action)
    }

    override fun onBackClick() {
        router.pop()
    }

    override fun onTransactionSent() {
        params.onDismiss()
    }
}