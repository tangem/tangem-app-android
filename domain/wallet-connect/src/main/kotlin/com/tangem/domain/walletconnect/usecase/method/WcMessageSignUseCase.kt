package com.tangem.domain.walletconnect.usecase.method

import com.tangem.domain.walletconnect.usecase.blockaid.WcBlockAidEligibleTransactionUseCase

/**
 * UseCase for
 *
 * ## Ethereum
 * personal_sign https://docs.reown.com/advanced/multichain/rpc-reference/ethereum-rpc#personal-sign
 * eth_sign https://docs.reown.com/advanced/multichain/rpc-reference/ethereum-rpc#eth-sign
 * eth_signTypedData https://docs.reown.com/advanced/multichain/rpc-reference/ethereum-rpc#eth-signtypeddata
 *
 * ## Solana
 * solana_signMessage https://docs.reown.com/advanced/multichain/rpc-reference/solana-rpc#solana-signmessage
 */
interface WcMessageSignUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<WcMessageSignUseCase.SignModel>,
    WcBlockAidEligibleTransactionUseCase {

    data class SignModel(
        val humanMsg: String,
    )
}