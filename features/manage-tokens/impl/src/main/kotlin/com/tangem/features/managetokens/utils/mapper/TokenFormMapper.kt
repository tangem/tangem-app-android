package com.tangem.features.managetokens.utils.mapper

import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM

internal fun CustomTokenFormUM.TokenFormUM?.mapToDomainModel(): AddCustomTokenForm.Raw {
    if (this == null) {
        return AddCustomTokenForm.Raw()
    }

    return AddCustomTokenForm.Raw(
        contractAddress = fields.getValue(CustomTokenFormUM.TokenFormUM.Field.CONTRACT_ADDRESS).value,
        symbol = fields.getValue(CustomTokenFormUM.TokenFormUM.Field.SYMBOL).value,
        name = fields.getValue(CustomTokenFormUM.TokenFormUM.Field.NAME).value,
        decimals = fields.getValue(CustomTokenFormUM.TokenFormUM.Field.DECIMALS).value,
    )
}