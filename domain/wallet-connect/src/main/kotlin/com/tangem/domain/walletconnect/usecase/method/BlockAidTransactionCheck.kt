package com.tangem.domain.walletconnect.usecase.method

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.simultation.TokenInfo
import com.tangem.domain.core.lce.LceFlow

interface BlockAidTransactionCheck {

    val securityStatus: LceFlow<Throwable, Result>

    sealed interface Result {
        val result: CheckTransactionResult

        data class Plain(override val result: CheckTransactionResult) : Result

        data class Approval(
            override val result: CheckTransactionResult,
            val approval: WcApproval,
            val tokenInfo: TokenInfo,
            val isMutable: Boolean,
        ) : Result {

            suspend fun approvalAmount() = approval.getAmount()
        }
    }
}