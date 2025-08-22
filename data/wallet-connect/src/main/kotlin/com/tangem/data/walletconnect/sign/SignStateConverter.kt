package com.tangem.data.walletconnect.sign

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcSignStep

object SignStateConverter {

    internal fun <M> WcSignState<M>.toPreSign(signModel: M = this.signModel) = copy(
        signModel = signModel,
        domainStep = WcSignStep.PreSign,
    )

    internal fun <M> WcSignState<M>.toSigning(signModel: M = this.signModel) = copy(
        domainStep = WcSignStep.Signing,
        signModel = signModel,
    )

    internal fun <M> WcSignState<M>.toResult(result: Either<WcRequestError, String>, signModel: M = this.signModel) =
        copy(
            domainStep = WcSignStep.Result(result),
            signModel = signModel,
        )
}