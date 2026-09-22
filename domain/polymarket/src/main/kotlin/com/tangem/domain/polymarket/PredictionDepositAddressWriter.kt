package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWalletId

interface PredictionDepositAddressWriter {

    /**
     * @param address the confirmed EVM deposit address; `null` withdraws it, which takes the collateral out of
     *  both the receive sheet and the swap target list
     */
    suspend fun store(userWalletId: UserWalletId, address: String?)
}