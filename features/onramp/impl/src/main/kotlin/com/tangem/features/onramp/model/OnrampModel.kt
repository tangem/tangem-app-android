package com.tangem.features.onramp.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.onramp.component.OnrampComponent
import com.tangem.features.onramp.entity.OnrampBottomSheetConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("MagicNumber")
@ComponentScoped
internal class OnrampModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params: OnrampComponent.Params = paramsContainer.require()
    val bottomSheetNavigation: SlotNavigation<OnrampBottomSheetConfig> = SlotNavigation()

    init {
        modelScope.launch {
            delay(1500)
            bottomSheetNavigation.activate(OnrampBottomSheetConfig.ConfirmResidency)
        }
    }

    fun pop() {
        router.pop()
    }
}