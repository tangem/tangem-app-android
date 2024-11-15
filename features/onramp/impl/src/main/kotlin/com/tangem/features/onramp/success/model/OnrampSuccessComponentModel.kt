package com.tangem.features.onramp.success.model

import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.onramp.component.OnrampSuccessComponent
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

internal class OnrampSuccessComponentModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    // todo onramp remove in https://tangem.atlassian.net/browse/AND-9100
    @Suppress("UnusedPrivateMember")
    private val params: OnrampSuccessComponent.Params = paramsContainer.require()
    private val _state: MutableStateFlow<OnrampSuccessComponentUM> = MutableStateFlow(
        value = OnrampSuccessComponentUM.Loading,
    )
    val state: StateFlow<OnrampSuccessComponentUM> get() = _state.asStateFlow()
}
