package com.tangem.features.managetokens.utils.mapper

import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM

internal fun CustomTokenFormUM.TokenFormUM.mapToDomainModel(): AddCustomTokenForm.Raw {
    return AddCustomTokenForm.Raw(
        contractAddress = contractAddress.value,
        symbol = symbol.value,
        name = name.value,
        decimals = decimals.value,
    )
}