package com.tangem.domain.walletconnect.usecase.method

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.walletconnect.usecase.blockaid.WcBlockAidEligibleTransactionUseCase

/**
 * UseCase for
 * ## Ethereum
 * eth_sendTransaction  https://docs.reown.com/advanced/multichain/rpc-reference/ethereum-rpc#eth-sendtransaction
 * eth_signTransaction https://docs.reown.com/advanced/multichain/rpc-reference/ethereum-rpc#eth-signtransaction
 */
interface WcEthTransactionUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<WcEthTransaction>,
    WcBlockAidEligibleTransactionUseCase,
    WcMutableFee

data class WcEthTransaction(
    val dAppFee: Fee.Ethereum.Legacy?,
    val transactionData: TransactionData.Uncompiled,
)