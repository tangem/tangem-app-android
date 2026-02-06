package com.tangem.features.feed.model.earn

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.features.feed.components.earn.DefaultEarnComponent
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.earn.state.EarnUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class EarnModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<DefaultEarnComponent.Params>()

    val state: StateFlow<EarnUM>
        field = MutableStateFlow(createInitialState())

    private fun createInitialState(): EarnUM = EarnUM(
        mostlyUsed = EarnListUM.Loading,
        bestOpportunities = EarnListUM.Loading,
        selectedNetworkFilter = null,
        selectedTypeFilter = null,
        networkFilters = persistentListOf(),
        typeFilters = persistentListOf(),
        onBackClick = params.onBackClick,
        onNetworkFilterClick = { },
        onTypeFilterClick = { },
    )
}