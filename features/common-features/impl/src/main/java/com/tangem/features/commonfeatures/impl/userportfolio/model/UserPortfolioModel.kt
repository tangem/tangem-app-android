package com.tangem.features.commonfeatures.impl.userportfolio.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.commonfeatures.impl.userportfolio.UserPortfolioComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class UserPortfolioModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<UserPortfolioComponent.Params>()

    val state: StateFlow<UserPortfolioUM?> = params.uiState
}