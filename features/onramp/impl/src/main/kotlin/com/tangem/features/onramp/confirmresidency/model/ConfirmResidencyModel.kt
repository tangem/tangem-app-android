package com.tangem.features.onramp.confirmresidency.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.confirmresidency.ConfirmResidencyComponent
import com.tangem.features.onramp.confirmresidency.entity.ConfirmResidencyUM
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.selectcountry.entity.ConfirmResidencyBottomSheetConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@ComponentScoped
internal class ConfirmResidencyModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ConfirmResidencyComponent.Params = paramsContainer.require()
    val bottomSheetNavigation: SlotNavigation<ConfirmResidencyBottomSheetConfig> = SlotNavigation()
    val state: MutableStateFlow<ConfirmResidencyUM> = MutableStateFlow(
        ConfirmResidencyUM(
            country = params.countryName,
            countryFlagUrl = params.countryFlagUrl,
            isCountrySupported = params.isOnrampSupported,
            primaryButtonConfig = getPrimaryButtonConfig(),
            secondaryButtonConfig = ConfirmResidencyUM.ActionButtonConfig(
                onClick = ::onChangeClick,
                text = resourceReference(R.string.common_change),
            ),
        ),
    )

    private fun getPrimaryButtonConfig() = if (params.isOnrampSupported) {
        ConfirmResidencyUM.ActionButtonConfig(
            onClick = params.onDismiss,
            text = resourceReference(R.string.common_confirm),
        )
    } else {
        ConfirmResidencyUM.ActionButtonConfig(
            onClick = ::onCloseClick,
            text = resourceReference(R.string.common_close),
        )
    }

    @Suppress("EmptyFunctionBlock")
    private fun onCloseClick() {
    }

    private fun onChangeClick() {
        bottomSheetNavigation.activate(ConfirmResidencyBottomSheetConfig.SelectCountry)
    }
}
