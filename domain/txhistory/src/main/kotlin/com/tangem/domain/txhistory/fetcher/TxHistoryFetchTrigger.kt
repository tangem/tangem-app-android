package com.tangem.domain.txhistory.fetcher

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

sealed interface TxHistoryFetchTrigger {

    data class TokenDetailsOpen(
        val walletId: UserWalletId,
        val currency: CryptoCurrency,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryJointOperationTrigger

    data class TokenDetailsPTR(
        val walletId: UserWalletId,
        val currency: CryptoCurrency,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryJointOperationTrigger

    data class WalletSelected(
        val walletId: UserWalletId,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryJointOperationTrigger

    /** A periodic refresh while a joint account's history is on screen */
    data class JointHistoryPoll(
        val walletId: UserWalletId,
    ) : TxHistoryFetchTrigger, TxHistoryJointOperationTrigger
}

sealed interface TxHistoryExpressTrigger : TxHistoryFetchTrigger
sealed interface TxHistoryJointOperationTrigger : TxHistoryFetchTrigger