package com.tangem.features.send.v2.feeselector.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.params.FeeSelectorParams
import com.tangem.features.send.v2.feeselector.route.FeeSelectorRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val feeSelectorLogicFactory: FeeSelectorLogic.Factory,
    private val sendFeatureToggles: SendFeatureToggles,
) : Model() {

    private val params = paramsContainer.require<FeeSelectorParams>()
    private val feeSelectorLogic = feeSelectorLogicFactory.create(
        params = params,
        modelScope = modelScope,
    )

    val uiState = feeSelectorLogic.uiState
    val intents: FeeSelectorIntents = feeSelectorLogic

    fun onChildBack() {
        // TODO("Not yet implemented")
    }

    fun getInitialRoute(): FeeSelectorRoute {
        if (sendFeatureToggles.isGaslessTransactionsEnabled.not()) {
            return FeeSelectorRoute.ChooseSpeed
        }

        // TODO: Add logic to choose initial route based on other conditions
        return FeeSelectorRoute.ChooseSpeed
    }
}