package com.tangem.features.jointaccount.supportednetworks.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.jointaccount.usecase.GetJointAccountSupportedNetworksUseCase
import com.tangem.features.jointaccount.supportednetworks.converter.SupportedNetworkItemConverter
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@Stable
@ModelScoped
internal class JointSupportedNetworksModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    getSupportedNetworks: GetJointAccountSupportedNetworksUseCase,
) : Model() {

    val uiState: StateFlow<JointSupportedNetworksUM>
        field = MutableStateFlow(
            JointSupportedNetworksUM(
                networks = SupportedNetworkItemConverter.convert(getSupportedNetworks()).toImmutableList(),
                onDismiss = ::onDismiss,
                onGotItClick = ::onDismiss,
            ),
        )

    private fun onDismiss() {
        router.pop()
    }
}