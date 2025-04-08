package com.tangem.domain.walletconnect.usecase.sign

import com.tangem.domain.walletconnect.usecase.WcMethodUseCase
import kotlinx.coroutines.flow.Flow

interface WcSignUseCase : WcMethodUseCase {

    interface FinalAction {
        fun cancel()
        fun sign()
    }

    interface SimpleRun<SignModel> {
        operator fun invoke(): Flow<WcSignState<SignModel>>
    }

    interface ArgsRun<SignModel, Args> {
        operator fun invoke(args: Args): Flow<WcSignState<SignModel>>
    }
}