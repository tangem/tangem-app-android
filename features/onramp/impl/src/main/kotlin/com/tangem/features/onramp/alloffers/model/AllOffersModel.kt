package com.tangem.features.onramp.alloffers.model

import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onramp.alloffers.AllOffersComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

internal class AllOffersModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: AllOffersComponent.Params = paramsContainer.require()

    fun dismiss() {
        params.onDismiss()
    }
}