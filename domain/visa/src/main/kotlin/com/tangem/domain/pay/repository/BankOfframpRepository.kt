package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState

/** Bank off-ramp BFF operations. Every function is total: failures come back as [BankOfframpError], never thrown. */
interface BankOfframpRepository {

    suspend fun getBankAccounts(userWalletId: UserWalletId): Either<BankOfframpError, List<BankAccount>>

    suspend fun getOptions(userWalletId: UserWalletId): Either<BankOfframpError, BankOfframpOptions>

    suspend fun createOrder(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult>

    suspend fun getOrderState(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<BankOfframpError, BankOfframpOrderState>

    suspend fun submitSignature(
        userWalletId: UserWalletId,
        orderId: String,
        signature: String,
    ): Either<BankOfframpError, Unit>

    suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<BankOfframpError, Unit>
}