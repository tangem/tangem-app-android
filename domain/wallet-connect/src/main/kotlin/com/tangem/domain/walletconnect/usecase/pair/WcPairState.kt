package com.tangem.domain.walletconnect.usecase.pair

import arrow.core.Either
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSessionProposal

sealed interface WcPairState {
    data object Loading : WcPairState
    data class Error(val throwable: Throwable) : WcPairState
    data class Proposal(
        val dAppSession: WcSessionProposal,
        val securityStatus: Any,
    ) : WcPairState

    sealed interface Approving : WcPairState {
        val session: WcSessionProposal

        data class Loading(override val session: WcSessionProposal) : Approving
        data class Result(
            override val session: WcSessionProposal,
            val result: Either<Throwable, WcSession>,
        ) : Approving
    }
}
