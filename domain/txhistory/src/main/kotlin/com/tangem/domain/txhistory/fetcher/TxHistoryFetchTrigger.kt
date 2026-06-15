package com.tangem.domain.txhistory.fetcher

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

sealed interface TxHistoryFetchTrigger {

    data class TokenDetailsOpen(
        val walletId: UserWalletId,
        val currency: CryptoCurrency,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryGatewayTrigger

    data class TokenDetailsPTR(
        val walletId: UserWalletId,
        val currency: CryptoCurrency,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryGatewayTrigger
}

sealed interface TxHistoryExpressTrigger : TxHistoryFetchTrigger
sealed interface TxHistoryGatewayTrigger : TxHistoryFetchTrigger