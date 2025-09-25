package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningEntryComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@ModelScoped
internal class YieldSupplyStartEarningEntryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model(), YieldSupplyStartEarningComponent.ModelCallback {

    private val params = paramsContainer.require<YieldSupplyStartEarningEntryComponent.Params>()

    override fun onBackClick() {
        router.pop()
    }

    override fun onFeePolicyClick() {
        router.push(YieldSupplyStartEarningRoute.FeePolicy)
    }

    override fun onTransactionSent() {
        params.onDismiss(true)
    }
}