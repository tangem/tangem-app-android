package com.tangem.domain.walletconnect.usecase.sign

import arrow.core.Either
import com.tangem.domain.walletconnect.usecase.WcMethodUseCase
import kotlinx.coroutines.flow.Flow

interface WcSignUseCase : WcMethodUseCase {

    fun cancel()
    fun sign()

    interface SimpleRun<SignModel> {
        operator fun invoke(): Flow<WcSignState<SignModel>>
    }

    interface ArgsRun<SignModel, Args> {
        operator fun invoke(args: Args): Flow<WcSignState<SignModel>>
    }
}

data class WcSignState<SignModel>(
    val signModel: SignModel,
    val domainStep: WcSignStep,
)

sealed interface WcSignStep {
    data object PreSign : WcSignStep
    data object Signing : WcSignStep
    data class Result(val result: Either<Throwable, Unit>) : WcSignStep
}