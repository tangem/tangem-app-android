package com.tangem.features.managetokens.utils.ui

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenConfig
import com.tangem.features.managetokens.entity.customtoken.AddCustomTokenUM
import com.tangem.features.managetokens.impl.R

internal fun AddCustomTokenConfig.Step.toContentModel(popBack: () -> Unit): AddCustomTokenUM = when (this) {
    AddCustomTokenConfig.Step.INITIAL_NETWORK_SELECTOR,
    AddCustomTokenConfig.Step.FORM,
    -> AddCustomTokenUM(
        popBack = popBack,
        title = resourceReference(R.string.add_custom_token_title),
        showBackButton = false,
    )
    AddCustomTokenConfig.Step.NETWORK_SELECTOR -> AddCustomTokenUM(
        popBack = popBack,
        title = resourceReference(R.string.custom_token_network_selector_title),
        showBackButton = true,
    )
    AddCustomTokenConfig.Step.DERIVATION_PATH_SELECTOR -> AddCustomTokenUM(
        popBack = popBack,
        title = resourceReference(R.string.custom_token_derivation_path),
        showBackButton = true,
    )
}