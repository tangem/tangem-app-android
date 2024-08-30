package com.tangem.features.managetokens.entity.customtoken

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal data class AddCustomTokenConfig(
    val step: Step,
    val popBack: () -> Unit,
    val userWalletId: UserWalletId,
    val selectedNetwork: SelectedNetwork? = null,
    val selectedDerivationPath: SelectedDerivationPath? = null,
    val formValues: CustomTokenFormValues = CustomTokenFormValues(),
) : TangemBottomSheetConfigContent {

    enum class Step {
        INITIAL_NETWORK_SELECTOR,
        NETWORK_SELECTOR,
        DERIVATION_PATH_SELECTOR,
        FORM,
    }
}

@Serializable
internal data class SelectedNetwork(
    val id: Network.ID,
    val name: TextReference,
    val derivationPath: Network.DerivationPath,
    val canHandleTokens: Boolean,
)

@Serializable
internal data class SelectedDerivationPath(
    val id: Network.ID?,
    val value: Network.DerivationPath,
    val networkName: TextReference,
)