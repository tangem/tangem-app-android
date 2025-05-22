package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.components.WcSelectNetworksComponent.WcSelectNetworksParams
import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.features.walletconnect.connections.model.transformers.WcSelectNetworksCheckedTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcSelectNetworksTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcSelectNetworksModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<WcSelectNetworksParams>()

    val state: StateFlow<WcSelectNetworksUM>
    field = MutableStateFlow(
        WcSelectNetworksUM(
            missing = persistentListOf(),
            required = persistentListOf(),
            available = persistentListOf(),
            notAdded = persistentListOf(),
            onDone = ::onDone,
        ),
    )

    private val additionallyEnabledNetworks = MutableStateFlow<Set<Network.RawID>>(setOf())

    init {
        state.update(
            WcSelectNetworksTransformer(
                missingRequiredNetworks = params.missingRequiredNetworks,
                requiredNetworks = params.requiredNetworks,
                availableNetworks = params.availableNetworks,
                notAddedNetworks = params.notAddedNetworks,
                enabledNetworks = params.enabledAvailableNetworks,
                onCheckedChange = ::onCheckedChange,
            ),
        )
    }

    private fun onCheckedChange(isChecked: Boolean, networkId: String) {
        additionallyEnabledNetworks.update {
            if (isChecked) it.plus(Network.RawID(networkId)) else it.minus(Network.RawID(networkId))
        }
        state.update(WcSelectNetworksCheckedTransformer(networkId, isChecked))
    }

    private fun onDone() {
        params.callback.onNetworksSelected(additionallyEnabledNetworks.value)
        router.pop()
    }
}