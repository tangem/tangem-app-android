package com.tangem.data.walletconnect.sign

import arrow.core.Either
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep

object SignStateConverter {

    internal fun <M> preSign(signModel: M) = WcSignState(signModel, WcSignStep.PreSign)
    internal fun <M> signing(signModel: M) = WcSignState(signModel, WcSignStep.Signing)
    internal fun <M> result(result: Either<Throwable, Unit>, signModel: M) =
        WcSignState(signModel, WcSignStep.Result(result))

    internal fun <M> WcSignState<M>.toPreSign(signModel: M = this.signModel) = copy(
        signModel = signModel,
        domainStep = WcSignStep.PreSign,
    )

    internal fun <M> WcSignState<M>.toSigning(signModel: M = this.signModel) = copy(
        domainStep = WcSignStep.Signing,
        signModel = signModel,
    )

    internal fun <M> WcSignState<M>.toResult(result: Either<Throwable, Unit>, signModel: M = this.signModel) = copy(
        domainStep = WcSignStep.Result(result),
        signModel = signModel,
    )
}