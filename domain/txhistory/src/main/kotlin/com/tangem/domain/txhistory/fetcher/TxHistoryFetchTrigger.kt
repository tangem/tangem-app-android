package com.tangem.domain.txhistory.fetcher

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.CoroutineScope

sealed interface TxHistoryFetchTrigger {

    val walletId: UserWalletId

    data class TokenDetailsOpen(
        override val walletId: UserWalletId,
        val currency: CryptoCurrency,
        val modelScope: CoroutineScope,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryJointOperationTrigger

    data class TokenDetailsPTR(
        override val walletId: UserWalletId,
        val currency: CryptoCurrency,
        val modelScope: CoroutineScope,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryJointOperationTrigger

    data class WalletSelected(
        override val walletId: UserWalletId,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger, TxHistoryJointOperationTrigger

    data class OnExpressOperationExecute(
        override val walletId: UserWalletId,
        val type: Operation,
    ) : TxHistoryFetchTrigger, TxHistoryExpressTrigger {
        sealed interface Operation
        data object Swap : Operation
        data object Onramp : Operation
    }

    /** A periodic refresh while a joint account's history is on screen */
    data class JointHistoryPoll(
        override val walletId: UserWalletId,
    ) : TxHistoryFetchTrigger, TxHistoryJointOperationTrigger
}

sealed interface TxHistoryExpressTrigger : TxHistoryFetchTrigger
sealed interface TxHistoryJointOperationTrigger : TxHistoryFetchTrigger