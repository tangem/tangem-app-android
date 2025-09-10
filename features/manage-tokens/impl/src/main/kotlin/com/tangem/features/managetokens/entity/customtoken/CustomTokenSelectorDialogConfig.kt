package com.tangem.features.managetokens.entity.customtoken

import com.tangem.features.managetokens.component.AddCustomTokenMode
import kotlinx.serialization.Serializable

@Serializable
internal sealed class CustomTokenSelectorDialogConfig {

    @Serializable
    data class CustomDerivationInput(
        val mode: AddCustomTokenMode,
    ) : CustomTokenSelectorDialogConfig()
}