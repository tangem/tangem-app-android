package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.CustomTokenNetworkSelectorComponent
import com.tangem.features.managetokens.entity.AddCustomTokenButtonUM
import com.tangem.features.managetokens.entity.AddCustomTokenUM
import com.tangem.features.managetokens.entity.ClickableFieldUM
import com.tangem.features.managetokens.entity.SelectedNetworkUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.AddCustomTokenBottomSheet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class PreviewAddCustomTokenComponent(
    initialState: AddCustomTokenUM = AddCustomTokenUM.NetworkSelector(popBack = {}),
) : AddCustomTokenComponent {

    private val userWalletId = UserWalletId(stringValue = "321")

    private val previewState: MutableStateFlow<AddCustomTokenUM> = MutableStateFlow(initialState)

    @Composable
    override fun BottomSheet(isVisible: Boolean, onDismiss: () -> Unit) {
        val state by previewState.collectAsStateWithLifecycle()
        val config = TangemBottomSheetConfig(
            isShow = isVisible,
            onDismissRequest = onDismiss,
            content = state,
        )

        AddCustomTokenBottomSheet(
            config = config,
            content = {
                when (val s = state) {
                    is AddCustomTokenUM.Form -> {
                        PreviewCustomTokenFormComponent(
                            networkName = ClickableFieldUM(
                                label = resourceReference(R.string.custom_token_network_input_title),
                                value = stringReference(s.selectedNetwork.name),
                                onClick = { showNetworkSelector(s.selectedNetwork) },
                            ),
                        ).content(this)
                    }
                    is AddCustomTokenUM.NetworkSelector -> {
                        PreviewCustomTokenNetworkSelectorComponent(
                            params = CustomTokenNetworkSelectorComponent.Params(
                                userWalletId = userWalletId,
                                selectedNetwork = s.selectedNetwork,
                                onNetworkSelected = ::showForm,
                            ),
                            networksSize = 20,
                        ).content(this)
                    }
                }
            },
        )
    }

    private fun showNetworkSelector(selectedNetwork: SelectedNetworkUM) {
        previewState.update {
            AddCustomTokenUM.NetworkSelector(selectedNetwork, popBack = { showForm(selectedNetwork) })
        }
    }

    private fun showForm(network: SelectedNetworkUM) {
        previewState.update {
            AddCustomTokenUM.Form(
                popBack = {},
                selectedNetwork = network,
                addTokenButton = AddCustomTokenButtonUM.Visible(
                    isEnabled = false,
                    onClick = {},
                ),
            )
        }
    }
}
