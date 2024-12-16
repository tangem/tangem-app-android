package com.tangem.features.onramp.confirmresidency.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.onramp.OnrampSaveDefaultCountryUseCase
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.features.onramp.confirmresidency.ConfirmResidencyComponent
import com.tangem.features.onramp.confirmresidency.entity.ConfirmResidencyBottomSheetConfig
import com.tangem.features.onramp.confirmresidency.entity.ConfirmResidencyUM
import com.tangem.features.onramp.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class ConfirmResidencyModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
    private val saveDefaultCountryUseCase: OnrampSaveDefaultCountryUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ConfirmResidencyComponent.Params = paramsContainer.require()
    val bottomSheetNavigation: SlotNavigation<ConfirmResidencyBottomSheetConfig> = SlotNavigation()
    val state: MutableStateFlow<ConfirmResidencyUM> = MutableStateFlow(
        ConfirmResidencyUM(
            country = params.country.name,
            countryFlagUrl = params.country.image,
            isCountrySupported = params.country.onrampAvailable,
            primaryButtonConfig = getPrimaryButtonConfig(),
            secondaryButtonConfig = ConfirmResidencyUM.ActionButtonConfig(
                onClick = ::onChangeClick,
                text = resourceReference(R.string.common_change),
            ),
        ),
    )

    init {
        analyticsEventHandler.send(OnrampAnalyticsEvent.ResidenceConfirmScreenOpened(params.country.name))
    }

    private fun getPrimaryButtonConfig() = if (params.country.onrampAvailable) {
        ConfirmResidencyUM.ActionButtonConfig(
            onClick = {
                analyticsEventHandler.send(OnrampAnalyticsEvent.OnResidenceConfirm(params.country.name))
                modelScope.launch { saveDefaultCountryUseCase.invoke(params.country) }
            },
            text = resourceReference(R.string.common_confirm),
        )
    } else {
        ConfirmResidencyUM.ActionButtonConfig(
            onClick = {
                analyticsEventHandler.send(OnrampAnalyticsEvent.CloseOnramp)
                router.pop()
            },
            text = resourceReference(R.string.common_close),
        )
    }

    private fun onChangeClick() {
        analyticsEventHandler.send(OnrampAnalyticsEvent.OnResidenceChange)
        bottomSheetNavigation.activate(ConfirmResidencyBottomSheetConfig.SelectCountry)
    }
}