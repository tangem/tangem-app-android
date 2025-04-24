package com.tangem.domain.walletconnect.usecase.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.tangem.domain.core.lce.Lce
import kotlinx.coroutines.flow.Flow

interface WcBlockAidEligibleTransactionUseCase {

    val securityStatus: Flow<Lce<Throwable, CheckTransactionResult>>
}
