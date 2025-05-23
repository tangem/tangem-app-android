package com.tangem.domain.walletconnect.usecase.method

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.walletconnect.usecase.blockaid.WcBlockAidEligibleTransactionUseCase

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
    WcBlockAidEligibleTransactionUseCase

/**
 * Base UseCase for wc methods with list of TransactionData
 *
 * ## Solana
 * solana_signAllTransactions https://docs.reown.com/advanced/multichain/rpc-reference/solana-rpc#solana-signalltransactions
 */
interface WcListTransactionUseCase :
    WcSignUseCase<List<TransactionData>>,
    WcBlockAidEligibleTransactionUseCase

/**
 * Wc Methods that support an updatable fee
 * [dAppFee] call to gee fee from dApp
 * [updateFee] call when user update the fee
 */
interface WcMutableFee {
    suspend fun dAppFee(): Fee?
    fun updateFee(fee: Fee)
}

/**
 * Wc Methods that support an updatable allowance
 * [updateFee] call when user update the fee
 */
interface WcMutableAllowance {
    fun updateAllowance(allowance: Any) // todo wc
}