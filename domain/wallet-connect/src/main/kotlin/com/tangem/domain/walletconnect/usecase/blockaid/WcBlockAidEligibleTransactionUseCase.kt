package com.tangem.domain.walletconnect.usecase.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.tangem.domain.core.lce.LceFlow

interface WcBlockAidEligibleTransactionUseCase {

    val securityStatus: LceFlow<Throwable, CheckTransactionResult>
}