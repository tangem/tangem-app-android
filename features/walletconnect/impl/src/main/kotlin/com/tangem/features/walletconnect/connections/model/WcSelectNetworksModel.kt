package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.getGreyedOutIconRes
import com.tangem.core.ui.extensions.iconResId
import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.connections.components.WcSelectNetworksComponent.WcSelectNetworksParams
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcSelectNetworksUM
import com.tangem.features.walletconnect.connections.model.transformers.WcSelectNetworksCheckedTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.toImmutableList
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
    private val additionallyEnabledNetworks = MutableStateFlow<Set<Network>>(
        filterAdditionallyEnabledNetworks(
            availableNetworks = params.availableNetworks,
            additionallyEnabledNetworks = params.enabledAvailableNetworks,
        ),
    )

    val state: StateFlow<WcSelectNetworksUM>
    field = MutableStateFlow(getInitialState())

    private fun onCheckedChange(isChecked: Boolean, network: Network) {
        additionallyEnabledNetworks.update {
            if (isChecked) it.plus(network) else it.minus(network)
        }
        state.update(WcSelectNetworksCheckedTransformer(network = network, isChecked = isChecked))
    }

    private fun onDone() {
        params.callback.onNetworksSelected(additionallyEnabledNetworks.value)
        router.pop()
    }

    private fun filterAdditionallyEnabledNetworks(
        availableNetworks: Set<Network>,
        additionallyEnabledNetworks: Set<Network>,
    ): Set<Network> {
        return availableNetworks.filterTo(hashSetOf()) { it in additionallyEnabledNetworks }
    }

    private fun getInitialState(): WcSelectNetworksUM {
        val editEnabled = params.missingRequiredNetworks.isEmpty()
        return WcSelectNetworksUM(
            missing = params.missingRequiredNetworks.map { network ->
                WcNetworkInfoItem.Required(
                    id = network.rawId,
                    icon = getGreyedOutIconRes(network.rawId),
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
            required = params.requiredNetworks.map { network ->
                WcNetworkInfoItem.Checked(
                    id = network.rawId,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                    enabled = editEnabled,
                )
            }.toImmutableList(),
            available = params.availableNetworks.map { network ->
                WcNetworkInfoItem.Checkable(
                    id = network.rawId,
                    icon = network.iconResId,
                    name = network.name,
                    symbol = network.currencySymbol,
                    checked = network in additionallyEnabledNetworks.value,
                    onCheckedChange = { onCheckedChange(it, network) },
                    enabled = editEnabled,
                )
            }.toImmutableList(),
            notAdded = params.notAddedNetworks.map { network ->
                WcNetworkInfoItem.ReadOnly(
                    id = network.rawId,
                    icon = getGreyedOutIconRes(network.rawId),
                    name = network.name,
                    symbol = network.currencySymbol,
                )
            }.toImmutableList(),
            onDone = ::onDone,
            doneButtonEnabled = editEnabled,
        )
    }
}