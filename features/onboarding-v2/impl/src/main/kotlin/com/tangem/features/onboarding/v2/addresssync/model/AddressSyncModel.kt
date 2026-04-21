package com.tangem.features.onboarding.v2.addresssync.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.addresssync.entity.AddressSyncUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ModelScoped
internal class AddressSyncModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<AddressSyncUM>
        field: MutableStateFlow<AddressSyncUM> = MutableStateFlow(getInitialState())

    private fun getInitialState(): AddressSyncUM {
        TODO("Will be implemented during [REDACTED_TASK_KEY]")
    }
}