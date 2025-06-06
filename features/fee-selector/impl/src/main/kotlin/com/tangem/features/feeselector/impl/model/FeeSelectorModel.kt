package com.tangem.features.feeselector.impl.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.features.feeselector.api.component.FeeSelectorComponent
import com.tangem.features.feeselector.impl.entity.FeeSelectorUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class FeeSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<FeeSelectorComponent.Params>()
    val uiState: StateFlow<FeeSelectorUM>
    field = MutableStateFlow<FeeSelectorUM>(FeeSelectorUM.Loading)

    fun dismiss() {
        router.pop()
    }
}