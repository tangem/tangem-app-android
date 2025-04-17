package com.tangem.domain.walletconnect.usecase.ethereum

import com.tangem.domain.walletconnect.usecase.sign.WcSignUseCase

interface WcEthMessageSignUseCase :
    WcSignUseCase,
    WcSignUseCase.SimpleRun<WcEthMessageSignUseCase.SignModel> {

    data class SignModel(
        val humanMsg: String,
        val account: String,
        val rawMsg: String,
    )
}