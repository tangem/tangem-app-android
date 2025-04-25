package com.tangem.domain.walletconnect.usecase.pair

import arrow.core.Either
import com.tangem.domain.walletconnect.model.*
import kotlinx.coroutines.flow.Flow

interface WcPairUseCase {

    operator fun invoke(): Flow<WcPairState>

    fun approve(sessionForApprove: WcSessionApprove)
    fun reject()

    interface Factory {
        fun create(pairRequest: WcPairRequest): WcPairUseCase
    }
}

sealed interface WcPairState {
    data object Loading : WcPairState
    data class Error(val error: WcPairError) : WcPairState
    data class Proposal(val dAppSession: WcSessionProposal) : WcPairState

    sealed interface Approving : WcPairState {
        val session: WcSessionApprove

        data class Loading(override val session: WcSessionApprove) : Approving
        data class Result(
            override val session: WcSessionApprove,
            val result: Either<WcPairError, WcSession>,
        ) : Approving
    }
}