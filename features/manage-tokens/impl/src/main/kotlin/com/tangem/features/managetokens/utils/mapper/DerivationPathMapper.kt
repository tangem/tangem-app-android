package com.tangem.features.managetokens.utils.mapper

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.entity.customtoken.SelectedNetwork
import com.tangem.features.managetokens.entity.item.DerivationPathUM
import com.tangem.features.managetokens.impl.R

internal fun Network.toDerivationPathModel(
    isSelected: Boolean,
    onSelectedStateChange: (Boolean) -> Unit,
): DerivationPathUM? {
    return DerivationPathUM(
        id = rawId,
        value = derivationPath.value ?: return null,
        networkName = stringReference(name),
        isSelected = isSelected,
        onSelectedStateChange = onSelectedStateChange,
    )
}

internal fun SelectedNetwork.toDerivationPathModel(
    isSelected: Boolean,
    onSelectedStateChange: (Boolean) -> Unit,
): DerivationPathUM? {
    return DerivationPathUM(
        id = id.rawId.value,
        value = derivationPath.value ?: return null,
        networkName = resourceReference(R.string.custom_token_derivation_path_default),
        isSelected = isSelected,
        onSelectedStateChange = onSelectedStateChange,
    )
}