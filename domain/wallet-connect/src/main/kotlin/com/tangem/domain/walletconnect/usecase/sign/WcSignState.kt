package com.tangem.domain.walletconnect.usecase.sign

import arrow.core.Either

data class WcSignState<SignModel>(
    val signModel: SignModel,
    val domainStep: WcSignStep,
)

sealed interface WcSignStep {
    data object PreSign : WcSignStep
    data object Signing : WcSignStep
    data class Result(val result: Either<Throwable, Unit>) : WcSignStep
}