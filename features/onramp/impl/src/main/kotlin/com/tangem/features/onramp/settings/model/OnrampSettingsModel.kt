package com.tangem.features.onramp.settings.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onramp.settings.OnrampSettingsComponent
import com.tangem.features.onramp.settings.entity.OnrampSettingsConfig
import com.tangem.features.onramp.settings.entity.OnrampSettingsItemUM
import com.tangem.features.onramp.settings.entity.OnrampSettingsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@ComponentScoped
internal class OnrampSettingsModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: OnrampSettingsComponent.Params = paramsContainer.require()
    val bottomSheetNavigation: SlotNavigation<OnrampSettingsConfig> = SlotNavigation()
    val state = OnrampSettingsUM(
        onBack = params.onBack,
        items = listOf(
            OnrampSettingsItemUM.Residence(
                countryName = "United States of America",
                flagUrl = "",
                onClick = { bottomSheetNavigation.activate(OnrampSettingsConfig.SelectCountry) },
            ),
        ).toImmutableList(),
    )
}
