package com.tangem.domain.exchange

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Manager that holds info about available actions as Sell and Buy
 */
interface RampStateManager {

    fun availableForBuy(scanResponse: ScanResponse, cryptoCurrency: CryptoCurrency): Boolean

    fun availableForSell(cryptoCurrency: CryptoCurrency): Boolean

    suspend fun availableForSwap(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean
}
