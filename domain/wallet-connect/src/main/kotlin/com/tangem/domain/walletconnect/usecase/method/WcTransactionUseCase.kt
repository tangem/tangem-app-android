package com.tangem.domain.walletconnect.usecase.method

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.walletconnect.model.WcApprovedAmount

/**
 * Base UseCase for wc methods with single TransactionData
 *
 * ## Ethereum
 * eth_sendTransaction  https://docs.reown.com/advanced/multichain/rpc-reference/ethereum-rpc#eth-sendtransaction
 * eth_signTransaction https://docs.reown.com/advanced/multichain/rpc-reference/ethereum-rpc#eth-signtransaction
 *
 * ## Solana
 * solana_signTransaction https://docs.reown.com/advanced/multichain/rpc-reference/solana-rpc#solana-signtransaction
 */
interface WcTransactionUseCase :
    WcSignUseCase<TransactionData>,
    BlockAidTransactionCheck

/**
 * Base UseCase for wc methods with list of TransactionData
 *
 * ## Solana
 * solana_signAllTransactions https://docs.reown.com/advanced/multichain/rpc-reference/solana-rpc#solana-signalltransactions
 */
interface WcListTransactionUseCase :
    WcSignUseCase<List<TransactionData>>,
    BlockAidTransactionCheck

/**
 * Wc Methods that support an updatable fee
 * [dAppFee] call to gee fee from dApp
 * [updateFee] triggered a new [TransactionData] emit
 */
interface WcMutableFee {
    suspend fun dAppFee(): Fee?
    fun updateFee(fee: Fee)
}

/**
 * Wc Approval Method
 * If [WcApprovedAmount.amount] is null it means unlimited
 * [updateAmount] triggered a new [TransactionData] emit
 */
interface WcApproval {
    fun getAmount(): WcApprovedAmount?
    fun updateAmount(amount: WcApprovedAmount?)
}

/**
 * Defines if multiple signatures are required for the transaction
 */
interface SignRequirements {
    fun isMultipleSignRequired(): Boolean
}