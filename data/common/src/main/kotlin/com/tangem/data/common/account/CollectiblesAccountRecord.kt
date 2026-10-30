package com.tangem.data.common.account

import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.DerivationIndex

/** The row the collectibles account keeps in the backend accounts document, the same on every write */
fun Account.Collectibles.toWalletAccountDTO(): WalletAccountDTO {
    return WalletAccountDTO(
        id = accountId.value,
        name = accountName.value,
        derivationIndex = DerivationIndex.Collectibles.value,
        icon = icon.value.name,
        iconColor = icon.color.name,
        type = WalletAccountDTO.Type.COLLECTIBLES,
    )
}