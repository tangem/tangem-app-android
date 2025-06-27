package com.tangem.features.welcome.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.welcome.impl.ui.state.WelcomeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class WelcomeModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<WelcomeUM>
    field = MutableStateFlow(WelcomeUM.Plain)
}