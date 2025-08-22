package com.tangem.data.wallets.converters

import com.tangem.datasource.api.tangemTech.models.CardInfoBody
import com.tangem.datasource.api.tangemTech.models.WalletIdBody
import com.tangem.domain.models.wallet.UserWallet

internal object WalletIdBodyConverter {

    fun convert(userWallet: UserWallet, publicKeys: Map<String, String>): WalletIdBody {
        return WalletIdBody(
            walletId = userWallet.walletId.stringValue,
            name = userWallet.name,
            cards = publicKeys.map {
                CardInfoBody(
                    cardId = it.key,
                    cardPublicKey = it.value,
                )
            },
        )
    }
}