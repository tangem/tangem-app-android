package com.tangem.features.earn.lending.impl.session

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.fixFee
import javax.inject.Inject

/** Signs and sends every transaction of a plan at once */
internal class LendingTxSender @Inject constructor(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
) {

    /** @return the hashes in the order of [LendingPlan.txs] */
    suspend fun send(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        plan: LendingPlan,
    ): Either<SendTransactionError, List<String>> = either {
        val userWallet = getUserWalletUseCase(userWalletId).getOrElse { raise(SendTransactionError.UnknownError()) }
        val txs = ensureNotNull(withSelectedFee(plan, currency)) { SendTransactionError.UnknownError() }
        sendTransactionUseCase(
            txsData = txs,
            userWallet = userWallet,
            network = currency.network,
            sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
        ).bind()
    }

    private fun withSelectedFee(plan: LendingPlan, currency: CryptoCurrency): List<TransactionData.Uncompiled>? {
        val selected = plan.fee as? Fee.Ethereum ?: return null
        val gasLimits = plan.sentGasLimits() ?: return null
        return plan.txs.zip(gasLimits) { tx, gasLimit -> tx.copy(fee = selected.fixFee(currency, gasLimit)) }
    }
}