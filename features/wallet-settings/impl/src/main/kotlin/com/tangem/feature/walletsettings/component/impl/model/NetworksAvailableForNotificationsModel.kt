package com.tangem.feature.walletsettings.component.impl.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.notifications.GetNetworksAvailableForNotificationsUseCase
import com.tangem.feature.walletsettings.ui.state.NetworksAvailableForNotificationsUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class NetworksAvailableForNotificationsModel @Inject constructor(
    private val getNetworksAvailableForNotificationsUseCase: GetNetworksAvailableForNotificationsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val state: StateFlow<NetworksAvailableForNotificationsUM> get() = _state

    private val _state = MutableStateFlow(
        value = NetworksAvailableForNotificationsUM(
            networks = persistentListOf(),
            isLoading = true,
        ),
    )

    init {
        modelScope.launch {
            getNetworksAvailableForNotificationsUseCase().onRight { networks ->
                _state.update {
                    it.copy(networks = networks.toImmutableList(), isLoading = false)
                }
            }
        }
    }
}