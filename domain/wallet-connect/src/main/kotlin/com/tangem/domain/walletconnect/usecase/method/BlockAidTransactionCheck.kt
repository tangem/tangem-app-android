package com.tangem.domain.walletconnect.usecase.method

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.tangem.domain.core.lce.LceFlow

interface BlockAidTransactionCheck {

    val securityStatus: LceFlow<Throwable, Result>

    sealed interface Result {
        val result: CheckTransactionResult

        data class Plain(override val result: CheckTransactionResult) : Result

        data class Approval(override val result: CheckTransactionResult) : Result
    }
}