package com.tangem.features.managetokens.entity.customtoken

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal data class AddCustomTokenConfig(
    val step: Step,
    val userWalletId: UserWalletId,
    val selectedNetwork: SelectedNetwork? = null,
    val selectedDerivationPath: SelectedDerivationPath? = null,
    val formValues: CustomTokenFormValues = CustomTokenFormValues(),
) {

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
    val name: String,
    val derivationPath: Network.DerivationPath,
    val canHandleTokens: Boolean,
)

@Serializable
internal data class SelectedDerivationPath(
    val id: Network.ID?,
    val value: Network.DerivationPath,
    val name: String,
    val isDefault: Boolean,
)
