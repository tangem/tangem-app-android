package com.tangem.datasource.api.tangemTech.converters

import com.tangem.datasource.api.tangemTech.models.CardInfoBody
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.domain.models.wallet.UserWallet

object WalletIdBodyConverter {

    fun convert(userWallet: UserWallet, publicKeys: Map<String, String>? = null): WalletIdBody {
        return WalletIdBody(
            walletId = userWallet.walletId.stringValue,
            name = userWallet.name,
            walletType = WalletType.from(userWallet),
            cards = publicKeys?.map { publicKeyById ->
                CardInfoBody(
                    cardId = publicKeyById.key,
                    cardPublicKey = publicKeyById.value,
                )
            },
        )
    }
}