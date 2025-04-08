package com.tangem.domain.walletconnect.usecase

import arrow.core.Either
import kotlinx.coroutines.flow.Flow

interface WcSimpleSignUseCase<SignModel, MiddleAction> : WcUseCase {

    fun signFlow(initModel: SignModel): Flow<State<SignModel>>

    fun action(action: MiddleAction)

    fun cancel()
    fun sign(toSign: SignModel)

    sealed interface State<SignModel> {
        val model: SignModel

        data class PreSign<SignModel>(override val model: SignModel) : State<SignModel>
        data class Signing<SignModel>(override val model: SignModel) : State<SignModel>
        data class Result<SignModel>(
            val result: Either<Throwable, Unit>,
            override val model: SignModel,
        ) : State<SignModel>
    }
}