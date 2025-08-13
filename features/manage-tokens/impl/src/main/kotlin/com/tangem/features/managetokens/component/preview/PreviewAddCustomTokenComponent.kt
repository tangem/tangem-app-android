package com.tangem.features.managetokens.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.managetokens.component.AddCustomTokenComponent
import com.tangem.features.managetokens.component.CustomTokenSelectorComponent
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenConfig
import com.tangem.features.managetokens.ui.AddCustomTokenBottomSheet
import com.tangem.features.managetokens.utils.ui.toContentModel
import kotlinx.coroutines.flow.MutableStateFlow

internal class PreviewAddCustomTokenComponent(
    initialState: AddCustomTokenConfig = AddCustomTokenConfig(
        userWalletId = UserWalletId(stringValue = "321"),
        step = AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR,
    ),
) : AddCustomTokenComponent {

    private val previewConfig: MutableStateFlow<AddCustomTokenConfig> = MutableStateFlow(initialState)

    override fun dismiss() {
        /* no-op */
    }

    @Composable
    override fun BottomSheet() {
        val config by previewConfig.collectAsStateWithLifecycle()
        val bottomSheetConfig = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = ::dismiss,
            content = config.step.toContentModel(::dismiss),
        )

        AddCustomTokenBottomSheet(
            config = bottomSheetConfig,
            content = { modifier ->
                when (config.step) {
                    AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR -> {
                        PreviewCustomTokenSelectorComponent(
                            params = CustomTokenSelectorComponent.Params.NetworkSelector(
                                userWalletId = config.userWalletId,
                                selectedNetwork = null,
                                onNetworkSelected = {},
                            ),
                        ).Content(modifier)
                    }
                    AddCustomTokenConfig.Step.NETWORK_SELECTOR -> {
                        PreviewCustomTokenSelectorComponent(
                            params = CustomTokenSelectorComponent.Params.NetworkSelector(
                                userWalletId = config.userWalletId,
                                selectedNetwork = config.selectedNetwork,
                                onNetworkSelected = {},
                            ),
                        ).Content(modifier)
                    }
                    AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR -> {
                        PreviewCustomTokenSelectorComponent(
                            params = CustomTokenSelectorComponent.Params.DerivationPathSelector(
                                userWalletId = config.userWalletId,
                                selectedNetwork = config.selectedNetwork!!,
                                selectedDerivationPath = config.selectedDerivationPath!!,
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