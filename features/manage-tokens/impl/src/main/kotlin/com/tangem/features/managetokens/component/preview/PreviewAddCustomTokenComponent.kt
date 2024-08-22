package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenConfig
import com.tangem.features.managetokens.ui.AddCustomTokenBottomSheet
import kotlinx.coroutines.flow.MutableStateFlow

internal class PreviewAddCustomTokenComponent(
    initialState: AddCustomTokenConfig = AddCustomTokenConfig(
        userWalletId = UserWalletId(stringValue = "321"),
        step = AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR,
        popBack = {},
    ),
) : AddCustomTokenComponent {

    private val previewState: MutableStateFlow<AddCustomTokenConfig> = MutableStateFlow(initialState)

    override fun dismiss() {
        /* no-op */
    }

    @Composable
    override fun BottomSheet() {
        val state by previewState.collectAsStateWithLifecycle()
        val config = TangemBottomSheetConfig(
            isShow = true,
            onDismissRequest = ::dismiss,
            content = state,
        )

        AddCustomTokenBottomSheet(
            config = config,
            content = { modifier ->
                when (state.step) {
                    AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR -> {
                        PreviewCustomTokenSelectorComponent(
                            params = CustomTokenSelectorComponent.Params.NetworkSelector(
                                userWalletId = state.userWalletId,
                                selectedNetwork = null,
                                onNetworkSelected = {},
                            ),
                        ).Content(modifier)
                    }
                    AddCustomTokenConfig.Step.NETWORK_SELECTOR -> {
                        PreviewCustomTokenSelectorComponent(
                            params = CustomTokenSelectorComponent.Params.NetworkSelector(
                                userWalletId = state.userWalletId,
                                selectedNetwork = state.selectedNetwork,
                                onNetworkSelected = {},
                            ),
                        ).Content(modifier)
                    }
                    AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR -> {
                        PreviewCustomTokenSelectorComponent(
                            params = CustomTokenSelectorComponent.Params.DerivationPathSelector(
                                userWalletId = state.userWalletId,
                                selectedDerivationPath = state.selectedDerivationPath,
                                onDerivationPathSelected = {},
                            ),
                        ).Content(modifier)
                    }
                    AddCustomTokenConfig.Step.FORM -> {
                        PreviewCustomTokenFormComponent().Content(modifier)
                    }
                }
            },
        )
    }
}