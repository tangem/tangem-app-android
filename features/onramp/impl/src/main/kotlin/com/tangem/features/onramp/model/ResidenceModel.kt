package com.tangem.features.onramp.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.component.ResidenceComponent
import com.tangem.features.onramp.entity.ResidenceUM
import com.tangem.features.onramp.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ComponentScoped
internal class ResidenceModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ResidenceComponent.Params = paramsContainer.require()
    val state: MutableStateFlow<ResidenceUM> = MutableStateFlow(
        ResidenceUM(
            country = params.countryName,
            countryFlagUrl = params.countryFlagUrl,
            isCountrySupported = params.isOnrampSupported,
            primaryButtonConfig = getPrimaryButtonConfig(),
            secondaryButtonConfig = ResidenceUM.ActionButtonConfig(
                onClick = ::onChangeClick,
                text = resourceReference(R.string.common_change),
            ),
        ),
    )

    private fun getPrimaryButtonConfig() = if (params.isOnrampSupported) {
        ResidenceUM.ActionButtonConfig(
            onClick = params.onDismiss,
            text = resourceReference(R.string.common_confirm),
        )
    } else {
        ResidenceUM.ActionButtonConfig(
            onClick = ::onCloseClick,
            text = resourceReference(R.string.common_close),
        )
    }

    @Suppress("EmptyFunctionBlock")
    private fun onCloseClick() {
    }

    private fun onChangeClick() {
        // TODO: https://tangem.atlassian.net/browse/AND-8409
    }
}
