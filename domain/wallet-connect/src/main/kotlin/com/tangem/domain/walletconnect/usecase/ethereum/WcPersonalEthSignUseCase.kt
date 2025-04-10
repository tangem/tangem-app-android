package com.tangem.domain.walletconnect.usecase.ethereum

import com.tangem.domain.walletconnect.usecase.sign.WcSignUseCase

interface WcPersonalEthSignUseCase :
    WcSignUseCase,
    WcSignUseCase.FinalAction,
    WcSignUseCase.SimpleRun<WcPersonalEthSignUseCase.SignModel> {

    data class SignModel(
        val humanMsg: String,
        val account: String,
        val rawMsg: String,
    )
}